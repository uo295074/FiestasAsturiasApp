package es.uniovi.imovil.fiestasasturias.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import androidx.core.widget.addTextChangedListener
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat

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

    private var kmActual: Double = 50.0
    private var kmFiltroActivo: Boolean = false
    private var showedDistanceOrderToast: Boolean = false

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
                    // en móvil abrimos detalle en el contenedor principal y añadimos backstack.
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, detailFragment)
                        .addToBackStack(null)
                        .commit()
                }
            },

            onFavClick = { fiesta ->
                viewModel.toggleFavorito(fiesta)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.fiestasFiltradas.observe(viewLifecycleOwner) { fiestas ->

            adapter.setData(fiestas)
        }

        viewModel.localidades.observe(viewLifecycleOwner) { localidadesBase ->
            val localidades = listOf(getString(R.string.all_locations)) + localidadesBase

            val adapterLocalidades = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                localidades
            )

            binding.spinnerLocalidad.setAdapter(adapterLocalidades)
        }

        viewModel.zonas.observe(viewLifecycleOwner) { zonasBase ->
            val zonas = listOf(getString(R.string.all_zones)) + zonasBase

            val adapterZonas = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                zonas
            )
            binding.spinnerZona.setAdapter(adapterZonas)
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
            aplicarFiltros()
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
        val adapterOrden = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            opcionesOrden
        )
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

    private fun aplicarFiltros() {

        val texto = binding.searchInput.text.toString()
        val localidadSeleccionada = binding.spinnerLocalidad.text.toString()
        val zonaSeleccionada = binding.spinnerZona.text.toString()
        val localidad = if (localidadSeleccionada == getString(R.string.all_locations)) "" else localidadSeleccionada
        val zona = if (zonaSeleccionada == getString(R.string.all_zones)) "" else zonaSeleccionada

        // si piden ordenar por cercanía sin ubicación, avisamos una vez y usamos fallback por nombre.
        if (getCurrentOrderCode() == FiestaViewModel.ORDER_DISTANCE &&
            (viewModel.userLat == null || viewModel.userLng == null) &&
            !showedDistanceOrderToast
        ) {
            Toast.makeText(requireContext(), getString(R.string.order_distance_no_location), Toast.LENGTH_SHORT).show()
            showedDistanceOrderToast = true
        }

        // "todas" se traduce a cadena vacía para no condicionar el filtro.
        viewModel.buscarFiltrarYDistancia(
            texto,
            localidad,
            zona,
            if (kmFiltroActivo) kmActual else null,
            getCurrentOrderCode()
        )
    }

    private fun guardarFiltros(busqueda: String, localidad: String, zona: String) {
        val localidadNormalizada = if (localidad == getString(R.string.all_locations)) "" else localidad
        val zonaNormalizada = if (zona == getString(R.string.all_zones)) "" else zona
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
        val localidad = prefs.getString(KEY_LOCALIDAD, "") ?: ""
        val zona = prefs.getString(KEY_ZONA, "") ?: ""
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
    }

    private fun guardarKm(km: Double) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit().putFloat(KEY_KM, km.toFloat()).apply()
    }

    private fun guardarOrden(label: String) {
        val code = if (label == getString(R.string.order_distance)) {
            FiestaViewModel.ORDER_DISTANCE
        } else {
            FiestaViewModel.ORDER_NAME
        }
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit().putString(KEY_ORDEN, code).apply()
    }

    private fun getCurrentOrderCode(): String {
        return if (binding.spinnerOrden.text.toString() == getString(R.string.order_distance)) {
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
}
