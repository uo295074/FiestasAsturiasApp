package es.uniovi.imovil.fiestasasturias

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import es.uniovi.imovil.fiestasasturias.model.FiestaResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FiestaDtoParsingTest {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(FiestaResponse::class.java)

    @Test
    fun parse_ok_when_otros_canales_is_object() {
        val json = """
            {
              "articles": {
                "article": [
                  {
                    "Nombre": {"content": "Fiesta A"},
                    "Informacion": {"DescripcionCorta": {"content": "Desc"}},
                    "Contacto": {
                      "Localidad": {"content": "Oviedo"},
                      "Email": {"content": "test@fiesta.es"},
                      "Web": {"content": "fiesta.es"},
                      "Dias": {"content": "1-3 mayo"}
                    },
                    "Geolocalizacion": {"Coordenadas": {"content": "43.36,-5.84"}},
                    "Visualizador": {"Slide": null},
                    "RedesSociales": {
                      "Facebook": {"content": "https://facebook.com/fiesta"},
                      "OtrosCanales": {
                        "NombreCanal": {"value": "TikTok"},
                        "CanalUrl": {"content": "https://tiktok.com/@fiesta"}
                      }
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals(1, response!!.articles.article.size)
        assertEquals("Fiesta A", response.articles.article[0].Nombre.content)
    }

    @Test
    fun parse_ok_when_otros_canales_is_array() {
        val json = """
            {
              "articles": {
                "article": [
                  {
                    "Nombre": {"content": "Fiesta B"},
                    "Informacion": {"DescripcionCorta": {"content": "Desc"}},
                    "Contacto": {
                      "Localidad": {"content": "Gijon"},
                      "Email": {"content": ""},
                      "Web": {"content": ""},
                      "Dias": {"content": ""}
                    },
                    "Geolocalizacion": {"Coordenadas": {"content": "43.54,-5.66"}},
                    "Visualizador": {"Slide": null},
                    "RedesSociales": {
                      "OtrosCanales": {
                        "NombreCanal": [{"value": "Telegram"}],
                        "CanalUrl": [{"content": "https://t.me/fiesta"}]
                      }
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals("Fiesta B", response!!.articles.article[0].Nombre.content)
    }
}
