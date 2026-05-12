package es.uniovi.imovil.fiestasasturias.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
    private val KEY_KM_ACTIVE = "km_active"

    private var kmActual: Double = 50.0
    private var kmFiltroActivo: Boolean = false

    // 🔥 detectar tablet UNA VEZ
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
                    // 👉 tablet: cargar en panel derecho
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.detailContainer, detailFragment)
                        .commit()
                } else {
                    // 👉 móvil: navegación normal
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

        // 📏 SLIDER KM
        binding.sliderKm.addOnChangeListener { _, value, _ ->

            kmActual = value.toDouble()
            kmFiltroActivo = true
            updateDistanceVisualState()
            guardarKm(kmActual)
            guardarKmActivo(kmFiltroActivo)

            aplicarFiltros()
        }

        // 🔍 BUSCADOR
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

        // 🎛 FILTRO
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

        binding.recyclerView.setOnTouchListener { v, _ ->
            v.performClick()
            binding.searchInput.clearFocus()
            ocultarTeclado()
            false
        }

        cargarFiltros()

        // evitar recargar innecesariamente
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

        viewModel.buscarFiltrarYDistancia(
            texto,
            localidad,
            zona,
            if (kmFiltroActivo) kmActual else null
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
        kmActual = prefs.getFloat(KEY_KM, 50f).toDouble()
        kmFiltroActivo = prefs.getBoolean(KEY_KM_ACTIVE, false)

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
        binding.sliderKm.value = kmActual.toFloat()
        updateDistanceVisualState()
    }

    private fun guardarKm(km: Double) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit().putFloat(KEY_KM, km.toFloat()).apply()
    }

    private fun guardarKmActivo(activo: Boolean) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit().putBoolean(KEY_KM_ACTIVE, activo).apply()
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
