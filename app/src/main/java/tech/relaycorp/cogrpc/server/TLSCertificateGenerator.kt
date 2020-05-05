package tech.relaycorp.cogrpc.server

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNamesBuilder
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder
import org.bouncycastle.util.encoders.Base64
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.util.Date

class TLSCertificateGenerator(
    val privateKey: PrivateKey,
    val certificateHolder: X509CertificateHolder
) {
    fun exportPrivateKey() = derToPem(this.privateKey.encoded, "PRIVATE KEY")

    fun exportCertificate() = derToPem(this.certificateHolder.encoded, "CERTIFICATE")

    companion object {
        private const val RSA_KEY_MODULUS = 2048
        const val SUBJECT_IP_ADDRESS = "192.168.43.1"

        fun generate(): TLSCertificateGenerator {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(this.RSA_KEY_MODULUS)
            val keyPair = keyGen.generateKeyPair()

            val distinguishedName = buildDistinguishedName()

            val now = ZonedDateTime.now(UTC)
            val builder = X509v3CertificateBuilder(
                distinguishedName,
                generateRandomBigInteger(),
                Date.from(now.minusHours(2).toInstant()), // Account for clock drift
                Date.from(now.plusHours(24).toInstant()),
                distinguishedName,
                SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
            )

            val sanExtensionBuilder = GeneralNamesBuilder()
            sanExtensionBuilder.addName(GeneralName(GeneralName.iPAddress, this.SUBJECT_IP_ADDRESS))
            builder.addExtension(
                Extension.subjectAlternativeName,
                true,
                sanExtensionBuilder.build()
            )

            val signerBuilder = makeSigner(keyPair.private)
            val certificateHolder = builder.build(signerBuilder)

            return TLSCertificateGenerator(keyPair.private, certificateHolder)
        }

        @Throws(CertificateException::class)
        private fun buildDistinguishedName(): X500Name {
            val builder = X500NameBuilder(BCStyle.INSTANCE)
            builder.addRDN(BCStyle.CN, this.SUBJECT_IP_ADDRESS)
            return builder.build()
        }

        private fun makeSigner(issuerPrivateKey: PrivateKey): ContentSigner {
            val signatureAlgorithm =
                DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WithRSAEncryption")
            val digestAlgorithm = DefaultDigestAlgorithmIdentifierFinder().find(signatureAlgorithm)
            val privateKeyParam: AsymmetricKeyParameter =
                PrivateKeyFactory.createKey(issuerPrivateKey.encoded)
            val contentSignerBuilder =
                BcRSAContentSignerBuilder(signatureAlgorithm, digestAlgorithm)
            return contentSignerBuilder.build(privateKeyParam)
        }
    }
}

private fun generateRandomBigInteger(): BigInteger {
    val random = SecureRandom()
    return BigInteger(64, random)
}

private fun derToPem(valueDer: ByteArray, pemLabel: String): ByteArray {
    val valueBase64 = Base64.toBase64String(valueDer)
    val valuePem = "-----BEGIN $pemLabel-----\n${valueBase64}\n-----END $pemLabel-----"
    return valuePem.toByteArray()
}
