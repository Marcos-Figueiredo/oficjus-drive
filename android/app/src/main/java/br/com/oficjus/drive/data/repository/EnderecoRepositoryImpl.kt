package br.com.oficjus.drive.data.repository

import br.com.oficjus.drive.data.local.EnderecoDao
import br.com.oficjus.drive.data.local.RotaDao
import br.com.oficjus.drive.data.local.EnderecoEntity
import br.com.oficjus.drive.data.local.RotaEntity
import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.Rota
import br.com.oficjus.drive.domain.RotaStatus
import br.com.oficjus.drive.domain.repository.EnderecoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnderecoRepositoryImpl @Inject constructor(
    private val enderecoDao: EnderecoDao,
    private val rotaDao: RotaDao
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

    override suspend fun limparAvulsos() {
        enderecoDao.limparAvulsos()
    }

    override suspend fun salvarRota(rota: Rota) {
        rotaDao.inserir(
            RotaEntity(
                id = rota.id,
                nome = rota.nome,
                totalParadas = rota.paradas.size,
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
        return Rota(
            id = rotaEntity.id,
            nome = rotaEntity.nome,
            paradas = paradas.map { it.toDomain() },
            status = RotaStatus.valueOf(rotaEntity.status.uppercase())
        )
    }

    override suspend fun atualizarStatusRota(rotaId: String, status: String) {
        rotaDao.atualizarStatus(rotaId, status)
    }

    override suspend fun deletarEnderecoDaRota(id: Long) {
        enderecoDao.deletarEnderecoDaRota(id)
    }

    override suspend fun deletarPorReferencia(rotaGrupoId: String, referencia: Int) {
        enderecoDao.deletarPorReferencia(rotaGrupoId, referencia)
    }

    override suspend fun limparRota(rotaId: String) {
        enderecoDao.limparPorRota(rotaId)
        rotaDao.deletar(rotaId)
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
}