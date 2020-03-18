package tech.relaycorp.courier

import android.app.Application
import tech.relaycorp.courier.common.di.AppComponent
import tech.relaycorp.courier.common.di.DaggerAppComponent
import timber.log.Timber

class App : Application() {

    val component: AppComponent by lazy {
        DaggerAppComponent.builder().build()
    }

    val mode by lazy {
        try {
            classLoader.loadClass("tech.relaycorp.courier.AppTest")
            Mode.Test
        } catch (e: ClassNotFoundException) {
            Mode.Normal
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupLogger()
    }

    private fun setupLogger() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    enum class Mode { Normal, Test }
}
