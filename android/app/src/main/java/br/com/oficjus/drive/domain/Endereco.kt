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
    val referencia: Int = 0 // ordem de digitação original (imutável)
) {
    val temCoordenadas: Boolean get() = latitude != null && longitude != null

    val enderecoCompleto: String
        get() = buildString {
            append(logradouro)
            if (numero.isNotBlank() && numero != "S/N") append(", $numero")
            append(" - $bairro, $cidade/$estado")
        }

    val cepFormatado: String
        get() {
            val c = cep.replace(Regex("\\D"), "")
            if (c.length != 8) return cep
            return "${c.substring(0, 5)}-${c.substring(5)}"
        }
}

data class Rota(
    val id: String,
    val nome: String,
    val paradas: List<Endereco>,
    val criadaEm: Long = System.currentTimeMillis(),
    val status: RotaStatus = RotaStatus.ATIVA
)

enum class RotaStatus {
    ATIVA, CONCLUIDA, PAUSADA
}