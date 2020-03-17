package tech.relaycorp.courier.common.di

import dagger.Component
import javax.inject.Singleton
import tech.relaycorp.courier.AppModule
import tech.relaycorp.courier.data.DataModule

@Singleton
@Component(
    modules = [
        AppModule::class,
        DataModule::class
    ]
)
interface AppComponent {
    fun activityComponent(): ActivityComponent
}
