package es.uniovi.imovil.fiestasasturias.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {

    // base común de la api de turismo asturias que usamos en toda la app.
    private const val BASE_URL = "http://156.35.163.145/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: FiestaApiService by lazy {
        // se crea en lazy para inicializar retrofit una sola vez.
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FiestaApiService::class.java)
    }
}
