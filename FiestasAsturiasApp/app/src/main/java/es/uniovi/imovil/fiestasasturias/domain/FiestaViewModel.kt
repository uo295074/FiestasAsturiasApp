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

    companion object {
        const val ORDER_NAME = "name"
        const val ORDER_DISTANCE = "distance"
    }

    // el viewmodel concentra el estado de la app y delega acceso a datos en repositorios.
    // así evitamos que los fragments mezclen lógica de ui con lógica de datos.
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

    // última ubicación conocida del usuario. se usa para filtro por distancia.
    var userLat: Double? = null
    var userLng: Double? = null

    // estado actual de filtros en lista.
    // lo guardamos aquí para poder re-aplicarlo cuando cambian datos o ubicación.
    private var currentBusqueda: String = ""
    private var currentLocalidad: String = ""
    private var currentZona: String = ""
    private var currentKm: Double? = null
    private var currentOrder: String = ORDER_NAME

    fun setUserLocation(lat: Double, lng: Double) {
        userLat = lat
        userLng = lng

        // si cambia la ubicación, puede cambiar el resultado del filtro por km.
        aplicarFiltrosLista()
    }

    fun cargarFiestas() {
        viewModelScope.launch {
            try {
                val data = repository.getFiestas()

                // al cargar desde red, sincronizamos estado de favoritos persistidos en room.
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

    // cambia favorito en memoria y lo persiste en segundo plano.
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

    // añade una fiesta al historial, evita duplicados y limita a los 20 ultimos elementos.
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

    fun clearHistorial() {
        // limpiamos primero en memoria para que la ui responda al momento.
        _historial.value = emptyList()
        viewModelScope.launch {
            historyRepository.clearAllHistory()
        }
    }

    fun clearFavoritos() {
        // quitamos marca favorito en toda la lista y propagamos a vistas dependientes.
        listaOriginal = listaOriginal.map { it.copy(esFavorito = false) }
        _fiestas.value = listaOriginal
        _favoritos.value = emptyList()
        aplicarFiltrosLista()

        viewModelScope.launch {
            favoritesRepository.clearAllFavorites()
        }
    }

    // punto de entrada unico para filtros de lista.
    // aquí solo guardamos estado y luego aplicamos en bloque.
    fun buscarFiltrarYDistancia(texto: String, localidad: String, zona: String, km: Double?, order: String) {

        currentBusqueda = texto
        currentLocalidad = localidad
        currentZona = zona
        currentKm = km
        currentOrder = order

        aplicarFiltrosLista()
    }

    private fun aplicarFiltrosLista() {

        // este filtro solo afecta a la pestaña de lista (fiestasFiltradas).
        // favoritos, historial y mapa salen de sus propios datos.

        val filtrada = listaOriginal.filter {

            val coincideBusqueda = it.nombre.contains(currentBusqueda, true)
            val coincideLocalidad = it.localidad.contains(currentLocalidad, true)
            val coincideZona = (it.zona ?: "").contains(currentZona, true)
            val coincideDistancia = cumpleDistancia(it, currentKm)

            coincideBusqueda && coincideLocalidad && coincideZona && coincideDistancia
        }

        val ordered = when (currentOrder) {
            ORDER_DISTANCE -> orderByDistance(filtrada)
            else -> filtrada.sortedBy { it.nombre.lowercase() }
        }

        _fiestasFiltradas.value = ordered
    }

    private fun orderByDistance(items: List<Fiesta>): List<Fiesta> {
        val lat = userLat
        val lng = userLng

        // si no hay ubicación disponible, ordenamos por nombre para no romper la experiencia.
        if (lat == null || lng == null) {
            return items.sortedBy { it.nombre.lowercase() }
        }

        return items.sortedBy { distanciaKm(lat, lng, it.latitud, it.longitud) }
    }

    private fun actualizarFavoritosYHistorial() {
        _favoritos.value = listaOriginal.filter { it.esFavorito }
    }

    // si no hay ubicación o no hay km activo, no se descarta ninguna fiesta.
    private fun cumpleDistancia(fiesta: Fiesta, km: Double?): Boolean {

        val lat = userLat ?: return true
        val lng = userLng ?: return true

        if (km == null) return true

        val dist = distanciaKm(lat, lng, fiesta.latitud, fiesta.longitud)

        return dist <= km
    }

    private fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {

        // cálculo con formula de haversine para distancia entre coordenadas geográficas.
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
