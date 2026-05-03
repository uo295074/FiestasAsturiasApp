package es.uniovi.imovil.fiestasasturias.ui

import android.os.Bundle
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

class ListFragment : Fragment() {

    private lateinit var binding: FragmentListBinding
    private val viewModel: FiestaViewModel by activityViewModels()
    private lateinit var adapter: FiestaAdapter

    private val PREFS_NAME = "filtros_prefs"
    private val KEY_BUSQUEDA = "busqueda"
    private val KEY_LOCALIDAD = "localidad"

    private var kmActual: Double = 50.0

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
                    putString("imagen", fiesta.imagen)
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

        viewModel.fiestas.observe(viewLifecycleOwner) { fiestas ->

            adapter.setData(fiestas)

            val localidades = fiestas.map { it.localidad }.distinct().sorted()

            val adapterLocalidades = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                localidades
            )

            binding.spinnerLocalidad.setAdapter(adapterLocalidades)
        }

        // 📏 SLIDER KM
        binding.sliderKm.addOnChangeListener { _, value, _ ->

            kmActual = value.toDouble()
            binding.textKm.text = "${kmActual.toInt()} km"

            aplicarFiltros()
        }

        // 🔍 BUSCADOR
        binding.searchInput.addTextChangedListener {
            guardarFiltros(it.toString(), binding.spinnerLocalidad.text.toString())
            aplicarFiltros()
        }

        // 🎛 FILTRO
        binding.spinnerLocalidad.addTextChangedListener {
            guardarFiltros(binding.searchInput.text.toString(), it.toString())
            aplicarFiltros()
        }

        cargarFiltros()

        // 🔥 evitar recargar innecesariamente
        if (viewModel.fiestas.value == null) {
            viewModel.cargarFiestas()
        }
    }

    private fun aplicarFiltros() {

        val texto = binding.searchInput.text.toString()
        val localidad = binding.spinnerLocalidad.text.toString()

        viewModel.buscarFiltrarYDistancia(
            texto,
            localidad,
            kmActual
        )
    }

    private fun guardarFiltros(busqueda: String, localidad: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit()
            .putString(KEY_BUSQUEDA, busqueda)
            .putString(KEY_LOCALIDAD, localidad)
            .apply()
    }

    private fun cargarFiltros() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)

        val busqueda = prefs.getString(KEY_BUSQUEDA, "") ?: ""
        val localidad = prefs.getString(KEY_LOCALIDAD, "") ?: ""

        binding.searchInput.setText(busqueda)
        binding.spinnerLocalidad.setText(localidad, false)
    }
}