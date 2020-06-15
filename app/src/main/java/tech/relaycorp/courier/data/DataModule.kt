package tech.relaycorp.courier.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.tfcporciuncula.flow.FlowSharedPreferences
import dagger.Module
import dagger.Provides
import tech.relaycorp.cogrpc.server.CogRPCServer
import tech.relaycorp.courier.App
import tech.relaycorp.courier.data.database.AppDatabase
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import javax.inject.Named
import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    @Singleton
    fun database(context: Context, appMode: App.Mode): AppDatabase =
        when (appMode) {
            App.Mode.Normal ->
                Room.databaseBuilder(context, AppDatabase::class.java, "courier")
            App.Mode.Test ->
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        }.build()

    @Provides
    fun storedMessageDao(database: AppDatabase) = database.storedMessageDao()

    @Provides
    @Named("preferences_name")
    fun preferencesName(appMode: App.Mode) =
        when (appMode) {
            App.Mode.Normal -> "pref_courier"
            App.Mode.Test -> "pref_courier_test"
        }

    @Provides
    fun sharedPreferences(
        context: Context,
        @Named("preferences_name") preferencesName: String
    ): SharedPreferences =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    @Provides
    fun flowSharedPreferences(sharedPreferences: SharedPreferences) =
        FlowSharedPreferences(sharedPreferences)

    @Provides
    fun cogRPCClientBuilder(): CogRPCClient.Builder = CogRPCClient.Builder

    @Provides
    fun cogRPCServer() = CogRPCServer.Builder.build("0.0.0.0", 21473)
}
