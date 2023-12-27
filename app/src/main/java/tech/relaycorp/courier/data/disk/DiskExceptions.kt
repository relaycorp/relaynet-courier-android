package tech.relaycorp.courier.data.disk

class MessageDataNotFoundException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

class DiskException(cause: Throwable? = null) : Exception(cause)
