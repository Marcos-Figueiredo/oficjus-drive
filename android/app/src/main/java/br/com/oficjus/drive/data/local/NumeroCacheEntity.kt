package br.com.oficjus.drive.data.local

import androidx.room.Entity

@Entity(
    tableName = "numero_cache",
    primaryKeys = ["logradouroId", "cep", "numero"]
)
data class NumeroCacheEntity(
    val logradouroId: Long,
    val cep: String,
    val numero: String,
    val latitude: Double?,
    val longitude: Double?,
    val cidade: String = ""
)