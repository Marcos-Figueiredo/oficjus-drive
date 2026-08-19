package br.com.oficjus.drive.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncPendenteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(item: SyncPendenteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirVarios(items: List<SyncPendenteEntity>)

    @Query("SELECT * FROM sync_pendente ORDER BY criadoEm ASC LIMIT 50")
    suspend fun listarPendentes(): List<SyncPendenteEntity>

    @Query("DELETE FROM sync_pendente WHERE id = :id")
    suspend fun deletar(id: Long)

    @Query("SELECT COUNT(*) FROM sync_pendente")
    suspend fun contar(): Int
}