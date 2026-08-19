package br.com.oficjus.drive.domain.model

data class Usuario(
    val id: String,
    val email: String,
    val nome: String? = null,
    val plano: String = "drive",
    val estado: String? = null,
    val cidade: String? = null,
    val comarcaId: String? = null,
    val segmentoId: String? = null,
    val tribunalId: String? = null,
    val usage: String? = null
)