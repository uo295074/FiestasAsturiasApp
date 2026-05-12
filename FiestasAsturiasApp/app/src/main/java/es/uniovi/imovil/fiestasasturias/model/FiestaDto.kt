package es.uniovi.imovil.fiestasasturias.model


data class FiestaDto(
    val Nombre: Nombre,
    val Informacion: Informacion,
    val Contacto: Contacto,
    val Geolocalizacion: Geolocalizacion,
    val Visualizador: Visualizador?,
    val RedesSociales: RedesSociales?
)

data class Nombre(
    val content: String
)

data class Informacion(
    val DescripcionCorta: Descripcion,
    val Descripcion: Descripcion?
)

data class Descripcion(
    val content: String?
)

data class Contacto(
    val Localidad: Localidad,
    val Zona: Zona?,
    val Email: Email?,
    val Web: Web?,
    val Dias: Dias?
)

data class Localidad(
    val content: String
)

data class Zona(
    val content: String?
)

data class Email(
    val content: String?
)

data class Web(
    val content: String?
)

data class Dias(
    val content: String?
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

data class RedesSociales(
    val Facebook: CanalSimple?,
    val Instagram: CanalSimple?,
    val Twitter: CanalSimple?,
    val Youtube: CanalSimple?,
    val Pinterest: CanalSimple?,
    val Rss: CanalSimple?,
    val OtrosCanales: OtrosCanales?
)

data class CanalSimple(
    val content: String?
)

data class OtrosCanales(
    val NombreCanal: Any?,
    val CanalUrl: Any?
)
