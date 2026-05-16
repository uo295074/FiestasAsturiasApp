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

    private val _fiestasBuscadas = MutableLiveData<List<Fiesta>>()
    val fiestasBuscadas: LiveData<List<Fiesta>> = _fiestasBuscadas

    private val _favoritos = MutableLiveData<List<Fiesta>>()
    val favoritos: LiveData<List<Fiesta>> = _favoritos

    private val _localidades = MutableLiveData<List<String>>()
    val localidades: LiveData<List<String>> = _localidades

    private val _zonas = MutableLiveData<List<String>>()
    val zonas: LiveData<List<String>> = _zonas

    private val _historial = MutableLiveData<List<Fiesta>>()
    val historial: LiveData<List<Fiesta>> = _historial

    private val _userLocation = MutableLiveData<Pair<Double, Double>?>()
    val userLocation: LiveData<Pair<Double, Double>?> = _userLocation

    private var listaOriginal: List<Fiesta> = emptyList()

    // última ubicación conocida del usuario. se usa para filtro por distancia.
    var userLat: Double? = null
    var userLng: Double? = null

    // estado actual de búsqueda en lista. los filtros visuales se aplican en ListFragment.
    private var currentBusqueda: String = ""

    fun setUserLocation(lat: Double, lng: Double) {
        userLat = lat
        userLng = lng
        _userLocation.value = lat to lng
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
                repository.actualizarCache(listaOriginal)
                _fiestas.value = listaConFavoritos
                _localidades.value = listaConFavoritos.map { it.localidad }.distinct().sorted()
                _zonas.value = listaConFavoritos.mapNotNull { it.zona }.distinct().sorted()

                val historyNames = historyRepository.getHistoryNames()
                _historial.value = historyNames.mapNotNull { name ->
                    listaConFavoritos.firstOrNull { it.nombre == name }
                }

                actualizarFavoritosYHistorial()
                buscarFiestas(currentBusqueda)

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
        repository.actualizarCache(listaOriginal)

        if (fiestaActualizada != null) {
            viewModelScope.launch {
                favoritesRepository.setFavorite(fiestaActualizada.nombre, fiestaActualizada.esFavorito)
            }
        }

        _fiestas.value = listaOriginal
        actualizarFavoritosYHistorial()
        buscarFiestas(currentBusqueda)
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
        repository.actualizarCache(listaOriginal)
        _fiestas.value = listaOriginal
        _favoritos.value = emptyList()
        buscarFiestas(currentBusqueda)

        viewModelScope.launch {
            favoritesRepository.clearAllFavorites()
        }
    }

    fun buscarFiestas(texto: String) {
        currentBusqueda = texto
        _fiestasBuscadas.value = repository.buscarFiestas(texto)
    }

    private fun actualizarFavoritosYHistorial() {
        _favoritos.value = listaOriginal.filter { it.esFavorito }
    }

}
