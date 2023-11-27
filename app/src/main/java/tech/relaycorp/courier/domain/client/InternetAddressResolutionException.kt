package tech.relaycorp.courier.domain.client

import java.lang.Exception

class InternetAddressResolutionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
