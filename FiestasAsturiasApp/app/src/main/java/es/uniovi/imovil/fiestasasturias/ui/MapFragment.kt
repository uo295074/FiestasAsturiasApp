package es.uniovi.imovil.fiestasasturias.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.location.LocationServices
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

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val viewModel: FiestaViewModel by activityViewModels()
    private lateinit var binding: FragmentMapBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMapBinding.bind(view)

        // ✨ Animación
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

        // 📍 fallback SIEMPRE → Asturias
        val asturias = LatLng(43.3619, -5.8494)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(asturias, 7f))

        obtenerUbicacionUsuario()

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

                val posicion = LatLng(fiesta.latitud, fiesta.longitud)

                map.addMarker(
                    MarkerOptions()
                        .position(posicion)
                        .title(fiesta.nombre)
                        .snippet(fiesta.localidad)
                )
            }

            // VOLVER A PINTAR USUARIO
            viewModel.userLat?.let { lat ->
                viewModel.userLng?.let { lng ->

                    val userLatLng = LatLng(lat, lng)

                    map.addMarker(
                        MarkerOptions()
                            .position(userLatLng)
                            .title("Tu ubicación")
                    )
                }
            }
        }

        viewModel.cargarFiestas()
    }

    override fun onResume() {
        super.onResume()
        obtenerUbicacionUsuario()
    }

    private fun obtenerUbicacionUsuario() {

        val fusedLocation = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocation.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->

            location?.let {

                val userLatLng = LatLng(it.latitude, it.longitude)

                // 🔥 GUARDAR BIEN
                viewModel.setUserLocation(it.latitude, it.longitude)

                map.addMarker(
                    MarkerOptions()
                        .position(userLatLng)
                        .title("Tu ubicación")
                )

                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(userLatLng, 10f)
                )
            }
        }
    }
}
