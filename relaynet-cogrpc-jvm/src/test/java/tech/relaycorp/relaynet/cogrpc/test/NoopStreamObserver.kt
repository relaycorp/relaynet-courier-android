package tech.relaycorp.relaynet.cogrpc.test

import io.grpc.stub.StreamObserver

class NoopStreamObserver<V> : StreamObserver<V> {
    override fun onNext(value: V) {}
    override fun onError(t: Throwable) {}
    override fun onCompleted() {}
}
