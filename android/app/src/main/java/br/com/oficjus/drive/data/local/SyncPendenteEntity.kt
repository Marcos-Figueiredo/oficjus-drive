package br.com.oficjus.drive.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_pendente")
data class SyncPendenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cep: String,
    val numero: String,
    val latitude: Double,
    val longitude: Double,
    val cidade: String,
    val logradouroId: Long,
    val criadoEm: Long = System.currentTimeMillis(),
    val tentativas: Int = 0
)