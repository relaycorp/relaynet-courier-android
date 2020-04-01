package tech.relaycorp.courier.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import tech.relaycorp.courier.App
import tech.relaycorp.courier.data.database.AppDatabase
import tech.relaycorp.courier.data.network.cogrpc.CogRPCServer
import javax.inject.Named

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
    fun database(context: Context, @Named("database_name") databaseName: String) =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()

    @Provides
    fun storedMessageDao(database: AppDatabase) = database.storedMessageDao()

    @Provides
    fun cogRPCServer() =
        CogRPCServer.build("127.0.0.1:8080")
}
