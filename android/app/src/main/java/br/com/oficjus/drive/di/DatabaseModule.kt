package br.com.oficjus.drive.di

import android.content.Context
import androidx.room.Room
import br.com.oficjus.drive.data.local.DriveDatabase
import br.com.oficjus.drive.data.local.EnderecoDao
import br.com.oficjus.drive.data.local.LogradouroCacheDao
import br.com.oficjus.drive.data.local.LogradouroCacheSync
import br.com.oficjus.drive.data.local.NumeroCacheDao
import br.com.oficjus.drive.data.local.NumeroCacheSync
import br.com.oficjus.drive.data.local.RemanescenteDao
import br.com.oficjus.drive.data.local.RotaDao
import br.com.oficjus.drive.data.local.SyncPendenteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DriveDatabase {
        return Room.databaseBuilder(
            context,
            DriveDatabase::class.java,
            "oficjus_drive.db"
        )
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideEnderecoDao(database: DriveDatabase): EnderecoDao {
        return database.enderecoDao()
    }

    @Provides
    fun provideRotaDao(database: DriveDatabase): RotaDao {
        return database.rotaDao()
    }

    @Provides
    fun provideLogradouroCacheDao(database: DriveDatabase): LogradouroCacheDao {
        return database.logradouroCacheDao()
    }

    @Provides
    fun provideNumeroCacheDao(database: DriveDatabase): NumeroCacheDao {
        return database.numeroCacheDao()
    }

    @Provides
    fun provideSyncPendenteDao(database: DriveDatabase): SyncPendenteDao {
        return database.syncPendenteDao()
    }

    @Provides
    fun provideRemanescenteDao(database: DriveDatabase): RemanescenteDao {
        return database.remanescenteDao()
    }

    @Provides
    @Singleton
    fun provideLogradouroCacheSync(
        cacheDao: LogradouroCacheDao,
        client: OkHttpClient
    ): LogradouroCacheSync {
        return LogradouroCacheSync(cacheDao, client)
    }

    @Provides
    @Singleton
    fun provideNumeroCacheSync(
        numeroCacheDao: NumeroCacheDao,
        client: OkHttpClient
    ): NumeroCacheSync {
        return NumeroCacheSync(numeroCacheDao, client)
    }
}