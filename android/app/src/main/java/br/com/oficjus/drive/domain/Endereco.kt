package br.com.oficjus.drive.domain

data class Endereco(
    val id: Long = 0,
    val cep: String,
    val logradouro: String,
    val numero: String,
    val bairro: String,
    val cidade: String,
    val estado: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val ordem: Int = 0,
    val referencia: Int = 0, // ordem de digitação original (imutável)
    val tipoGeocode: TipoGeocode = TipoGeocode.NENHUM
) {
    val temCoordenadas: Boolean get() = latitude != null && longitude != null

    val cepFormatado: String
        get() {
            val c = cep.replace(Regex("\\D"), "")
            if (c.length != 8) return cep
            return "${c.substring(0, 5)}-${c.substring(5)}"
        }
}

enum class TipoGeocode {
    NENHUM,
    EXATO,
    ESTIMADO
}

data class Rota(
    val id: String,
    val nome: String,
    val paradas: List<Endereco>,
    val status: RotaStatus = RotaStatus.ATIVA
)

enum class RotaStatus {
    ATIVA
}