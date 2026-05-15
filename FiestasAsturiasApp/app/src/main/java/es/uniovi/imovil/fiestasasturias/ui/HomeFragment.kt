package es.uniovi.imovil.fiestasasturias.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import es.uniovi.imovil.fiestasasturias.R

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // animamos solo bloques clave para dar sensación de entrada sin recargar la vista.
        val animatedViews = listOf(
            view.findViewById<View>(R.id.homeBadge),
            view.findViewById(R.id.homeTitle),
            view.findViewById(R.id.homeSubtitle),
            view.findViewById(R.id.homeFeaturedCard),
            view.findViewById(R.id.homeBlockOneCard),
            view.findViewById(R.id.homeBlockTwoCard)
        )

        animatedViews.forEachIndexed { index, item ->
            item.alpha = 0f
            item.translationY = 32f
            item.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 70L)
                .setDuration(320L)
                .start()
        }
    }
}
