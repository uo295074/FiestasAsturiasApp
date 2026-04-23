package es.uniovi.imovil.fiestasasturias.model


data class FiestaDto(
    val Nombre: Nombre,
    val Informacion: Informacion,
    val Contacto: Contacto,
    val Geolocalizacion: Geolocalizacion,
    val Visualizador: Visualizador?
)

data class Nombre(
    val content: String
)

data class Informacion(
    val DescripcionCorta: Descripcion
)

data class Descripcion(
    val content: String
)

data class Contacto(
    val Localidad: Localidad
)

data class Localidad(
    val content: String
)

data class Geolocalizacion(
    val Coordenadas: Coordenadas
)

data class Coordenadas(
    val content: String?
)

data class Visualizador(
    val Slide: Any?
)

data class Slide(
    val value: String?
)