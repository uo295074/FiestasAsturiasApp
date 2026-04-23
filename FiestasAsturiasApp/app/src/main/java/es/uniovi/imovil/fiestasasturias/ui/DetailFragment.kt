package es.uniovi.imovil.fiestasasturias.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import es.uniovi.imovil.fiestasasturias.databinding.FragmentDetailBinding
import com.bumptech.glide.Glide
import android.view.animation.DecelerateInterpolator

class DetailFragment : Fragment() {

    private lateinit var binding: FragmentDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // 🔙 BOTÓN VOLVER
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val nombre = arguments?.getString("nombre")
        val descripcion = arguments?.getString("descripcion")
        val localidad = arguments?.getString("localidad")
        val imagen = arguments?.getString("imagen")

        binding.title.text = nombre
        binding.location.text = localidad
        binding.description.text = descripcion

        Glide.with(requireContext())
            .load(imagen)
            .centerCrop()
            .into(binding.image)

        // ✨ ANIMACIÓN DE ENTRADA (PRO)
        binding.root.apply {
            alpha = 0f
            scaleX = 0.95f
            scaleY = 0.95f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
}