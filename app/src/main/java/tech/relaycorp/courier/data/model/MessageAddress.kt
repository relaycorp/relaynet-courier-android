package tech.relaycorp.courier.data.model

data class MessageAddress(val value: String) {

    enum class Type(val value: String) {
        Public("public"), Private("private");

        companion object {
            fun fromValue(value: String) =
                values().firstOrNull { it.value == value }
                    ?: throw IllegalArgumentException("Invalid MessageAddress.Type value = $value")
        }
    }
}
