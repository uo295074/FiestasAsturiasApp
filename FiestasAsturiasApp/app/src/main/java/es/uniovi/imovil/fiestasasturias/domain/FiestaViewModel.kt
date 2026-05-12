package es.uniovi.imovil.fiestasasturias.domain

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import es.uniovi.imovil.fiestasasturias.data.FavoritesRepository
import es.uniovi.imovil.fiestasasturias.data.HistoryRepository
import es.uniovi.imovil.fiestasasturias.data.FiestaRepository
import es.uniovi.imovil.fiestasasturias.data.local.AppDatabase
import es.uniovi.imovil.fiestasasturias.model.Fiesta
import kotlinx.coroutines.launch
import kotlin.math.*

class FiestaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FiestaRepository()
    private val favoritesRepository = FavoritesRepository(
        AppDatabase.getInstance(application).favoriteDao()
    )
    private val historyRepository = HistoryRepository(
        AppDatabase.getInstance(application).historialDao()
    )

    private val _fiestas = MutableLiveData<List<Fiesta>>()
    val fiestas: LiveData<List<Fiesta>> = _fiestas

    private val _fiestasFiltradas = MutableLiveData<List<Fiesta>>()
    val fiestasFiltradas: LiveData<List<Fiesta>> = _fiestasFiltradas

    private val _favoritos = MutableLiveData<List<Fiesta>>()
    val favoritos: LiveData<List<Fiesta>> = _favoritos

    private val _localidades = MutableLiveData<List<String>>()
    val localidades: LiveData<List<String>> = _localidades

    private val _zonas = MutableLiveData<List<String>>()
    val zonas: LiveData<List<String>> = _zonas

    private val _historial = MutableLiveData<List<Fiesta>>()
    val historial: LiveData<List<Fiesta>> = _historial

    private var listaOriginal: List<Fiesta> = emptyList()

    // 📍 ubicación usuario
    var userLat: Double? = null
    var userLng: Double? = null

    // 🎛 estado global filtros (CLAVE 🔥)
    private var currentBusqueda: String = ""
    private var currentLocalidad: String = ""
    private var currentZona: String = ""
    private var currentKm: Double? = null

    fun setUserLocation(lat: Double, lng: Double) {
        userLat = lat
        userLng = lng

        // 🔥 re-aplicar filtros al cambiar ubicación
        aplicarFiltrosLista()
    }

    fun cargarFiestas() {
        viewModelScope.launch {
            try {
                val data = repository.getFiestas()
                Log.d("DEBUG_API", "📦 Fiestas recibidas: ${data.size}")

                val favoritosGuardados = favoritesRepository.getFavoriteNames()

                val listaConFavoritos = data.map {
                    it.copy(esFavorito = favoritosGuardados.contains(it.nombre))
                }

                listaOriginal = listaConFavoritos
                _fiestas.value = listaConFavoritos
                _localidades.value = listaConFavoritos.map { it.localidad }.distinct().sorted()
                _zonas.value = listaConFavoritos.mapNotNull { it.zona }.distinct().sorted()

                val historyNames = historyRepository.getHistoryNames()
                _historial.value = historyNames.mapNotNull { name ->
                    listaConFavoritos.firstOrNull { it.nombre == name }
                }

                actualizarFavoritosYHistorial()
                aplicarFiltrosLista()

            } catch (e: Exception) {
                Log.e("DEBUG_API", "ERROR: ${e.message}")
            }
        }
    }

    // ⭐ FAVORITOS
    fun toggleFavorito(fiesta: Fiesta) {

        val fiestaActualizada = listaOriginal.firstOrNull { it.nombre == fiesta.nombre }
            ?.copy(esFavorito = !fiesta.esFavorito)

        val nuevos = listaOriginal.map {
            if (it.nombre == fiesta.nombre) it.copy(esFavorito = !it.esFavorito) else it
        }

        listaOriginal = nuevos

        if (fiestaActualizada != null) {
            viewModelScope.launch {
                favoritesRepository.setFavorite(fiestaActualizada.nombre, fiestaActualizada.esFavorito)
            }
        }

        _fiestas.value = listaOriginal
        actualizarFavoritosYHistorial()
        aplicarFiltrosLista()
    }

    // 🕓 HISTORIAL
    fun addHistorial(fiesta: Fiesta) {

        val actuales = _historial.value?.toMutableList() ?: mutableListOf()

        actuales.removeAll { it.nombre == fiesta.nombre }
        actuales.add(0, fiesta)

        _historial.value = actuales.take(20)

        viewModelScope.launch {
            historyRepository.addToHistory(fiesta.nombre)
        }

        actualizarFavoritosYHistorial()
    }

    // 🔍 FUNCIÓN GLOBAL (TODO PASA POR AQUÍ 🔥)
    fun buscarFiltrarYDistancia(texto: String, localidad: String, zona: String, km: Double?) {

        currentBusqueda = texto
        currentLocalidad = localidad
        currentZona = zona
        currentKm = km

        aplicarFiltrosLista()
    }

    private fun aplicarFiltrosLista() {

        val filtrada = listaOriginal.filter {

            val coincideBusqueda = it.nombre.contains(currentBusqueda, true)
            val coincideLocalidad = it.localidad.contains(currentLocalidad, true)
            val coincideZona = (it.zona ?: "").contains(currentZona, true)
            val coincideDistancia = cumpleDistancia(it, currentKm)

            coincideBusqueda && coincideLocalidad && coincideZona && coincideDistancia
        }

        _fiestasFiltradas.value = filtrada
    }

    private fun actualizarFavoritosYHistorial() {
        _favoritos.value = listaOriginal.filter { it.esFavorito }
    }

    // 📏 DISTANCIA
    private fun cumpleDistancia(fiesta: Fiesta, km: Double?): Boolean {

        val lat = userLat ?: return true
        val lng = userLng ?: return true

        if (km == null) return true

        val dist = distanciaKm(lat, lng, fiesta.latitud, fiesta.longitud)

        return dist <= km
    }

    private fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {

        val R = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }
}
