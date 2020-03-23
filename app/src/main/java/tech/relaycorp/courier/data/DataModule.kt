package tech.relaycorp.courier.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import tech.relaycorp.courier.App
import tech.relaycorp.courier.data.database.AppDatabase
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
}
