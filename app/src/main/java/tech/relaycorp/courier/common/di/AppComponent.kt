package tech.relaycorp.courier.common.di

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class
    ]
)
interface AppComponent {
    fun activityComponent(): ActivityComponent
}
