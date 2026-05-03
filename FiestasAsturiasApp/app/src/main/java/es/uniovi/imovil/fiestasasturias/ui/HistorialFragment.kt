package es.uniovi.imovil.fiestasasturias.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import es.uniovi.imovil.fiestasasturias.R
import es.uniovi.imovil.fiestasasturias.adapters.FiestaAdapter
import es.uniovi.imovil.fiestasasturias.databinding.FragmentListBinding
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel

class HistorialFragment : Fragment() {

    private lateinit var binding: FragmentListBinding
    private val viewModel: FiestaViewModel by activityViewModels()
    private lateinit var adapter: FiestaAdapter

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

        // 🔥 ocultar filtros
        binding.searchLayout.visibility = View.GONE
        binding.filterLayout.visibility = View.GONE
        binding.cardKm.visibility = View.GONE

        adapter = FiestaAdapter(

            onClick = { fiesta ->

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
                    // 👉 tablet: panel derecho
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

        viewModel.historial.observe(viewLifecycleOwner) { historial ->
            adapter.setData(historial)
        }
    }
}