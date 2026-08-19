package br.com.oficjus.drive.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enderecos")
data class EnderecoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cep: String,
    val logradouro: String,
    val numero: String,
    val bairro: String,
    val cidade: String,
    val estado: String,
    val latitude: Double?,
    val longitude: Double?,
    val ordem: Int = 0,
    val referencia: Int = 0,
    val rotaGrupoId: String? = null
)

@Entity(tableName = "rotas")
data class RotaEntity(
    @PrimaryKey val id: String, // rota_grupo_id
    val nome: String,
    val criadaEm: Long = System.currentTimeMillis(),
    val status: String = "ativa"
)

@Entity(tableName = "remanescentes")
data class RemanescenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cep: String,
    val logradouro: String,
    val numero: String,
    val bairro: String,
    val cidade: String,
    val estado: String,
    val latitude: Double?,
    val longitude: Double?,
    val ordem: Int = 0,
    val referencia: Int = 0,
    val salvoEm: Long = System.currentTimeMillis()
)