package tech.relaycorp.courier.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.tfcporciuncula.flow.FlowSharedPreferences
import dagger.Module
import dagger.Provides
import tech.relaycorp.courier.App
import tech.relaycorp.courier.data.database.AppDatabase
import tech.relaycorp.courier.data.network.cogrpc.CogRPCServer
import javax.inject.Named
import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    @Named("database_name")
    fun databaseName(appMode: App.Mode) =
        when (appMode) {
            App.Mode.Normal -> "courier"
            App.Mode.Test -> "courier_test"
        }

    @Provides
    @Singleton
    fun database(context: Context, @Named("database_name") databaseName: String) =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()

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
    fun cogRPCServer() =
        CogRPCServer.build("127.0.0.1:8080")
}
