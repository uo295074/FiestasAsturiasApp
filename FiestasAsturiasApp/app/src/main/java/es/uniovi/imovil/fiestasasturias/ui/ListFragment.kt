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
import androidx.fragment.app.viewModels
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel
import androidx.core.widget.addTextChangedListener
import android.widget.ArrayAdapter

class ListFragment : Fragment() {

    private lateinit var binding: FragmentListBinding
    private val viewModel: FiestaViewModel by viewModels()

    private lateinit var adapter: FiestaAdapter

    // 🔥 SharedPreferences
    private val PREFS_NAME = "filtros_prefs"
    private val KEY_BUSQUEDA = "busqueda"
    private val KEY_LOCALIDAD = "localidad"

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

        // 🔥 1. Adapter
        adapter = FiestaAdapter { fiesta ->

            val bundle = Bundle().apply {
                putString("nombre", fiesta.nombre)
                putString("descripcion", fiesta.descripcion)
                putString("localidad", fiesta.localidad)
                putString("imagen", fiesta.imagen)
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, DetailFragment().apply {
                    arguments = bundle
                })
                .addToBackStack(null)
                .commit()
        }

        // 🔥 2. RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // 🔥 3. Observer
        viewModel.fiestas.observe(viewLifecycleOwner) { fiestas ->

            adapter.setData(fiestas)

            // 🎛 localidades únicas
            val localidades = fiestas
                .map { it.localidad }
                .distinct()
                .sorted()

            val adapterLocalidades = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                localidades
            )

            binding.spinnerLocalidad.setAdapter(adapterLocalidades)
        }

        // 🔍 BUSCADOR
        binding.searchInput.addTextChangedListener {

            val textoBusqueda = it.toString()
            val textoFiltro = binding.spinnerLocalidad.text.toString()

            guardarFiltros(textoBusqueda, textoFiltro)

            viewModel.buscarYFiltrar(textoBusqueda, textoFiltro)
        }

        // 🎛 FILTRO
        binding.spinnerLocalidad.addTextChangedListener {

            val textoBusqueda = binding.searchInput.text.toString()
            val textoFiltro = it.toString()

            guardarFiltros(textoBusqueda, textoFiltro)

            viewModel.buscarYFiltrar(textoBusqueda, textoFiltro)
        }

        // 🔥 cargar filtros guardados
        cargarFiltros()

        // 🔥 cargar datos
        viewModel.cargarFiestas()
    }

    // 💾 guardar
    private fun guardarFiltros(busqueda: String, localidad: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit()
            .putString(KEY_BUSQUEDA, busqueda)
            .putString(KEY_LOCALIDAD, localidad)
            .apply()
    }

    // 🔄 cargar
    private fun cargarFiltros() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)

        val busqueda = prefs.getString(KEY_BUSQUEDA, "") ?: ""
        val localidad = prefs.getString(KEY_LOCALIDAD, "") ?: ""

        binding.searchInput.setText(busqueda)
        binding.spinnerLocalidad.setText(localidad, false)

        // aplicar filtros automáticamente
        viewModel.buscarYFiltrar(busqueda, localidad)
    }
}