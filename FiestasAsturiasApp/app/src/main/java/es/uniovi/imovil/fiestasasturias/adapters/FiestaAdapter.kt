package es.uniovi.imovil.fiestasasturias.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import es.uniovi.imovil.fiestasasturias.model.Fiesta
import es.uniovi.imovil.fiestasasturias.R

class FiestaAdapter(
    private val onClick: (Fiesta) -> Unit
) : RecyclerView.Adapter<FiestaAdapter.ViewHolder>() {

    private var lista: List<Fiesta> = emptyList()

    fun setData(nuevaLista: List<Fiesta>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image)
        val title: TextView = view.findViewById(R.id.title)
        val location: TextView = view.findViewById(R.id.location)
        val description: TextView = view.findViewById(R.id.description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fiesta, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fiesta = lista[position]

        holder.title.text = fiesta.nombre
        holder.location.text = fiesta.localidad
        holder.description.text = fiesta.descripcion

        Glide.with(holder.itemView.context)
            .load(fiesta.imagen)
            .centerCrop()
            .into(holder.image)

        // 🎯 CLICK
        holder.itemView.setOnClickListener {
            onClick(fiesta)
        }

        // ✨ ANIMACIÓN PRO (entrada suave)
        holder.itemView.apply {
            alpha = 0f
            translationY = 80f

            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
}