package tech.relaycorp.courier.common.di

import dagger.Component
import tech.relaycorp.courier.AppModule
import tech.relaycorp.courier.data.DataModule
import tech.relaycorp.courier.domain.PrivateSyncTest
import tech.relaycorp.courier.test.ClearTestDatabaseRule
import tech.relaycorp.courier.test.TestApp
import tech.relaycorp.courier.ui.main.MainActivityTest
import tech.relaycorp.courier.ui.settings.SettingsActivityTest
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        DataModule::class,
    ],
)
interface TestAppComponent : AppComponent {
    fun inject(app: TestApp)

    // Rules

    fun inject(rule: ClearTestDatabaseRule)

    // Tests

    fun inject(test: MainActivityTest)

    fun inject(test: PrivateSyncTest)

    fun inject(test: SettingsActivityTest)
}
