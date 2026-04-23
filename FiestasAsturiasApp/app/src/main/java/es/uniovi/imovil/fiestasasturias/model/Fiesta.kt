package es.uniovi.imovil.fiestasasturias.model

data class Fiesta(
    val nombre: String,
    val localidad: String,
    val descripcion: String,
    val imagen: String?,
    val latitud: Double,
    val longitud: Double,
)