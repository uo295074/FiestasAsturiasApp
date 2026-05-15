package es.uniovi.imovil.fiestasasturias.data

import es.uniovi.imovil.fiestasasturias.model.FiestaResponse
import retrofit2.http.GET

interface FiestaApiService {

    // endpoint único con el catálogo de fiestas en json.
    @GET("json/FiestasInteresTuristico.json")
    suspend fun getFiestas(): FiestaResponse
}
