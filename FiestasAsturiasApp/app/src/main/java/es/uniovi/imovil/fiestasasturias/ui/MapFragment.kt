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

        // animación ligera para que el mapa no aparezca de golpe.
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

        // aplicamos el tipo elegido en ajustes antes de pintar marcadores.
        applyConfiguredMapType()
        map.uiSettings.isZoomControlsEnabled = true

        // punto de partida estable por si todavía no hay ubicación del usuario.
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
                    putString("zona", it.zona)
                    putString("imagen", it.imagen)
                    putStringArrayList("imagenes", ArrayList(it.imagenes))
                    putString("email", it.email)
                    putString("web", it.web)
                    putString("dias", it.dias)
                    putString("facebook", it.facebook)
                    putString("instagram", it.instagram)
                    putString("twitter", it.twitter)
                    putString("youtube", it.youtube)
                    putString("pinterest", it.pinterest)
                    putString("rss", it.rss)
                    putStringArrayList(
                        "otrosCanales",
                        ArrayList(it.otrosCanales.map { pair -> "${pair.first}: ${pair.second}" })
                    )
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

            // al limpiar y repintar marcadores de fiestas, reponemos también el del usuario.
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

    //funcion para cambiar el tipo de mapa segun lo que haya seleccionado el usuario en la configuracion
    private fun applyConfiguredMapType() {
        val prefs = requireContext().getSharedPreferences(AppPreferences.PREFS, Context.MODE_PRIVATE)
        val type = prefs.getString(AppPreferences.KEY_MAP_TYPE, AppPreferences.DEFAULT_MAP_TYPE)
            ?: AppPreferences.DEFAULT_MAP_TYPE

        map.mapType = if (type == AppPreferences.MAP_TYPE_SATELLITE) {
            GoogleMap.MAP_TYPE_SATELLITE
        } else {
            GoogleMap.MAP_TYPE_NORMAL
        }
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
            // si el gps está apagado, llevamos al usuario a ajustes de ubicación.
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

        // pedimos una actualización puntual y la cortamos al primer resultado válido.
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

        // compartimos ubicación con el viewmodel para filtro por distancia.
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
