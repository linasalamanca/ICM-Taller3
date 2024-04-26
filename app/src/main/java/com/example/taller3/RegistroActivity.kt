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
import com.google.firebase.auth.FirebaseAuth
import android.content.ContentValues.TAG
import android.text.TextUtils
import androidx.core.net.toUri
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage

class RegistroActivity : AppCompatActivity() {

    //Companion object, carpeta users para el realtime database
    companion object{
        const val PATH_USERS="users/"
    }

    private lateinit var bindingRegistro: ActivityRegistroBinding

    //Registro autenticación, realtime database y storage
    private lateinit var autenticacion: FirebaseAuth
    private lateinit var storage: FirebaseStorage

    //Camara y Galería
    private lateinit var activityResultLauncherCamara: ActivityResultLauncher<Intent>
    private lateinit var activityResultLauncherGaleria: ActivityResultLauncher<Intent>

    //Ubicación actual
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    //Realtime Database
    private val database = FirebaseDatabase.getInstance()
    private lateinit var referencia: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        bindingRegistro = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(bindingRegistro.root)
        autenticacion = Firebase.auth
        storage = Firebase.storage

        guardarUriImagen(null)

        val correo = bindingRegistro.CorreoInputR
        val contrasena = bindingRegistro.ContrasenaInputR
        val btnCamara = bindingRegistro.CamaraBoton
        val btnGaleria = bindingRegistro.GaleriaBoton
        val imagenCuenta = bindingRegistro.imagenCargada
        val btnRegistro = bindingRegistro.BotonRegistro

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        localizaciónActual()
        permisosUbicacion()

        activarResultLauncherCamara(imagenCuenta)
        activarResultLauncherGaleria(imagenCuenta)

        btnCamara.setOnClickListener {
            permisosCamara()
        }

        btnGaleria.setOnClickListener {
            permisosGaleria()
        }

        btnRegistro.setOnClickListener {
            verificarCorreoExistente(correo.text.toString()){estadoCorreo ->
                if (validarCampos() && !estadoCorreo){
                    registroUsarioAuthentication(correo.text.toString(), contrasena.text.toString())
                }
            }
        }
    }

    //Validación de campos llenados

    private fun verificarCorreoExistente(correo: String, onComplete: (Boolean) -> Unit) {
        autenticacion.fetchSignInMethodsForEmail(correo)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods?.isEmpty() == true) {
                        Toast.makeText(this, "El correo electrónico está disponible para registro", Toast.LENGTH_SHORT).show()
                        onComplete(false)
                    } else {
                        Toast.makeText(this, "El correo electrónico ya está registrado", Toast.LENGTH_SHORT).show()
                        onComplete(true)
                    }
                } else {
                    // Error al verificar el correo electrónico
                    Toast.makeText(this, "Error al verificar el correo electrónico", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }
    }

    private fun validarCampos(): Boolean{
        var valid = true

        val uriImagen = obtenerUriImagen()
        if(uriImagen == null){
            Toast.makeText(this, "Imagen requerida", Toast.LENGTH_SHORT).show()
            valid = false
        }

        val nombre = bindingRegistro.NombreInputR.text.toString()
        if(TextUtils.isEmpty(nombre)){
            bindingRegistro.NombreInputR.error = "Requerido."
            valid = false
        }else{
            bindingRegistro.NombreInputR.error = null
        }

        val apellido = bindingRegistro.ApellidoInputR.text.toString()
        if(TextUtils.isEmpty(apellido)){
            bindingRegistro.ApellidoInputR.error = "Requerido."
            valid = false
        }else{
            bindingRegistro.ApellidoInputR.error = null
        }

        val email = bindingRegistro.CorreoInputR.text.toString()
        if(TextUtils.isEmpty(email)){
            bindingRegistro.CorreoInputR.error = "Requerido."
            valid = false
        }else{
            bindingRegistro.CorreoInputR.error = null
        }

        val contrasena = bindingRegistro.ContrasenaInputR.text.toString()
        if(TextUtils.isEmpty(contrasena)){
            bindingRegistro.ContrasenaInputR.error = "Requerido."
            valid = false
        }else{
            bindingRegistro.ContrasenaInputR.error = null
        }

        val noID = bindingRegistro.IDInputR.text.toString()
        if(TextUtils.isEmpty(noID)){
            bindingRegistro.IDInputR.error = "Requerido."
            valid = false
        }else{
            bindingRegistro.IDInputR.error = null
        }

        return valid
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
    private fun guardarUriImagen(uri: String?) {
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

    //Código de registro
    private fun registroUsarioAuthentication(email: String, contrasena: String){
        autenticacion.createUserWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener(this){task ->
                if(task.isSuccessful){
                    Log.d(TAG, "crearUsuarioCorreo: onComplete: " + task.isSuccessful)
                    val usuario = autenticacion.currentUser
                    if(usuario != null){
                        val actualizacionUsarios = UserProfileChangeRequest.Builder()
                        actualizacionUsarios.setDisplayName(email)
                        usuario.updateProfile(actualizacionUsarios.build())
                        updateUI(usuario)
                        registrarUsuarioRealtimeDatabase()
                        registrarUsuarioFirebaseStorage()
                    }
                }else{
                    Toast.makeText(this, "Registro fallido", Toast.LENGTH_SHORT).show()
                    task?.exception?.message?.let { Log.w(TAG, it)}
                }
            }
    }

    private fun updateUI(usuarioActual: FirebaseUser?) {
        if(usuarioActual != null){
            val intentInicio = Intent(this, MapaActivity::class.java)
            intentInicio.putExtra("usuario", usuarioActual.email)
            startActivity(intentInicio)
        }
    }

    //Código relacionado con el realtime database
    private fun registrarUsuarioRealtimeDatabase(){
        val nombre = bindingRegistro.NombreInputR
        val apellido = bindingRegistro.ApellidoInputR
        val id = bindingRegistro.IDInputR
        val latitud = bindingRegistro.LatitudDato
        val longitud = bindingRegistro.LongitudDato

        val usuarioRegistro = Usuario()
        usuarioRegistro.nombre = nombre.text.toString()
        usuarioRegistro.apellido = apellido.text.toString()
        usuarioRegistro.numeroIdentificacion = id.text.toString().toLong()
        usuarioRegistro.latitud = latitud.text.toString().toDouble()
        usuarioRegistro.longitud = longitud.text.toString().toDouble()
        usuarioRegistro.disponible = true
        referencia = database.getReference(PATH_USERS+autenticacion.currentUser!!.uid)
        referencia.setValue(usuarioRegistro)
    }

    //Código relacionado a la carga de imagen en Firebase Storage
    private fun registrarUsuarioFirebaseStorage(){
        val uriImagen = obtenerUriImagen()
        val imagenRef = storage.reference.child("images/${autenticacion.currentUser!!.uid}")
        imagenRef.putFile(uriImagen!!.toUri())
            .addOnSuccessListener(object: OnSuccessListener<UploadTask.TaskSnapshot>{
                override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot){
                    Log.i("STORAGE", "Imagen cargada exitosamente")
                }
            })
            .addOnFailureListener(object: OnFailureListener {
                override fun onFailure(exception: Exception){

                }
            })
    }

    //Código relacionado al ciclo de vida
    override fun onPause() {
        super.onPause()
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        }
    }
}