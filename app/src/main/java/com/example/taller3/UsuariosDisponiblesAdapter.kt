package com.example.taller3


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.*
import com.google.android.material.floatingactionbutton.FloatingActionButton

class UsuariosDisponiblesAdapter(
    pContext: Context,
    pActiveUsers: ArrayList<Usuario>,
    profilePicsBitmaps: ArrayList<Bitmap>
) : Adapter<UsuariosDisponiblesAdapter.MyViewHolder>() {
    var context : Context = pContext
    var usuariosActivos : ArrayList<Usuario> = pActiveUsers

    init {
        // Considera añadir aquí un listener a Firebase para actualizar usuariosActivos
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.item_usuario_disopnible, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val usuario = usuariosActivos[position]
        holder.nameTxtView.text = "${usuario.nombre} ${usuario.apellido}"
        // Considera usar Glide o Picasso para cargar imágenes desde una URL si está en Firebase Storage
        holder.locationBtn.setOnClickListener {
            val trackUserIntent = Intent(context, TrackUserActivity::class.java)
            trackUserIntent.putExtra("trackedUid", usuario.uid)  // Asegúrate de que Usuario.kt tenga un uid
            context.startActivity(trackUserIntent)
        }
    }

    override fun getItemCount(): Int {
        return usuariosActivos.size
    }

    class MyViewHolder(itemView: View) : ViewHolder(itemView) {
        val profileImg : ImageView = itemView.findViewById(R.id.cardProfilePicImg)
        val nameTxtView : TextView = itemView.findViewById(R.id.profileNameTxt)
        val locationBtn : FloatingActionButton = itemView.findViewById(R.id.userLocationBtn)
    }
}
