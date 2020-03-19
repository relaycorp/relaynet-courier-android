package tech.relaycorp.courier.common

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel

typealias BehaviorChannel<E> = ConflatedBroadcastChannel<E>
fun <E> PublishChannel() = BroadcastChannel<E>(1)
