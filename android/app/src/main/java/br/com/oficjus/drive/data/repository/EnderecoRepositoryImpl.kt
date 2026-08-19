package br.com.oficjus.drive.data.repository

import br.com.oficjus.drive.data.local.DriveDatabase
import br.com.oficjus.drive.data.local.EnderecoDao
import br.com.oficjus.drive.data.local.RotaDao
import br.com.oficjus.drive.data.local.RemanescenteDao
import br.com.oficjus.drive.data.local.EnderecoEntity
import br.com.oficjus.drive.data.local.RotaEntity
import br.com.oficjus.drive.data.local.RemanescenteEntity
import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.Rota
import br.com.oficjus.drive.domain.RotaStatus
import br.com.oficjus.drive.domain.repository.EnderecoRepository
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnderecoRepositoryImpl @Inject constructor(
    private val enderecoDao: EnderecoDao,
    private val rotaDao: RotaDao,
    private val remanescenteDao: RemanescenteDao,
    private val database: DriveDatabase
) : EnderecoRepository {

    override suspend fun salvarEndereco(endereco: Endereco): Long {
        val entity = EnderecoEntity(
            cep = endereco.cep,
            logradouro = endereco.logradouro,
            numero = endereco.numero,
            bairro = endereco.bairro,
            cidade = endereco.cidade,
            estado = endereco.estado,
            latitude = endereco.latitude,
            longitude = endereco.longitude,
            ordem = endereco.ordem,
            referencia = endereco.referencia
        )
        return enderecoDao.inserir(entity)
    }

    override suspend fun getEnderecosAvulsos(): List<Endereco> {
        return enderecoDao.getEnderecosAvulsos().map { it.toDomain() }
    }

    override suspend fun deletarEndereco(id: Long) {
        enderecoDao.deletar(id)
    }

    override suspend fun salvarRota(rota: Rota) {
        rotaDao.inserir(
            RotaEntity(
                id = rota.id,
                nome = rota.nome,
                status = rota.status.name.lowercase()
            )
        )
        val entities = rota.paradas.mapIndexed { index, endereco ->
            EnderecoEntity(
                cep = endereco.cep,
                logradouro = endereco.logradouro,
                numero = endereco.numero,
                bairro = endereco.bairro,
                cidade = endereco.cidade,
                estado = endereco.estado,
                latitude = endereco.latitude,
                longitude = endereco.longitude,
                ordem = index + 1,
                referencia = endereco.referencia,
                rotaGrupoId = rota.id
            )
        }
        enderecoDao.inserirVarios(entities)
    }

    override suspend fun getRotaAtiva(): Rota? {
        val rotaEntity = rotaDao.getTodas().firstOrNull { it.status == "ativa" }
            ?: return null
        val paradas = enderecoDao.getEnderecosPorRota(rotaEntity.id)
        return Rota(
            id = rotaEntity.id,
            nome = rotaEntity.nome,
            paradas = paradas.map { it.toDomain() },
            status = RotaStatus.ATIVA
        )
    }

    override suspend fun getRota(rotaGrupoId: String): Rota? {
        val rotaEntity = rotaDao.getTodas().firstOrNull { it.id == rotaGrupoId }
            ?: return null
        val paradas = enderecoDao.getEnderecosPorRota(rotaGrupoId)
        // Fallback seguro: status desconhecido vira ATIVA (não crasha)
        val status = try {
            RotaStatus.valueOf(rotaEntity.status.uppercase())
        } catch (_: Exception) {
            RotaStatus.ATIVA
        }
        return Rota(
            id = rotaEntity.id,
            nome = rotaEntity.nome,
            paradas = paradas.map { it.toDomain() },
            status = status
        )
    }

    override suspend fun deletarPorReferencia(rotaGrupoId: String, referencia: Int) {
        enderecoDao.deletarPorReferencia(rotaGrupoId, referencia)
    }

    override suspend fun limparRota(rotaId: String) {
        enderecoDao.limparPorRota(rotaId)
        rotaDao.deletar(rotaId)
    }

    override suspend fun substituirRota(rotaId: String, rota: Rota) {
        database.withTransaction {
            // Limpa registros antigos
            enderecoDao.limparPorRota(rotaId)
            rotaDao.deletar(rotaId)

            // Insere nova rota e paradas
            rotaDao.inserir(
                RotaEntity(
                    id = rota.id,
                    nome = rota.nome,
                    status = rota.status.name.lowercase()
                )
            )
            val entities = rota.paradas.mapIndexed { index, endereco ->
                EnderecoEntity(
                    cep = endereco.cep,
                    logradouro = endereco.logradouro,
                    numero = endereco.numero,
                    bairro = endereco.bairro,
                    cidade = endereco.cidade,
                    estado = endereco.estado,
                    latitude = endereco.latitude,
                    longitude = endereco.longitude,
                    ordem = index + 1,
                    referencia = endereco.referencia,
                    rotaGrupoId = rota.id
                )
            }
            enderecoDao.inserirVarios(entities)
        }
    }

    private fun EnderecoEntity.toDomain() = Endereco(
        id = id,
        cep = cep,
        logradouro = logradouro,
        numero = numero,
        bairro = bairro,
        cidade = cidade,
        estado = estado,
        latitude = latitude,
        longitude = longitude,
        ordem = ordem,
        referencia = referencia
    )

    private fun RemanescenteEntity.toDomain() = Endereco(
        id = id,
        cep = cep,
        logradouro = logradouro,
        numero = numero,
        bairro = bairro,
        cidade = cidade,
        estado = estado,
        latitude = latitude,
        longitude = longitude,
        ordem = ordem,
        referencia = referencia
    )

    override suspend fun salvarRemanescente(endereco: Endereco) {
        remanescenteDao.inserir(
            RemanescenteEntity(
                cep = endereco.cep,
                logradouro = endereco.logradouro,
                numero = endereco.numero,
                bairro = endereco.bairro,
                cidade = endereco.cidade,
                estado = endereco.estado,
                latitude = endereco.latitude,
                longitude = endereco.longitude,
                ordem = endereco.ordem,
                referencia = endereco.referencia
            )
        )
    }

    override suspend fun getRemanescentes(): List<Endereco> {
        return remanescenteDao.getTodos().map { it.toDomain() }
    }

    override suspend fun limparRemanescentes() {
        remanescenteDao.limparTodos()
    }
}