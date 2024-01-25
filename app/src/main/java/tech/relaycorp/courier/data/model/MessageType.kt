package tech.relaycorp.courier.data.model

enum class MessageType(val value: String) {
    Cargo("cargo"),
    CCA("cca"),
    ;

    companion object {
        fun fromValue(value: String) =
            values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid MessageAddress.Type value = $value")
    }
}
