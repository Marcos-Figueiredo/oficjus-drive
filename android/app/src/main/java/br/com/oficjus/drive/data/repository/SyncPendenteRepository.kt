package br.com.oficjus.drive.data.repository

import br.com.oficjus.drive.data.local.SyncPendenteDao
import br.com.oficjus.drive.data.local.SyncPendenteEntity
import br.com.oficjus.drive.domain.repository.CnefeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncPendenteRepository @Inject constructor(
    private val syncPendenteDao: SyncPendenteDao,
    private val cnefeRepository: CnefeRepository
) {

    suspend fun adicionar(cep: String, numero: String, latitude: Double, longitude: Double, cidade: String, logradouroId: Long) {
        syncPendenteDao.inserir(
            SyncPendenteEntity(
                cep = cep,
                numero = numero,
                latitude = latitude,
                longitude = longitude,
                cidade = cidade,
                logradouroId = logradouroId
            )
        )
    }

    suspend fun processarFila() {
        val pendentes = syncPendenteDao.listarPendentes()
        for (item in pendentes) {
            try {
                cnefeRepository.upsertCoordenadas(
                    cep = item.cep,
                    numero = item.numero,
                    latitude = item.latitude,
                    longitude = item.longitude,
                    cidade = item.cidade,
                    logradouroId = item.logradouroId
                )
                syncPendenteDao.deletar(item.id)
            } catch (_: Exception) {
                // Se falhou de novo, deixa pra próxima
                if (item.tentativas >= 5) {
                    syncPendenteDao.deletar(item.id) // desiste após 5 tentativas
                }
            }
        }
    }

    suspend fun contar(): Int = syncPendenteDao.contar()
}