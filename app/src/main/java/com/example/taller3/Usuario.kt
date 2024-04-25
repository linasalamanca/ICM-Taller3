package com.example.taller3

data class Usuario(
    var nombre: String,
    var apellido: String,
    var numeroIdentificacion: Long,
    var latitud: Double,
    var longitud: Double,
    var disponible: Boolean
) {
    constructor() : this("", "", 0L, 0.0, 0.0, true) // Constructor sin argumentos
}