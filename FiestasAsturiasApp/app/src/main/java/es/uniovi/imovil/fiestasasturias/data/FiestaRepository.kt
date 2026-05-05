package es.uniovi.imovil.fiestasasturias.data

import android.util.Log
import es.uniovi.imovil.fiestasasturias.model.Fiesta

class FiestaRepository {

    suspend fun getFiestas(): List<Fiesta> {

        val response = RetrofitClient.api.getFiestas()

        return response.articles.article.map { dto ->

            val coordsString = dto.Geolocalizacion.Coordenadas.content
            val lat: Double
            val lng: Double
            if (!coordsString.isNullOrEmpty() && coordsString.contains(",")) {
                val parts = coordsString.split(",")
                lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 0.0
                lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0
            } else {
                lat = 0.0
                lng = 0.0
            }
            val imagen = try {
                val slide = dto.Visualizador?.Slide
                val raw = when (slide){
                    is Map<*,*> -> slide["value"] as? String
                    is List<*> -> (slide.firstOrNull() as? Map<*,*>)?.get("value") as? String
                    else -> null
                }

                if (raw != null){
                    val json = org.json.JSONObject(raw)
                    val uuid = json.takeIf { it.has("uuid") }?.optString("uuid")
                    val groupId = json.takeIf { it.has("groupId") }?.optString("groupId")

                    if (uuid != null && groupId != null){
                        "https://www.turismoasturias.es/c/document_library/get_file?uuid=$uuid&groupId=$groupId"
                    }else{
                        null
                    }
                }else{
                    null
                }

            } catch (e: Exception) {
                null
            }
            Log.d("IMG_DEBUG", "Imagen: $imagen")

            val redes = dto.RedesSociales
            val otrosCanales = buildOtrosCanales(
                redes?.OtrosCanales?.NombreCanal,
                redes?.OtrosCanales?.CanalUrl
            )

            Fiesta(
                nombre = dto.Nombre.content,
                localidad = dto.Contacto.Localidad.content,
                descripcion = dto.Informacion.DescripcionCorta.content,
                imagen = imagen,
                latitud = lat,
                longitud = lng,
                email = normalizeText(dto.Contacto.Email?.content),
                web = normalizeUrl(dto.Contacto.Web?.content),
                dias = normalizeText(dto.Contacto.Dias?.content),
                facebook = normalizeUrl(redes?.Facebook?.content),
                instagram = normalizeUrl(redes?.Instagram?.content),
                twitter = normalizeUrl(redes?.Twitter?.content),
                youtube = normalizeUrl(redes?.Youtube?.content),
                pinterest = normalizeUrl(redes?.Pinterest?.content),
                rss = normalizeUrl(redes?.Rss?.content),
                otrosCanales = otrosCanales
            )
        }
    }

    private fun normalizeText(value: String?): String? {
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() }
        return normalized
    }

    private fun normalizeUrl(value: String?): String? {
        val clean = normalizeText(value) ?: return null
        return if (clean.startsWith("http://") || clean.startsWith("https://")) {
            clean
        } else {
            "https://$clean"
        }
    }

    private fun buildOtrosCanales(
        nombres: Any?,
        urls: Any?
    ): List<Pair<String, String>> {
        val names = extractValues(nombres, "value")
        val links = extractValues(urls, "content").mapNotNull { normalizeUrl(it) }
        if (names.isEmpty() || links.isEmpty()) return emptyList()

        val size = minOf(names.size, links.size)
        return (0 until size).map { index ->
            names[index] to links[index]
        }
    }

    private fun extractValues(raw: Any?, key: String): List<String> {
        val items = when (raw) {
            null -> emptyList()
            is List<*> -> raw
            else -> listOf(raw)
        }

        return items.mapNotNull { item ->
            val value = when (item) {
                is Map<*, *> -> item[key] as? String
                else -> null
            }
            normalizeText(value)
        }
    }
}
