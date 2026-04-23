package es.uniovi.imovil.fiestasasturias.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import es.uniovi.imovil.fiestasasturias.R
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel
import es.uniovi.imovil.fiestasasturias.databinding.FragmentMapBinding
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.activityViewModels
import kotlin.getValue

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val viewModel: FiestaViewModel by activityViewModels()
    private lateinit var binding: FragmentMapBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMapBinding.bind(view)

        // ✨ Animación entrada
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = true

        map.setOnInfoWindowClickListener { marker ->

            val fiesta = viewModel.fiestas.value?.find {
                it.nombre == marker.title
            }

            fiesta?.let {

                val bundle = Bundle().apply {
                    putString("nombre", it.nombre)
                    putString("descripcion", it.descripcion)
                    putString("localidad", it.localidad)
                    putString("imagen", it.imagen)
                }

                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(R.id.fragmentContainerView, DetailFragment().apply {
                        arguments = bundle
                    })
                    .addToBackStack(null)
                    .commit()
            }
        }

        viewModel.fiestas.observe(viewLifecycleOwner) { fiestas ->

            map.clear()

            fiestas.forEach { fiesta ->

                val lat = fiesta.latitud
                val lng = fiesta.longitud

                if (lat != 0.0 && lng != 0.0) {

                    val posicion = LatLng(lat, lng)

                    map.addMarker(
                        MarkerOptions()
                            .position(posicion)
                            .title(fiesta.nombre)
                            .snippet(fiesta.localidad)
                    )
                }
            }

            val asturias = LatLng(43.3619, -5.8494)

            // 🎯 Animación cámara suave
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(asturias, 7f),
                1200,
                null
            )
        }

        viewModel.cargarFiestas()
    }
}