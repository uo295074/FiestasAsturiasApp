package es.uniovi.imovil.fiestasasturias.model

// estos dto reflejan el formato del json remoto tal cual viene.
// luego FiestaRepository transforma esto al modelo de dominio Fiesta.

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
    // en el json real puede venir como objeto, lista o null.
    // se deja Any? para parsearlo  en el repositorio.
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
    // también llega con estructura variable, por eso usamos Any?.
    val NombreCanal: Any?,
    val CanalUrl: Any?
)
