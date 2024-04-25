package com.example.taller3

data class Usuario(
    var nombre: String,
    var apellido: String,
    var numeroIdentificacion: Long,
    var latitud: Double,
    var longitud: Double
) {
    constructor() : this("", "", 0L, 0.0, 0.0) // Constructor sin argumentos
}