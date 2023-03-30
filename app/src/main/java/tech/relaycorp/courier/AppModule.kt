package tech.relaycorp.courier

import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import tech.relaycorp.cogrpc.server.Networking
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@Module
class AppModule(
    private val app: App
) {

    @Provides
    fun app() = app

    @Provides
    fun appMode() = app.mode

    @Provides
    fun context(): Context = app

    @Provides
    fun resources(): Resources = app.resources

    @Provides
    fun connectivityManager() =
        app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    fun wifiApState(): WifiApStateAvailability =
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S_V2) {
            WifiApStateAvailability.Available
        } else {
            WifiApStateAvailability.Unavailable
        }

    @Provides
    @Named("GetGatewayIpAddress")
    fun getGatewayIpAddress(): () -> String = Networking::getGatewayIpAddress

    @Provides
    @Named("BackgroundCoroutineContext")
    fun backgroundCoroutineContext(): CoroutineContext = Dispatchers.IO

    enum class WifiApStateAvailability {
        Available, Unavailable
    }
}
