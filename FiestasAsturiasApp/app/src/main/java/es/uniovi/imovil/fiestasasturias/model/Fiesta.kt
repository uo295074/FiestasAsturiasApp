package es.uniovi.imovil.fiestasasturias.model

// este es el modelo de dominio que usa la ui.
// llega ya "limpio" desde el repositorio y evita que fragments trabajen con el json crudo.
data class Fiesta(
    val nombre: String,
    val localidad: String,
    val zona: String? = null,
    val descripcion: String,
    val imagen: String?,
    // imagen principal para tarjetas y galería completa para el detalle.
    val imagenes: List<String> = emptyList(),
    val latitud: Double,
    val longitud: Double,
    val email: String? = null,
    val web: String? = null,
    val dias: String? = null,
    val facebook: String? = null,
    val instagram: String? = null,
    val twitter: String? = null,
    val youtube: String? = null,
    val pinterest: String? = null,
    val rss: String? = null,
    val otrosCanales: List<Pair<String, String>> = emptyList(),
    // este estado lo sincronizamos con room al cargar y al pulsar favorito.
    var esFavorito: Boolean = false
)
