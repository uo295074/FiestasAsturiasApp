package es.uniovi.imovil.fiestasasturias.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import es.uniovi.imovil.fiestasasturias.R
import es.uniovi.imovil.fiestasasturias.adapters.FiestaAdapter
import es.uniovi.imovil.fiestasasturias.databinding.FragmentListBinding
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel
import es.uniovi.imovil.fiestasasturias.model.Fiesta
import androidx.core.widget.addTextChangedListener
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.fragment.app.activityViewModels
import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.findNavController
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class ListFragment : Fragment() {

    private lateinit var binding: FragmentListBinding
    private val viewModel: FiestaViewModel by activityViewModels()
    private lateinit var adapter: FiestaAdapter

    private val PREFS_NAME = "filtros_prefs"
    private val KEY_BUSQUEDA = "busqueda"
    private val KEY_LOCALIDAD = "localidad"
    private val KEY_ZONA = "zona"
    private val KEY_KM = "km"
    private val KEY_ORDEN = "orden"
    private val KM_MAX = 200.0
    private val allLocationLabels = setOf("", "all locations", "todas las localidades")
    private val allZoneLabels = setOf("", "all areas", "todas las zonas")
    private val distanceOrderLabels = setOf("nearby", "cercania", "cercanía")

    private var kmActual: Double = 50.0
    private var kmFiltroActivo: Boolean = false
    private var showedDistanceOrderToast: Boolean = false
    private var resultadoBusqueda: List<Fiesta> = emptyList()

    private class FullListAdapter(context: Context, items: List<String>) :
        ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, items.toMutableList()) {

        private val allItems = items.toList()

        override fun getCount(): Int = allItems.size

        override fun getItem(position: Int): String = allItems[position]

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    return FilterResults().apply {
                        values = allItems
                        count = allItems.size
                    }
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
    }

    // en tablet abrimos detalle en panel derecho; en móvil navegamos a pantalla completa.
    private var isTablet = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isTablet = requireActivity().findViewById<View?>(R.id.detailContainer) != null

        adapter = FiestaAdapter(

            onClick = { fiesta ->

                viewModel.addHistorial(fiesta)

                val bundle = Bundle().apply {
                    putString("nombre", fiesta.nombre)
                    putString("descripcion", fiesta.descripcion)
                    putString("localidad", fiesta.localidad)
                    putString("zona", fiesta.zona)
                    putString("imagen", fiesta.imagen)
                    putStringArrayList("imagenes", ArrayList(fiesta.imagenes))
                    putString("email", fiesta.email)
                    putString("web", fiesta.web)
                    putString("dias", fiesta.dias)
                    putString("facebook", fiesta.facebook)
                    putString("instagram", fiesta.instagram)
                    putString("twitter", fiesta.twitter)
                    putString("youtube", fiesta.youtube)
                    putString("pinterest", fiesta.pinterest)
                    putString("rss", fiesta.rss)
                    putStringArrayList(
                        "otrosCanales",
                        ArrayList(fiesta.otrosCanales.map { "${it.first}: ${it.second}" })
                    )
                }

                val detailFragment = DetailFragment().apply {
                    arguments = bundle
                }

                if (isTablet) {
                    // en tablet se mantiene patrón master-detail en la misma pantalla.
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.detailContainer, detailFragment)
                        .commit()
                } else {
                    // en móvil usamos la api de navegación para mantener el backstack consistente.
                    findNavController().navigate(R.id.nav_detail, bundle)
                }
            },

            onFavClick = { fiesta ->
                viewModel.toggleFavorito(fiesta)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        prepareSelector(binding.spinnerLocalidad)
        prepareSelector(binding.spinnerZona)
        prepareSelector(binding.spinnerOrden)

        viewModel.fiestasBuscadas.observe(viewLifecycleOwner) { fiestas ->
            resultadoBusqueda = fiestas
            aplicarFiltros()
        }

        viewModel.userLocation.observe(viewLifecycleOwner) {
            aplicarFiltros()
        }

        viewModel.localidades.observe(viewLifecycleOwner) { localidadesBase ->
            val localidades = listOf(getString(R.string.all_locations)) + localidadesBase

            val adapterLocalidades = FullListAdapter(requireContext(), localidades)

            binding.spinnerLocalidad.setAdapter(adapterLocalidades)
            refreshLocalizedSelectors()
        }

        viewModel.zonas.observe(viewLifecycleOwner) { zonasBase ->
            val zonas = listOf(getString(R.string.all_zones)) + zonasBase

            val adapterZonas = FullListAdapter(requireContext(), zonas)
            binding.spinnerZona.setAdapter(adapterZonas)
            refreshLocalizedSelectors()
        }

        binding.btnOpenFilters.setOnClickListener {
            ocultarTeclado()
            binding.filtersDrawer.openDrawer(GravityCompat.END)
        }

        // el filtro de distancia solo se considera activo cuando el usuario toca el slider.
        binding.sliderKm.addOnChangeListener { _, value, _ ->

            kmActual = value.toDouble()
            // si está en el máximo, lo tratamos como "sin filtro de distancia".
            kmFiltroActivo = kmActual < KM_MAX
            updateDistanceVisualState()
            guardarKm(kmActual)

            aplicarFiltros()
        }

        // búsqueda en tiempo real por nombre.
        binding.searchInput.addTextChangedListener {
            guardarFiltros(
                it.toString(),
                binding.spinnerLocalidad.text.toString(),
                binding.spinnerZona.text.toString()
            )
            viewModel.buscarFiestas(it.toString())
        }

        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                ocultarTeclado()
                true
            } else {
                false
            }
        }

        // localidad y zona se aplican al instante y se guardan en preferencias.
        binding.spinnerLocalidad.addTextChangedListener {
            guardarFiltros(
                binding.searchInput.text.toString(),
                it.toString(),
                binding.spinnerZona.text.toString()
            )
            aplicarFiltros()
            ocultarTeclado()
            binding.filtersDrawer.closeDrawer(GravityCompat.END)
        }

        binding.spinnerZona.addTextChangedListener {
            guardarFiltros(
                binding.searchInput.text.toString(),
                binding.spinnerLocalidad.text.toString(),
                it.toString()
            )
            aplicarFiltros()
            ocultarTeclado()
            binding.filtersDrawer.closeDrawer(GravityCompat.END)
        }

        val opcionesOrden = listOf(
            getString(R.string.order_name),
            getString(R.string.order_distance)
        )
        val adapterOrden = FullListAdapter(requireContext(), opcionesOrden)
        binding.spinnerOrden.setAdapter(adapterOrden)
        binding.spinnerOrden.addTextChangedListener {
            guardarOrden(it.toString())
            if (it.toString() != getString(R.string.order_distance)) {
                showedDistanceOrderToast = false
            }
            aplicarFiltros()
            ocultarTeclado()
            binding.filtersDrawer.closeDrawer(GravityCompat.END)
        }

        binding.recyclerView.setOnTouchListener { v, _ ->
            v.performClick()
            binding.searchInput.clearFocus()
            ocultarTeclado()
            false
        }

        cargarFiltros()

        // evitamos recargar innecesariamente si ya hay datos en memoria.
        if (viewModel.fiestas.value == null) {
            viewModel.cargarFiestas()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLocalizedSelectors()
    }

    private fun aplicarFiltros() {

        val localidadSeleccionada = binding.spinnerLocalidad.text.toString()
        val zonaSeleccionada = binding.spinnerZona.text.toString()
        val localidad = normalizeLocalidad(localidadSeleccionada)
        val zona = normalizeZona(zonaSeleccionada)

        // si piden ordenar por cercanía sin ubicación, avisamos una vez y usamos fallback por nombre.
        if (getCurrentOrderCode() == FiestaViewModel.ORDER_DISTANCE &&
            (viewModel.userLat == null || viewModel.userLng == null) &&
            !showedDistanceOrderToast
        ) {
            Toast.makeText(requireContext(), getString(R.string.order_distance_no_location), Toast.LENGTH_SHORT).show()
            showedDistanceOrderToast = true
        }

        // "todas" se traduce a cadena vacía para no condicionar el filtro.
        val filtrada = resultadoBusqueda.filter { fiesta ->
            val coincideLocalidad = fiesta.localidad.contains(localidad, ignoreCase = true)
            val coincideZona = (fiesta.zona ?: "").contains(zona, ignoreCase = true)
            val coincideDistancia = cumpleDistancia(fiesta, if (kmFiltroActivo) kmActual else null)

            coincideLocalidad && coincideZona && coincideDistancia
        }

        adapter.setData(ordenarParaPresentacion(filtrada))
    }

    private fun guardarFiltros(busqueda: String, localidad: String, zona: String) {
        val localidadNormalizada = normalizeLocalidad(localidad)
        val zonaNormalizada = normalizeZona(zona)
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit()
            .putString(KEY_BUSQUEDA, busqueda)
            .putString(KEY_LOCALIDAD, localidadNormalizada)
            .putString(KEY_ZONA, zonaNormalizada)
            .apply()
    }

    private fun cargarFiltros() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)

        val busqueda = prefs.getString(KEY_BUSQUEDA, "") ?: ""
        val localidad = normalizeLocalidad(prefs.getString(KEY_LOCALIDAD, "") ?: "")
        val zona = normalizeZona(prefs.getString(KEY_ZONA, "") ?: "")
        val orden = prefs.getString(KEY_ORDEN, FiestaViewModel.ORDER_NAME) ?: FiestaViewModel.ORDER_NAME
        kmActual = prefs.getFloat(KEY_KM, KM_MAX.toFloat()).toDouble().coerceIn(1.0, KM_MAX)
        // al restaurar, máximo equivale a filtro desactivado.
        kmFiltroActivo = kmActual < KM_MAX

        // restauramos estado de ui para que los filtros persistan entre sesiones.
        binding.searchInput.setText(busqueda)
        if (localidad.isBlank()) {
            binding.spinnerLocalidad.setText(getString(R.string.all_locations), false)
        } else {
            binding.spinnerLocalidad.setText(localidad, false)
        }
        if (zona.isBlank()) {
            binding.spinnerZona.setText(getString(R.string.all_zones), false)
        } else {
            binding.spinnerZona.setText(zona, false)
        }
        binding.spinnerOrden.setText(
            if (orden == FiestaViewModel.ORDER_DISTANCE) getString(R.string.order_distance) else getString(R.string.order_name),
            false
        )
        binding.sliderKm.value = kmActual.toFloat()
        updateDistanceVisualState()

        // saneamos preferencias por si quedaron guardadas etiquetas traducidas de una sesion previa.
        guardarFiltros(busqueda, localidad, zona)
        viewModel.buscarFiestas(busqueda)
    }

    private fun guardarKm(km: Double) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit().putFloat(KEY_KM, km.toFloat()).apply()
    }

    private fun guardarOrden(label: String) {
        val code = if (label == getString(R.string.order_distance) || label.trim().lowercase() in distanceOrderLabels) {
            FiestaViewModel.ORDER_DISTANCE
        } else {
            FiestaViewModel.ORDER_NAME
        }
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit().putString(KEY_ORDEN, code).apply()
    }

    private fun getCurrentOrderCode(): String {
        val label = binding.spinnerOrden.text.toString()
        return if (label == getString(R.string.order_distance) || label.trim().lowercase() in distanceOrderLabels) {
            FiestaViewModel.ORDER_DISTANCE
        } else {
            FiestaViewModel.ORDER_NAME
        }
    }

    private fun updateDistanceVisualState() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val mutedColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)

        if (kmFiltroActivo) {
            binding.textKm.text = getString(R.string.distance_active_value, kmActual.toInt())
            binding.textKm.setTextColor(activeColor)
            binding.sliderKm.trackActiveTintList = ColorStateList.valueOf(activeColor)
            binding.sliderKm.haloTintList = ColorStateList.valueOf(activeColor)
        } else {
            binding.textKm.text = getString(R.string.distance_inactive)
            binding.textKm.setTextColor(mutedColor)
            binding.sliderKm.trackActiveTintList = ColorStateList.valueOf(mutedColor)
            binding.sliderKm.haloTintList = ColorStateList.valueOf(mutedColor)
        }
    }

    private fun ocultarTeclado() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun prepareSelector(view: AutoCompleteTextView) {
        view.keyListener = null
        view.setOnClickListener { view.showDropDown() }
    }

    private fun refreshLocalizedSelectors() {
        if (!this::binding.isInitialized) return

        val localidadNorm = normalizeLocalidad(binding.spinnerLocalidad.text?.toString() ?: "")
        if (localidadNorm.isBlank()) {
            binding.spinnerLocalidad.setText(getString(R.string.all_locations), false)
        }

        val zonaNorm = normalizeZona(binding.spinnerZona.text?.toString() ?: "")
        if (zonaNorm.isBlank()) {
            binding.spinnerZona.setText(getString(R.string.all_zones), false)
        }

        val orderCode = getCurrentOrderCode()
        binding.spinnerOrden.setText(
            if (orderCode == FiestaViewModel.ORDER_DISTANCE) getString(R.string.order_distance) else getString(R.string.order_name),
            false
        )
    }

    private fun normalizeLocalidad(value: String): String {
        val normalized = value.trim().lowercase()
        return if (normalized in allLocationLabels || value == getString(R.string.all_locations)) "" else value
    }

    private fun normalizeZona(value: String): String {
        val normalized = value.trim().lowercase()
        return if (normalized in allZoneLabels || value == getString(R.string.all_zones)) "" else value
    }

    private fun ordenarParaPresentacion(items: List<Fiesta>): List<Fiesta> {
        return if (getCurrentOrderCode() == FiestaViewModel.ORDER_DISTANCE) {
            val lat = viewModel.userLat
            val lng = viewModel.userLng
            if (lat == null || lng == null) {
                items.sortedBy { it.nombre.lowercase() }
            } else {
                items.sortedBy { distanciaKm(lat, lng, it.latitud, it.longitud) }
            }
        } else {
            items.sortedBy { it.nombre.lowercase() }
        }
    }

    private fun cumpleDistancia(fiesta: Fiesta, km: Double?): Boolean {
        val lat = viewModel.userLat ?: return true
        val lng = viewModel.userLng ?: return true
        if (km == null) return true

        return distanciaKm(lat, lng, fiesta.latitud, fiesta.longitud) <= km
    }

    private fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // cálculo con formula de haversine para distancia entre coordenadas geográficas.
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }
}
