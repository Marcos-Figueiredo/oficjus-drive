package br.com.oficjus.drive.domain.repository

import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.Rota

interface CnefeRepository {
    suspend fun buscarPorLogradouro(termo: String, cidade: String? = null, estado: String? = null): List<Endereco>
    suspend fun buscarPorCepENumero(cep: String, numero: String): List<Endereco>
    suspend fun buscarPorCep(cep: String): List<Endereco>
    suspend fun buscarNumerosPorCep(cep: String): List<Endereco>
    suspend fun upsertCoordenadas(cep: String, numero: String, latitude: Double, longitude: Double, cidade: String, logradouroId: Long)
}

interface EnderecoRepository {
    suspend fun salvarEndereco(endereco: Endereco): Long
    suspend fun getEnderecosAvulsos(): List<Endereco>
    suspend fun deletarEndereco(id: Long)
    suspend fun salvarRota(rota: Rota)
    suspend fun getRotaAtiva(): Rota?
    suspend fun getRota(rotaGrupoId: String): Rota?
    suspend fun limparRota(rotaId: String)
    suspend fun deletarPorReferencia(rotaGrupoId: String, referencia: Int)
    suspend fun substituirRota(rotaId: String, rota: Rota)
    suspend fun salvarRemanescente(endereco: Endereco)
    suspend fun getRemanescentes(): List<Endereco>
    suspend fun limparRemanescentes()
}

interface ComarcaRepository {
    suspend fun listarCidades(segmentoCodigo: String, tribunaCodigo: String, comarcaCodigo: String): List<String>
}