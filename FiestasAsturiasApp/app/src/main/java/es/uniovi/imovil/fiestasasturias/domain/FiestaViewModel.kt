package es.uniovi.imovil.fiestasasturias.domain

import androidx.lifecycle.*
import es.uniovi.imovil.fiestasasturias.data.FiestaRepository
import es.uniovi.imovil.fiestasasturias.model.Fiesta
import kotlinx.coroutines.launch
import android.util.Log

class FiestaViewModel : ViewModel() {

    private val repository = FiestaRepository()

    private val _fiestas = MutableLiveData<List<Fiesta>>()
    val fiestas: LiveData<List<Fiesta>> = _fiestas

    // 🔥 Lista original (IMPORTANTE para búsqueda y filtros)
    private var listaOriginal: List<Fiesta> = emptyList()

    fun cargarFiestas() {
        viewModelScope.launch {
            try {
                val data = repository.getFiestas()
                Log.d("DEBUG_API", "Fiestas recibidas: ${data.size}")

                listaOriginal = data              // 🔥 guardamos original
                _fiestas.value = data            // 🔥 mostramos todo

            } catch (e: Exception) {
                Log.e("DEBUG_API", "ERROR: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // 🔍 SOLO búsqueda
    fun buscar(texto: String) {
        val resultado = listaOriginal.filter {
            it.nombre.contains(texto, true)
        }
        _fiestas.value = resultado
    }

    // 🎛 SOLO filtro por localidad
    fun filtrarPorLocalidad(localidad: String) {
        val resultado = listaOriginal.filter {
            it.localidad.contains(localidad, true)
        }
        _fiestas.value = resultado
    }

    // 🚀 BÚSQUEDA + FILTRO (lo que quiere el profe 🔥)
    fun buscarYFiltrar(texto: String, localidad: String) {

        val resultado = listaOriginal.filter {

            val coincideBusqueda = it.nombre.contains(texto, true)
            val coincideFiltro = it.localidad.contains(localidad, true)

            coincideBusqueda && coincideFiltro
        }

        _fiestas.value = resultado
    }
}