package br.com.oficjus.drive.data.repository

import br.com.oficjus.drive.data.remote.CnefeApi
import br.com.oficjus.drive.data.remote.ViaCepApi
import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.repository.CnefeRepository
import br.com.oficjus.drive.domain.repository.ViaCepRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CnefeRepositoryImpl @Inject constructor(
    private val cnefeApi: CnefeApi
) : CnefeRepository {

    override suspend fun buscarPorLogradouro(termo: String): List<Endereco> {
        val termoNormalizado = termo
            .uppercase()
            .replace(Regex("[^A-Z0-9\\s]"), "")
            .trim()

        if (termoNormalizado.length < 3) return emptyList()

        // Remove tipos de logradouro conhecidos para não atrapalhar a busca
        val tipos = listOf(
            "RUA", "R\\.?", "AVENIDA", "AV\\.?", "PRACA", "PC\\.?", "PÇ\\.",
            "BECO", "BC\\.?", "TRAVESSA", "TV\\.?", "TRV\\.?",
            "ALAMEDA", "AL\\.?", "RODOVIA", "ROD\\.?", "ESTRADA", "ESTR\\.?", "EST\\.",
            "LARGO", "LG\\.?", "VILA", "VL\\.?", "CONJUNTO", "CJ\\.",
            "QUADRA", "QD\\.?", "SETOR", "ST\\.?", "JARDIM", "JD\\.",
            "PARQUE", "PQ\\.?", "SERVIDAO", "SERV\\.?", "VIELA",
            "PASSAGEM", "PSG\\.?", "PONTE", "PTE\\.?", "MORRO",
            "FAZENDA", "FAZ\\.?", "CHACARA", "CH\\.?", "SITIO",
            "COLONIA", "COL\\.?", "DISTRITO", "DIST\\.",
            "GLEBA", "GL\\.?", "HORTO", "HT\\.?", "LOTEAMENTO", "LT\\.",
            "NUCLEO", "NUC\\.?", "RECANTO", "REC\\.?",
            "RESIDENCIAL", "RES\\.?", "ROTATORIA", "ROT\\.",
            "TUNEL", "TUN\\.?", "VIA", "VIADUTO", "VD\\."
        ).joinToString("|")
        val termoLimpo = termoNormalizado
            .replace(Regex("^($tipos)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()

        val termoBusca = if (termoLimpo.length >= 3) termoLimpo else termoNormalizado

        val response = cnefeApi.buscarPorLogradouro(
            logradouro = "ilike.*${termoBusca}*".replace(" ", "%20"),
            limit = 50
        )

        // Agrupa por logradouro + CEP (unifica ruas com muitos números)
        // Ex: "Rua Independência, 30110000" aparece 1 vez, não 30
        return response
            .filter { it.cep.isNotBlank() }
            .groupBy { (it.logradouroNome ?: "") + "|" + it.cep }
            .map { (_, items) ->
                val primeiro = items.first()
                val partes = listOfNotNull(
                    primeiro.logradouroTipo,
                    primeiro.logradouroTitulo,
                    primeiro.logradouroNome
                ).joinToString(" ")

                Endereco(
                    cep = primeiro.cep,
                    logradouro = partes,
                    numero = "",  // sem número — usuário digitará depois
                    bairro = primeiro.bairro ?: "",
                    cidade = primeiro.cidade,
                    estado = "MG",
                    latitude = primeiro.latitude,  // mantém para ordenação por distância
                    longitude = primeiro.longitude
                )
            }
            .take(20)
    }

    override suspend fun buscarPorCepENumero(cep: String, numero: String): List<Endereco> {
        return try {
            val filtroCep = "eq.$cep"
            val response = if (numero.isNotBlank()) {
                cnefeApi.buscarPorCepENumero(filtroCep, "eq.$numero")
            } else {
                cnefeApi.buscarPorCep(filtroCep)
            }
            response.map { dto ->
                val partes = listOfNotNull(
                    dto.logradouroTipo,
                    dto.logradouroTitulo,
                    dto.logradouroNome
                ).joinToString(" ")

                Endereco(
                    cep = dto.cep,
                    logradouro = partes,
                    numero = dto.numero ?: numero,
                    bairro = dto.bairro ?: "",
                    cidade = dto.cidade,
                    estado = "MG",
                    latitude = dto.latitude,
                    longitude = dto.longitude
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun buscarPorCep(cep: String): List<Endereco> {
        return try {
            val response = cnefeApi.buscarPorCep("eq.$cep")
            response.map { dto ->
                val partes = listOfNotNull(
                    dto.logradouroTipo,
                    dto.logradouroTitulo,
                    dto.logradouroNome
                ).joinToString(" ")

                Endereco(
                    cep = dto.cep,
                    logradouro = partes,
                    numero = dto.numero ?: "S/N",
                    bairro = dto.bairro ?: "",
                    cidade = dto.cidade,
                    estado = "MG",
                    latitude = dto.latitude,
                    longitude = dto.longitude
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun buscarCoordsPorCep(cep: String): Endereco? {
        return try {
            val response = cnefeApi.buscarCoordsPorCep(cep)
            response.firstOrNull { it.latitude != null && it.longitude != null }?.let { dto ->
                Endereco(
                    cep = cep,
                    logradouro = "",
                    numero = "",
                    bairro = dto.bairro ?: "",
                    cidade = dto.cidade,
                    estado = "MG",
                    latitude = dto.latitude,
                    longitude = dto.longitude
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Singleton
class ViaCepRepositoryImpl @Inject constructor(
    private val viaCepApi: ViaCepApi
) : ViaCepRepository {

    override suspend fun buscarPorCep(cep: String): Endereco? {
        val cepLimpo = cep.replace(Regex("\\D"), "")
        if (cepLimpo.length != 8) return null

        return try {
            val response = viaCepApi.buscarPorCep(cepLimpo)
            if (response.erro) return null

            Endereco(
                cep = response.cep?.replace("-", "") ?: cepLimpo,
                logradouro = response.logradouro ?: "",
                numero = "",
                bairro = response.bairro ?: "",
                cidade = response.localidade ?: "",
                estado = response.uf ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}