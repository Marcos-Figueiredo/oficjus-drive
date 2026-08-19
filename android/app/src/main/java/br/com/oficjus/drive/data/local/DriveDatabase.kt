package br.com.oficjus.drive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        EnderecoEntity::class, RotaEntity::class,
        LogradouroCacheEntity::class, NumeroCacheEntity::class,
        SyncPendenteEntity::class, RemanescenteEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class DriveDatabase : RoomDatabase() {
    abstract fun enderecoDao(): EnderecoDao
    abstract fun rotaDao(): RotaDao
    abstract fun logradouroCacheDao(): LogradouroCacheDao
    abstract fun numeroCacheDao(): NumeroCacheDao
    abstract fun syncPendenteDao(): SyncPendenteDao
    abstract fun remanescenteDao(): RemanescenteDao
}