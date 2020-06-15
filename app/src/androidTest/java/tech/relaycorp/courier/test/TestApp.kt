package tech.relaycorp.courier.test

import tech.relaycorp.courier.App
import tech.relaycorp.courier.AppModule
import tech.relaycorp.courier.common.di.DaggerTestAppComponent
import tech.relaycorp.courier.common.di.TestAppComponent

class TestApp : App() {
    override val component: TestAppComponent =
        DaggerTestAppComponent.builder()
            .appModule(AppModule(this))
            .build()

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
    }
}
