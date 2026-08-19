package br.com.oficjus.drive.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logradouro_cache")
data class LogradouroCacheEntity(
    @PrimaryKey val logradouroCompleto: String,
    val bairro: String,
    val cep: String,
    val cidade: String,
    val estado: String,
    val cidadeCache: String
)