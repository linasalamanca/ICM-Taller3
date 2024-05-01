package com.example.taller3

data class Usuario(
    var uid: String,
    var nombre: String,
    var apellido: String,
    var numeroIdentificacion: Long,
    var latitud: Double,
    var longitud: Double,
    var disponible: Boolean = false,
    var imageUrl: String? = null,
    var available: Boolean = false
) {
    constructor() : this("","", "", 0L, 0.0, 0.0, true)
}
