package es.uniovi.imovil.fiestasasturias.model

data class Fiesta(
    val nombre: String,
    val localidad: String,
    val descripcion: String,
    val imagen: String?,
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
    var esFavorito: Boolean = false
)
