package es.uniovi.imovil.fiestasasturias.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import es.uniovi.imovil.fiestasasturias.R
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel
import es.uniovi.imovil.fiestasasturias.databinding.FragmentMapBinding
import android.view.animation.DecelerateInterpolator

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val viewModel: FiestaViewModel by activityViewModels()
    private lateinit var binding: FragmentMapBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var userMarker: Marker? = null
    private var requestingLocationUpdates = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMapBinding.bind(view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

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

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionUsuario() {

        if (!hasLocationPermission()) {
            return
        }

        if (!isLocationEnabled()) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                updateUserLocation(location, moveCamera = false)
            }
            requestSingleFreshLocation()
        }.addOnFailureListener {
            requestSingleFreshLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleFreshLocation() {
        if (requestingLocationUpdates || !hasLocationPermission()) return

        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
                .setMinUpdateDistanceMeters(10f)
                .setWaitForAccurateLocation(true)
                .setMaxUpdates(1)
                .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                updateUserLocation(location, moveCamera = true)
                stopLocationUpdates()
            }
        }

        requestingLocationUpdates = true

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback as LocationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        val callback = locationCallback ?: return
        fusedLocationClient.removeLocationUpdates(callback)
        locationCallback = null
        requestingLocationUpdates = false
    }

    private fun updateUserLocation(location: Location, moveCamera: Boolean) {
        val userLatLng = LatLng(location.latitude, location.longitude)

        viewModel.setUserLocation(location.latitude, location.longitude)

        userMarker?.remove()
        userMarker = map.addMarker(
            MarkerOptions()
                .position(userLatLng)
                .title("Tu ubicacion")
        )

        if (moveCamera) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f))
        }
    }

    private fun hasLocationPermission(): Boolean {
        val context = requireContext()
        val fineGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
