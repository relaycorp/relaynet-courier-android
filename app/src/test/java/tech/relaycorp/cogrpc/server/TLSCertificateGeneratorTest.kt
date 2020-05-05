package tech.relaycorp.cogrpc.server

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.security.interfaces.RSAPrivateKey
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

class TLSCertificateGeneratorTest {
    @Nested
    inner class Generate {
        @Test
        fun `Key pair should be RSA 2048`() {
            val generator = TLSCertificateGenerator.generate()

            assertTrue(generator.privateKey is RSAPrivateKey)
            assertEquals(2048, (generator.privateKey as RSAPrivateKey).modulus.bitLength())
        }

        @Test
        @RepeatedTest(8) // Because the bitLength of the value is variable
        fun `Serial number should be a random 64-bit number`() {
            val generator = TLSCertificateGenerator.generate()

            val serialNumber = generator.certificateHolder.serialNumber
            assertTrue(
                serialNumber.bitLength() in 48..64,
                "Value should be between 48 and 64 bits; got ${serialNumber.bitLength()}"
            )
        }

        @Test
        fun `Start date should be two hours in the past to account for clock drift`() {
            val twoHoursAgo = ZonedDateTime.now(UTC).minusHours(2)

            val generator = TLSCertificateGenerator.generate()

            val startDateTimestamp = generator.certificateHolder.notBefore.toInstant().epochSecond
            assertTrue(twoHoursAgo.toEpochSecond() <= startDateTimestamp)
            assertTrue(startDateTimestamp < twoHoursAgo.plusSeconds(2).toEpochSecond())
        }

        @Test
        fun `End date should be 24 hours in the future`() {
            val generator = TLSCertificateGenerator.generate()

            val twentyFourHoursInFuture = ZonedDateTime.now(UTC).plusHours(24)
            val endDateTimestamp = generator.certificateHolder.notAfter.toInstant().epochSecond
            assertTrue(endDateTimestamp <= twentyFourHoursInFuture.toEpochSecond())
            assertTrue(twentyFourHoursInFuture.minusSeconds(2).toEpochSecond() < endDateTimestamp)
        }

        @Test
        fun `Subject DN should only contain the CN set to expected IP address`() {
            val generator = TLSCertificateGenerator.generate()

            val subjectRelativeNames = generator.certificateHolder.subject.rdNs
            assertEquals(1, subjectRelativeNames.size)
            assertEquals(false, subjectRelativeNames[0].isMultiValued)
            assertEquals(BCStyle.CN, subjectRelativeNames[0].first.type)
            assertEquals(
                TLSCertificateGenerator.SUBJECT_IP_ADDRESS,
                subjectRelativeNames[0].first.value.toString()
            )
        }

        @Test
        fun `Issuer DN should only contain the CN set to expected IP address`() {
            val generator = TLSCertificateGenerator.generate()

            val issuerRelativeNames = generator.certificateHolder.issuer.rdNs
            assertEquals(1, issuerRelativeNames.size)
            assertEquals(false, issuerRelativeNames[0].isMultiValued)
            assertEquals(BCStyle.CN, issuerRelativeNames[0].first.type)
            assertEquals(
                TLSCertificateGenerator.SUBJECT_IP_ADDRESS,
                issuerRelativeNames[0].first.value.toString()
            )
        }

        @Test
        fun `SubjectAlternativeName extension should be set to expected IP address as critical`() {
            val generator = TLSCertificateGenerator.generate()

            val extension =
                generator.certificateHolder.getExtension((Extension.subjectAlternativeName))
            assertTrue(extension.isCritical)
            val altNames = GeneralNames.getInstance(extension.parsedValue)
            assertEquals(1, altNames.names.size)
            val expectedAltName = GeneralName(
                GeneralName.iPAddress,
                TLSCertificateGenerator.SUBJECT_IP_ADDRESS
            )
            assertEquals(expectedAltName.encoded.asList(), altNames.names[0].encoded.asList())
        }

        @Test
        fun `Certificate signature should use SHA-256`() {
            val generator = TLSCertificateGenerator.generate()

            assertEquals(
                PKCSObjectIdentifiers.sha256WithRSAEncryption,
                generator.certificateHolder.signatureAlgorithm.algorithm
            )
        }
    }
}
