package tech.relaycorp.courier.data.model

enum class GatewayType(val value: String) {
    Internet("internet"), Private("private");

    companion object {
        fun fromValue(value: String) =
            values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid gateway type ($value)")
    }
}
