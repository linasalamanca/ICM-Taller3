package com.example.taller3

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class TrackUserActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_user)  // Asegúrate de tener un layout correspondiente

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        mapView = findViewById(R.id.map)  // Asegúrate de tener un MapView en tu layout
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        // Aquí obtendrías el UID del usuario desde los extras del intent
        val userId = intent.getStringExtra("trackedUid")

        // Implementa la lógica para obtener la ubicación del usuario de Firebase y mostrarla en el mapa
        displayUserLocation(userId)
    }

    private fun displayUserLocation(userId: String?) {
        if (userId != null) {
            // Aquí colocarías la lógica para obtener la ubicación del usuario de Firebase
            // Por ahora, pondré una ubicación estática
            val geoPoint = GeoPoint(40.7128, -74.0060)  // Ejemplo: Nueva York
            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
            mapView.controller.setCenter(geoPoint)
        }
    }
}
