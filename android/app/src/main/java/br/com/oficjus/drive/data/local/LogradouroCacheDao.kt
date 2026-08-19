package br.com.oficjus.drive.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LogradouroCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirVarios(enderecos: List<LogradouroCacheEntity>)

    @Query("""
        SELECT * FROM logradouro_cache 
        WHERE (:cidadeCache = '' OR cidadeCache = :cidadeCache)
          AND logradouroCompleto LIKE '%' || :termo || '%'
        ORDER BY logradouroCompleto
        LIMIT 20
    """)
    suspend fun buscar(termo: String, cidadeCache: String = ""): List<LogradouroCacheEntity>

    @Query("""
        SELECT * FROM logradouro_cache 
        WHERE (:cidadeCache = '' OR cidadeCache = :cidadeCache)
          AND cep = :cep
        LIMIT 20
    """)
    suspend fun buscarPorCep(cep: String, cidadeCache: String = ""): List<LogradouroCacheEntity>

    @Query("SELECT COUNT(*) FROM logradouro_cache WHERE cidadeCache = :cidadeCache")
    suspend fun contarPorCidade(cidadeCache: String): Int

    @Query("DELETE FROM logradouro_cache WHERE cidadeCache = :cidadeCache")
    suspend fun deletarPorCidade(cidadeCache: String)

    @Query("DELETE FROM logradouro_cache")
    suspend fun limparTudo()

    @Query("DELETE FROM logradouro_cache WHERE cidadeCache NOT IN (:cidades)")
    suspend fun deletarForaDaComarca(cidades: List<String>)

    @Query("SELECT COUNT(*) FROM logradouro_cache")
    suspend fun contarTudo(): Int

    @Query("SELECT DISTINCT cep FROM logradouro_cache WHERE cidadeCache IN (:cidades)")
    suspend fun listarCepsDasCidades(cidades: List<String>): List<String>
}