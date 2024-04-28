package com.example.taller3

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller3.databinding.ActivityMapaBinding
import com.google.common.io.Resources
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.IOException
import java.io.InputStream
import androidx.appcompat.widget.Toolbar

class MapaActivity : AppCompatActivity() {

    companion object{
        const val PATH_USERS="users/"
    }

    private lateinit var bindingMapa: ActivityMapaBinding

    //Variables relacionadas a firebase
    private val database = FirebaseDatabase.getInstance()
    private lateinit var referencia: DatabaseReference
    private lateinit var autenticacion: FirebaseAuth

    private lateinit var auth: FirebaseAuth
    private var location: Location? = null
    private lateinit var changeService: CambioDisponibilidad

    //Coordenadas
    private var latitud: Double = 0.0
    private var longitud: Double = 0.0
    private val startPoint = org.osmdroid.util.GeoPoint(4.628593, -74.065041)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.menu.menu)
        bindingMapa = ActivityMapaBinding.inflate(layoutInflater)
        setContentView(bindingMapa.root)
        autenticacion = Firebase.auth
        auth = autenticacion
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        referencia = database.getReference(PATH_USERS + auth.currentUser!!.uid)
        referencia.child("available").setValue(true)

        //OSM
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = packageName
        bindingMapa.osmMap.setTileSource(TileSourceFactory.MAPNIK)
        bindingMapa.osmMap.setMultiTouchControls(true)
        changeService = CambioDisponibilidad(this)
        changeService.startListening()

        permisos()
        actualizarMarcadorJSON()

        //Código momentáneo para cerrar sesión

        bindingMapa.Salir.setOnClickListener {
            autenticacion.signOut()
            val intent = Intent(this, InicioSesionActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        // Handle item selection
        return when (item.itemId)
        {
            R.id.menuLogOut ->
            {
                //myRef = database.getReference(PATH_USERS + auth.currentUser!!.uid)
                referencia.child("disponible").setValue(false)
                auth.signOut()
                val intentLogOut = Intent(this, InicioSesionActivity::class.java)
                intentLogOut.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                changeService.stopListening()
                startActivity(intentLogOut)
                finish()

                true
            }
            R.id.menuToggleStatus ->
            {
                //myRef = database.getReference(PATH_USERS + auth.currentUser!!.uid)
                referencia.child("disponible").get().addOnSuccessListener { availableSnapshot ->
                    val isAvailable = availableSnapshot.getValue(Boolean::class.java) ?: false
                    referencia.child("disponible").setValue(!isAvailable)
                    val statusText = if (!isAvailable) "disponible" else "no disponible"
                    Toast.makeText(this, "Ahora te encuentras $statusText", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menuAvailableUsers ->
            {
                val intentAvailableUsers = Intent(this, UsuariosActivosActivity::class.java)
                changeService.stopListening()
                startActivity(intentAvailableUsers)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun permisos(){
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                Permisos.LOCATION_PERMISSION_CODE)
        } else {
            ubicacionActual()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            Permisos.LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ubicacionActual()
                }else{
                    Toast.makeText(this, "Permiso no otorgado por el usuario", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Código relacionado a la ubicación
    @SuppressLint("MissingPermission")
    private fun ubicacionActual() {
        //Solicitud de ubicación cada 10 s de acuerdo al objeto locationListener
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 10f, locationListener)
    }

    private val locationListener: LocationListener = object : LocationListener {
        //Método que se ejecuta cuando la ubicación cambia, actualizando el objeto location
        override fun onLocationChanged(location: Location) {
            Log.d("MapsActivity", "Ubicación actualizada: $location")
            latitud = location.latitude
            longitud = location.longitude
            Log.i("LISTENER", "Latitud: $latitud y Longitud: $longitud")
            actualizarMarcadorUbiActual(latitud, longitud)
            startPoint.latitude = latitud
            startPoint.longitude = longitud
            actualizarUbicacionUsuario(latitud, longitud)
        }
    }

    private var marcador: Marker? = null
    private fun actualizarMarcadorUbiActual(latitud: Double, longitud: Double) {
        if (marcador != null){
            marcador?.let { bindingMapa.osmMap.overlays.remove(it)}
        }

        val punto = GeoPoint(latitud, longitud)
        marcador = Marker(bindingMapa.osmMap).apply{
            icon = cambioTamañoIcono(resources.getDrawable(R.drawable.ubicacion))
            position = punto
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        bindingMapa.osmMap.overlays.add(marcador)

        val mapController: IMapController = bindingMapa.osmMap.controller
        mapController.animateTo(punto)
        mapController.setZoom(18.0)
    }

    private fun actualizarMarcadorJSON(){
        val datosJSON = JSONObject(loadJSONFromAsset())
        val datosLocalizacion = datosJSON.getJSONObject("locations")

        for( i in datosLocalizacion.keys()){
            val dato = datosLocalizacion.getJSONObject(i)
            val latitudJson = dato.getDouble("latitude")
            val longitudJson = dato.getDouble("longitude")
            val nombreJson = dato.getString("name")

            val punto = GeoPoint(latitudJson, longitudJson)
            val marcadorJson = Marker(bindingMapa.osmMap).apply {
                icon = cambioTamañoIcono(resources.getDrawable(R.drawable.ubicacion_json))
                position = punto
                title = nombreJson
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            bindingMapa.osmMap.overlays.add(marcadorJson)
        }
    }

    private fun cambioTamañoIcono(icono: Drawable): Drawable {
        val bitmap = (icono as BitmapDrawable).bitmap
        val bitmapCambiado = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
        return BitmapDrawable(resources, bitmapCambiado)
    }

    //Actualización ubicación en el Realtime database
    private fun actualizarUbicacionUsuario(latitud: Double, longitud: Double){
        val uid = autenticacion.currentUser?.uid
        if (uid != null){
            val usuarioActual = database.getReference(PATH_USERS + uid)
            usuarioActual.child("latitud").setValue(latitud)
            usuarioActual.child("longitud").setValue(longitud)
        }
    }

    //Código relacionado a los archivos JSON
    private fun loadJSONFromAsset(): String? {
        var json: String? = null
        try{
            val istream: InputStream = assets.open( "locations.json")
            val size: Int = istream.available()
            val buffer = ByteArray(size)
            istream.read(buffer)
            istream.close()
            json = String(buffer, Charsets. UTF_8)
        }catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }



    //Código relacionado al ciclo de vida
    override fun onResume() {
        super.onResume()
        bindingMapa.osmMap.onResume()
        val mapController: IMapController = bindingMapa.osmMap.controller
        mapController.setZoom(18.0)
        mapController.setCenter(this.startPoint)
    }

    override fun onPause() {
        super.onPause()
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
        bindingMapa.osmMap.onPause()
    }
}