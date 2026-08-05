package br.com.oficjus.drive.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EnderecoDao {

    @Query("SELECT * FROM enderecos WHERE rotaGrupoId IS NULL ORDER BY ordem ASC")
    suspend fun getEnderecosAvulsos(): List<EnderecoEntity>

    @Query("SELECT * FROM enderecos WHERE rotaGrupoId = :rotaGrupoId ORDER BY ordem ASC")
    suspend fun getEnderecosPorRota(rotaGrupoId: String): List<EnderecoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(endereco: EnderecoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirVarios(enderecos: List<EnderecoEntity>)

    @Query("DELETE FROM enderecos WHERE id = :id")
    suspend fun deletar(id: Long)

    @Query("DELETE FROM enderecos WHERE rotaGrupoId IS NULL")
    suspend fun limparAvulsos()

    @Query("DELETE FROM enderecos WHERE rotaGrupoId = :rotaGrupoId")
    suspend fun limparPorRota(rotaGrupoId: String)

    @Query("DELETE FROM enderecos WHERE id = :id AND rotaGrupoId IS NOT NULL")
    suspend fun deletarEnderecoDaRota(id: Long)

    @Query("DELETE FROM enderecos WHERE rotaGrupoId = :rotaGrupoId AND referencia = :referencia")
    suspend fun deletarPorReferencia(rotaGrupoId: String, referencia: Int)
}

@Dao
interface RotaDao {

    @Query("SELECT * FROM rotas ORDER BY criadaEm DESC")
    suspend fun getTodas(): List<RotaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(rota: RotaEntity)

    @Query("UPDATE rotas SET status = :status WHERE id = :id")
    suspend fun atualizarStatus(id: String, status: String)

    @Query("DELETE FROM rotas WHERE id = :id")
    suspend fun deletar(id: String)
}