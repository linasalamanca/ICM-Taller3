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
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.*
import com.google.android.material.floatingactionbutton.FloatingActionButton

class UsuariosDisponiblesAdapter (pContext: Context, pActiveUsers: ArrayList<Usuario>, pImgMaps : ArrayList <Bitmap>) : Adapter<UsuariosDisponiblesAdapter.MyViewHolder>()
{
    var context : Context
    var usuariosActivos : ArrayList<Usuario>
    var profilePicBitmaps : ArrayList<Bitmap>

    init
    {
        this.context = pContext
        this.usuariosActivos = pActiveUsers
        this.profilePicBitmaps = pImgMaps
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder
    {
        val inflater : LayoutInflater = LayoutInflater.from(context)
        val view : View = inflater.inflate(R.layout.item_usuario_disopnible, parent, false)

        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int)
    {
        holder.nameTxtView.text = usuariosActivos[position].nombre.plus(" ").plus(usuariosActivos[position].apellido)
        holder.profileImg.setImageBitmap(profilePicBitmaps[position])
        holder.locationBtn.setOnClickListener {
            //val trackUserIntent = Intent(context, TrackUserActivity::class.java)
            //trackUserIntent.putExtra("trackedUid", activeUsers[position].uid)
            //context.startActivity(trackUserIntent)
        }
    }

    override fun getItemCount(): Int
    {
       // return activeUsers.size
        return 1;
    }


    class MyViewHolder(itemView: View) : ViewHolder(itemView)
    {
        val profileImg : ImageView = itemView.findViewById(R.id.cardProfilePicImg)
        val nameTxtView : TextView = itemView.findViewById(R.id.profileNameTxt)
        val locationBtn : FloatingActionButton = itemView.findViewById(R.id.userLocationBtn)
    }
}