package br.com.oficjus.drive.domain.model

data class Usuario(
    val id: String,
    val email: String,
    val nome: String? = null,
    val plano: String = "drive"
)