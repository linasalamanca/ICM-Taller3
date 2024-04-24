package com.example.taller3

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.taller3.databinding.ActivityRegistroBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class RegistroActivity : AppCompatActivity() {

    private lateinit var bindingRegistro: ActivityRegistroBinding

    //Camara y Galería
    private lateinit var activityResultLauncherCamara: ActivityResultLauncher<Intent>
    private lateinit var activityResultLauncherGaleria: ActivityResultLauncher<Intent>

    //Ubicación actual
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        bindingRegistro = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(bindingRegistro.root)

        val btnCamara = bindingRegistro.CamaraBoton
        val btnGaleria = bindingRegistro.GaleriaBoton
        val imagenCuenta = bindingRegistro.imagenCargada

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        permisosUbicacion()
        localizaciónActual()

        activarResultLauncherCamara(imagenCuenta)
        activarResultLauncherGaleria(imagenCuenta)

        btnCamara.setOnClickListener {
            permisosCamara()
        }

        btnGaleria.setOnClickListener {
            permisosGaleria()
        }
    }

    //Código relacionado a todos los permisos
    private fun permisosCamara(){
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                Permisos.CAMERA_PERMISSION_CODE
            )
        } else {
            abrirCamara()
        }
    }

    private fun permisosGaleria(){
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                Permisos.GALERY_PERMISSION_CODE
            )
        } else {
            abrirGaleria()
        }
    }

    private fun permisosUbicacion(){
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Inicia las actualizaciones de ubicación
                startLocationUpdates()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // Solicita los permisos de ubicación
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    Permisos.LOCATION_PERMISSION_CODE
                )
            }
            else -> {
                // Solicita los permisos de ubicación
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    Permisos.LOCATION_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            Permisos.CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, start the contacts activity
                    abrirCamara()
                } else {
                    // Permission denied, show a message asking the user to change the setting in the device settings
                    Toast.makeText(this, "Permiso no otorgado por el usuario", Toast.LENGTH_SHORT).show()
                }
            }
            Permisos.GALERY_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, start the contacts activity
                    abrirGaleria()
                } else {
                    // Permission denied, show a message asking the user to change the setting in the device settings
                    Toast.makeText(this, "Permiso no otorgado por el usuario", Toast.LENGTH_SHORT).show()
                }
            }
            Permisos.LOCATION_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Ubicación habilitada", Toast.LENGTH_SHORT).show()
                    startLocationUpdates()
                } else {
                    Toast.makeText(this, "Permiso no otorgado por el usuario", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    //Código relacionado a la cámara
    private fun abrirCamara(){
        val intentCamara = Intent("android.media.action.IMAGE_CAPTURE")
        activityResultLauncherCamara.launch(intentCamara)
    }

    private fun activarResultLauncherCamara(imagen: ImageView){
        activityResultLauncherCamara = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ resultado ->
            if(resultado.resultCode == Activity.RESULT_OK){
                val bitMapImagen = resultado.data?.extras?.get("data") as? Bitmap
                if(bitMapImagen != null){
                    val imagenURI = MediaStore.Images.Media.insertImage(
                        contentResolver,
                        bitMapImagen,
                        "Imagen",
                        "Imagen de la cuenta"
                    )
                    Glide.with(this).load(bitMapImagen).into(imagen)
                    guardarUriImagen(imagenURI.toString())
                    obtenerUriImagen()
                }
            }else{
                Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Código relacionado a la galería
    private fun abrirGaleria(){
        val intentGaleria = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncherGaleria.launch(intentGaleria)
    }

    private fun activarResultLauncherGaleria(imagen: ImageView){
        activityResultLauncherGaleria = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ resultado ->
            if(resultado.resultCode == Activity.RESULT_OK){
                val uri = resultado.data?.data
                //imagen.setImageURI(uri)
                Glide.with(this).load(uri).into(imagen)
                guardarUriImagen(uri.toString())
                obtenerUriImagen()
            }else{
                Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //URI de las imágenes
    private fun guardarUriImagen(uri: String) {
        val sharedPreferences = getSharedPreferences("preferencias_imagen", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("uri_imagen", uri)
        editor.apply()
    }

    private fun obtenerUriImagen(): String? {
        val sharedPreferences = getSharedPreferences("preferencias_imagen", MODE_PRIVATE)
        Log.i("URI", sharedPreferences.getString("uri_imagen", null).toString())
        return sharedPreferences.getString("uri_imagen", null)
    }

    //Código relacionado a la ubicación actual de la persona
    private fun localizaciónActual(){
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                Log.i("LOCATION", "Actualización")
                if(location!=null) {
                    bindingRegistro.LongitudDato.text = location.longitude.toString()
                    bindingRegistro.LatitudDato.text = location.latitude.toString()
                }else {
                    // La ubicación es nula
                    Log.i("LOCATION", "La ubicación es nula")
                }
            }
        }
    }

    private fun startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
    }

    private fun createLocationRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

    //Código relacionado al ciclo de vida

    override fun onPause() {
        super.onPause()
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        }
    }
}