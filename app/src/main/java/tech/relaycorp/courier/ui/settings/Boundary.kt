package tech.relaycorp.courier.ui.settings

data class Boundary<T>(
    val min: T,
    val max: T,
    val step: T
) {
    fun <R> map(mapper: ((T) -> R)) =
        Boundary(
            mapper.invoke(min),
            mapper.invoke(max),
            mapper.invoke(step)
        )
}
