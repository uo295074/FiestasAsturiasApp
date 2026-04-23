package es.uniovi.imovil.fiestasasturias.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import es.uniovi.imovil.fiestasasturias.R
import es.uniovi.imovil.fiestasasturias.adapters.FiestaAdapter
import es.uniovi.imovil.fiestasasturias.databinding.FragmentListBinding
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel
import kotlin.getValue

class FavoritosFragment : Fragment() {

    private lateinit var binding: FragmentListBinding
    private val viewModel: FiestaViewModel by activityViewModels()
    private lateinit var adapter: FiestaAdapter

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


        binding.searchLayout.visibility = View.GONE
        binding.filterLayout.visibility = View.GONE

        adapter = FiestaAdapter(

            // CLICK → detalle + historial
            onClick = { fiesta ->

                viewModel.addHistorial(fiesta)

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
            },

            // ⭐ quitar de favoritos
            onFavClick = { fiesta ->
                viewModel.toggleFavorito(fiesta)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // 🔥 observar favoritos
        viewModel.favoritos.observe(viewLifecycleOwner) { favoritos ->
            adapter.setData(favoritos)
        }
    }
}