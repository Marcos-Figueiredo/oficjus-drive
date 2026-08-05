package br.com.oficjus.drive.domain.repository

import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.Rota

interface CnefeRepository {
    suspend fun buscarPorLogradouro(termo: String): List<Endereco>
    suspend fun buscarPorCepENumero(cep: String, numero: String): List<Endereco>
    suspend fun buscarPorCep(cep: String): List<Endereco>
    suspend fun buscarCoordsPorCep(cep: String): Endereco?
}

interface ViaCepRepository {
    suspend fun buscarPorCep(cep: String): Endereco?
}

interface EnderecoRepository {
    suspend fun salvarEndereco(endereco: Endereco): Long
    suspend fun getEnderecosAvulsos(): List<Endereco>
    suspend fun deletarEndereco(id: Long)
    suspend fun limparAvulsos()
    suspend fun salvarRota(rota: Rota)
    suspend fun getRotaAtiva(): Rota?
    suspend fun getRota(rotaGrupoId: String): Rota?
    suspend fun atualizarStatusRota(rotaId: String, status: String)
    suspend fun limparRota(rotaId: String)
    suspend fun deletarEnderecoDaRota(id: Long)
    suspend fun deletarPorReferencia(rotaGrupoId: String, referencia: Int)
}