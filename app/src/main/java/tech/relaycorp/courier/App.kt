package tech.relaycorp.courier

import android.app.Application
import android.os.StrictMode
import tech.relaycorp.courier.background.WifiHotspotStateReceiver
import tech.relaycorp.courier.common.di.AppComponent
import tech.relaycorp.courier.common.di.DaggerAppComponent
import timber.log.Timber
import javax.inject.Inject

class App : Application() {

    @Inject
    lateinit var wifiHotspotStateReceiver: WifiHotspotStateReceiver

    val component: AppComponent by lazy {
        DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
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
        component.inject(this)
        setupLogger()
        setupStrictMode()
        wifiHotspotStateReceiver.register()
    }

    override fun onTerminate() {
        super.onTerminate()
        wifiHotspotStateReceiver.unregister()
    }

    private fun setupLogger() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build()
            )
        }
    }

    enum class Mode { Normal, Test }
}
