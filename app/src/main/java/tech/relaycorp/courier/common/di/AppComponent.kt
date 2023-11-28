package tech.relaycorp.courier.common.di

import dagger.Component
import tech.relaycorp.courier.App
import tech.relaycorp.courier.AppModule
import tech.relaycorp.courier.data.DataModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        DataModule::class,
    ],
)
interface AppComponent {
    fun activityComponent(): ActivityComponent

    fun inject(app: App)
}
