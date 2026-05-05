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

class FavoritosFragment : Fragment() {

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

                viewModel.addHistorial(fiesta)

                val bundle = Bundle().apply {
                    putString("nombre", fiesta.nombre)
                    putString("descripcion", fiesta.descripcion)
                    putString("localidad", fiesta.localidad)
                    putString("imagen", fiesta.imagen)
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

        viewModel.favoritos.observe(viewLifecycleOwner) { favoritos ->
            adapter.setData(favoritos)
        }
    }
}
