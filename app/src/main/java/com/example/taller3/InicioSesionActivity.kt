package com.example.taller3

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.databinding.ActivityInicioSesionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class InicioSesionActivity : AppCompatActivity() {

    private lateinit var bindingIniSesion: ActivityInicioSesionBinding

    private lateinit var autenticacion: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio_sesion)
        bindingIniSesion = ActivityInicioSesionBinding.inflate(layoutInflater)
        setContentView(bindingIniSesion.root)
        autenticacion = Firebase.auth

        bindingIniSesion.botonInicioSesion.setOnClickListener {
            Log.i("CORRECCION", "Botón oprimido")
            val email = bindingIniSesion.CorreoInput.text.toString()
            val contrasena = bindingIniSesion.ContrasenaInput.text.toString()
            inicioSesion(email, contrasena)
        }

        bindingIniSesion.Registro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val usuarioActual = autenticacion.currentUser
        updateUI(usuarioActual)
    }

    private fun inicioSesion(email: String, contrasena: String){
        Log.i("CORRECCION", "Ingreso a inicioSesion")
        if(validarCampos() && emailValido(email)){
            Log.i("CORRECCION", "Ingreso al if")
            autenticacion.signInWithEmailAndPassword(email,contrasena)
                .addOnCompleteListener(this){ task ->
                    Log.i("CORRECCION", "Ingreso al lambda")
                    Log.d(TAG, "inicioCorreoSesion:onComplete:" + task.isSuccessful)
                    if(task.isSuccessful){
                        Log.d(TAG, "inicioCorreoSesion: success")
                        val usuario = autenticacion.currentUser
                        updateUI(usuario)
                    }else{
                        Log.w(TAG, "inicioCorreoSesion: failure", task.exception)
                        Toast.makeText(this, "Inicio de sesión fallido", Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        }
    }

    private fun validarCampos(): Boolean{
        var valid = true
        val email =bindingIniSesion.CorreoInput.text.toString()
        if(TextUtils.isEmpty(email)){
            bindingIniSesion.CorreoInput.error = "Requerido."
            valid = false
        }else{
            bindingIniSesion.CorreoInput.error = null
        }

        val contrasena = bindingIniSesion.ContrasenaInput.text.toString()
        if(TextUtils.isEmpty(contrasena)){
            bindingIniSesion.ContrasenaInput.error = "Requerido."
            valid = false
        }else{
            bindingIniSesion.ContrasenaInput.error = null
        }
        return valid
    }

    private fun emailValido(email: String): Boolean{
        if(!email.contains("@") || !email.contains(".") || email.length < 5){
            return false
        }
        return true
    }

    private fun updateUI(usuarioActual: FirebaseUser?) {
        if(usuarioActual != null){
            val intentInicio = Intent(this, MapaActivity::class.java)
            intentInicio.putExtra("usuario", usuarioActual.email)
            startActivity(intentInicio)
        }else{
            bindingIniSesion.CorreoInput.setText("")
            bindingIniSesion.ContrasenaInput.setText("")
        }
    }
}