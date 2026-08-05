package br.com.oficjus.drive.di

import android.content.Context
import androidx.room.Room
import br.com.oficjus.drive.data.local.DriveDatabase
import br.com.oficjus.drive.data.local.EnderecoDao
import br.com.oficjus.drive.data.local.RotaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        ).build()
    }

    @Provides
    fun provideEnderecoDao(database: DriveDatabase): EnderecoDao {
        return database.enderecoDao()
    }

    @Provides
    fun provideRotaDao(database: DriveDatabase): RotaDao {
        return database.rotaDao()
    }
}