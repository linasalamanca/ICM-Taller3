package com.example.taller3


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UsuariosDisponiblesAdapter(
    pContext: Context,
    pActiveUsers: ArrayList<Usuario>,
    profilePicsBitmaps: ArrayList<Bitmap>
) : Adapter<UsuariosDisponiblesAdapter.MyViewHolder>() {
    var context : Context = pContext
    var usuariosActivos : ArrayList<Usuario> = pActiveUsers

    init {
        val databaseReference = FirebaseDatabase.getInstance().getReference("usuariosActivos")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usuariosActivos.clear()
                for (postSnapshot in snapshot.children) {
                    val usuario = postSnapshot.getValue(Usuario::class.java)
                    usuario?.let { usuariosActivos.add(it) }
                }
                notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("FirebaseAdapter", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.item_usuario_disopnible, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val usuario = usuariosActivos[position]
        holder.nameTxtView.text = "${usuario.nombre} ${usuario.apellido}"
        holder.locationBtn.setOnClickListener {
            val trackUserIntent = Intent(context, TrackUserActivity::class.java)
            trackUserIntent.putExtra("trackedid", usuario.numeroIdentificacion)
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
