package br.com.oficjus.drive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [EnderecoEntity::class, RotaEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DriveDatabase : RoomDatabase() {
    abstract fun enderecoDao(): EnderecoDao
    abstract fun rotaDao(): RotaDao
}