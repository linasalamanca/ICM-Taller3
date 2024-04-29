package com.example.taller3

import com.example.taller3.MapaActivity.Companion.PATH_USERS


import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import com.example.taller3.UsuariosDisponiblesAdapter

class UsuariosActivosActivity : AppCompatActivity()
{

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    private val storage = Firebase.storage
    private lateinit var availableUsers: ArrayList<Usuario>
    private lateinit var profilePicsBitmaps: ArrayList<Bitmap>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usuarios_activos)
        auth = Firebase.auth
        myRef = database.getReference(PATH_USERS)
        availableUsers = ArrayList<Usuario>()
        profilePicsBitmaps = ArrayList<Bitmap>()

        val recViewUsers: RecyclerView = findViewById(R.id.usuariosDispRecView)
        val recViewAdapter = UsuariosDisponiblesAdapter(this, availableUsers, profilePicsBitmaps)
        recViewUsers.adapter = recViewAdapter
        recViewUsers.layoutManager = LinearLayoutManager(this)

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                GlobalScope.launch(Dispatchers.IO) {
                    availableUsers.clear()
                    profilePicsBitmaps.clear()
                    for (userSnapshot in dataSnapshot.children) {
                        val user = userSnapshot.getValue(Usuario::class.java)
                        if (auth.currentUser != null && user != null && user.disponible && user.numeroIdentificacion.toString() != auth.currentUser!!.uid) {
                            val imageRef = storage.reference.child("images/profile/${user.numeroIdentificacion}/image.jpg")
                            val bitmap = downloadImageFromUrl(imageRef.downloadUrl.await())
                            if (bitmap != null) {
                                withContext(Dispatchers.Main) {
                                    availableUsers.add(user)
                                    profilePicsBitmaps.add(bitmap)
                                    recViewAdapter.notifyDataSetChanged()
                                }
                            } else {
                                Log.e(TAG, "Failed to download image for user ${user.numeroIdentificacion}")
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                System.out.println("No longer authenticated")
            }
        })
    }



    private suspend fun downloadImageFromUrl(url: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = URL(url.toString()).openStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bitmap
            } catch (e: IOException) {
                Log.e(TAG, "Failed to download image from URL $url", e)
                null
            }
        }
    }


}