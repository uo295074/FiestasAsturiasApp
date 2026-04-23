package es.uniovi.imovil.fiestasasturias.data

import es.uniovi.imovil.fiestasasturias.model.FiestaResponse
import retrofit2.http.GET

interface FiestaApiService {

    @GET("json/FiestasInteresTuristico.json")
    suspend fun getFiestas(): FiestaResponse
}