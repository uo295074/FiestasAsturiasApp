package es.uniovi.imovil.fiestasasturias.data

import android.util.Log
import es.uniovi.imovil.fiestasasturias.model.Fiesta
import org.json.JSONObject

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
                    val uuid = json.optString("uuid", null)
                    val groupId = json.optString("groupId", null)

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

            Fiesta(
                nombre = dto.Nombre.content,
                localidad = dto.Contacto.Localidad.content,
                descripcion = dto.Informacion.DescripcionCorta.content,
                imagen = imagen,
                latitud = lat,
                longitud = lng
            )
        }
    }
}