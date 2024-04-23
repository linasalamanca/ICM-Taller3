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
        autenticacion = Firebase.auth

        inicioSesion()
    }

    override fun onStart() {
        super.onStart()
        val usuarioActual = autenticacion.currentUser
        updateUI(usuarioActual)
    }

    private fun inicioSesion(){
        autenticacion.signInWithEmailAndPassword(bindingIniSesion.CorreoInput.text.toString(),
            bindingIniSesion.ContrasenaInput.text.toString())
            .addOnCompleteListener(this){ task ->
                Log.d(TAG, "inicioCorreoSesion:onComplete:" + task.isSuccessful)
                if(task.isSuccessful){
                    Log.w(TAG, "inicioCorreoSesion: failure", task.exception)
                    Toast.makeText(this, "Inicio de sesión fallido", Toast.LENGTH_SHORT).show()
                    bindingIniSesion.CorreoInput.setText("")
                    bindingIniSesion.ContrasenaInput.setText("")
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