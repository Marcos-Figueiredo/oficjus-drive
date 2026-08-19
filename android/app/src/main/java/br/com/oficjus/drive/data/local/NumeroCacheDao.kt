package br.com.oficjus.drive.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NumeroCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirVarios(numeros: List<NumeroCacheEntity>)

    @Query("""
        SELECT * FROM numero_cache 
        WHERE cep = :cep AND numero = :numero
        LIMIT 1
    """)
    suspend fun buscarPorCepENumero(cep: String, numero: String): NumeroCacheEntity?

    @Query("SELECT COUNT(*) FROM numero_cache")
    suspend fun contar(): Int

    @Query("SELECT * FROM numero_cache WHERE cep = :cep ORDER BY CAST(numero AS INTEGER)")
    suspend fun buscarPorCep(cep: String): List<NumeroCacheEntity>

    @Query("DELETE FROM numero_cache")
    suspend fun limparTudo()

    @Query("DELETE FROM numero_cache WHERE substr(cep,1,4) NOT IN (:prefixosCep)")
    suspend fun deletarForaDaComarca(prefixosCep: List<String>)

    @Query("SELECT COUNT(*) FROM numero_cache WHERE substr(cep,1,4) IN (:prefixosCep)")
    suspend fun contarDaComarca(prefixosCep: List<String>): Int
}