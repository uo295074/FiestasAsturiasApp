package es.uniovi.imovil.fiestasasturias.domain

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import es.uniovi.imovil.fiestasasturias.data.FiestaRepository
import es.uniovi.imovil.fiestasasturias.model.Fiesta
import kotlinx.coroutines.launch

class FiestaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FiestaRepository()

    private val prefs = application.getSharedPreferences("fiestas_prefs", Context.MODE_PRIVATE)

    private val _fiestas = MutableLiveData<List<Fiesta>>()
    val fiestas: LiveData<List<Fiesta>> = _fiestas

    private val _favoritos = MutableLiveData<List<Fiesta>>()
    val favoritos: LiveData<List<Fiesta>> = _favoritos

    private val _historial = MutableLiveData<List<Fiesta>>()
    val historial: LiveData<List<Fiesta>> = _historial

    private var listaOriginal: List<Fiesta> = emptyList()

    fun cargarFiestas() {
        viewModelScope.launch {
            try {
                val data = repository.getFiestas()
                Log.d("DEBUG_API", "Fiestas recibidas: ${data.size}")

                // 🔥 aplicar favoritos guardados
                val favoritosGuardados = prefs.getStringSet("favoritos", emptySet()) ?: emptySet()

                val listaConFavoritos = data.map {
                    it.copy(esFavorito = favoritosGuardados.contains(it.nombre))
                }

                listaOriginal = listaConFavoritos
                _fiestas.value = listaConFavoritos

                actualizarFavoritos()

            } catch (e: Exception) {
                Log.e("DEBUG_API", "ERROR: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ⭐ TOGGLE FAVORITO
    fun toggleFavorito(fiesta: Fiesta) {

        val nuevos = listaOriginal.map {
            if (it.nombre == fiesta.nombre) {
                it.copy(esFavorito = !it.esFavorito)
            } else it
        }

        listaOriginal = nuevos
        _fiestas.value = nuevos

        // 💾 guardar en prefs
        val favoritosSet = nuevos.filter { it.esFavorito }.map { it.nombre }.toSet()

        prefs.edit().putStringSet("favoritos", favoritosSet).apply()

        actualizarFavoritos()
    }

    private fun actualizarFavoritos() {
        _favoritos.value = listaOriginal.filter { it.esFavorito }
    }

    // 🕓 HISTORIAL
    fun addHistorial(fiesta: Fiesta) {

        val actuales = _historial.value?.toMutableList() ?: mutableListOf()

        // evitar duplicados
        actuales.removeAll { it.nombre == fiesta.nombre }

        actuales.add(0, fiesta) // añadir arriba

        _historial.value = actuales.take(20) // máximo 20
    }

    // 🔍 BUSCAR
    fun buscar(texto: String) {
        val resultado = listaOriginal.filter {
            it.nombre.contains(texto, true)
        }
        _fiestas.value = resultado
    }

    // 🎛 FILTRAR
    fun filtrarPorLocalidad(localidad: String) {
        val resultado = listaOriginal.filter {
            it.localidad.contains(localidad, true)
        }
        _fiestas.value = resultado
    }

    // 🚀 BUSCAR + FILTRAR
    fun buscarYFiltrar(texto: String, localidad: String) {

        val resultado = listaOriginal.filter {

            val coincideBusqueda = it.nombre.contains(texto, true)
            val coincideFiltro = it.localidad.contains(localidad, true)

            coincideBusqueda && coincideFiltro
        }

        _fiestas.value = resultado
    }
}