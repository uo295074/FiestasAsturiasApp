package es.uniovi.imovil.fiestasasturias.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import es.uniovi.imovil.fiestasasturias.R

class DetailImagePagerAdapter(
    private val images: List<String>
) : RecyclerView.Adapter<DetailImagePagerAdapter.ImageViewHolder>() {

    class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detail_image, parent, false) as ImageView
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        // cada página del carrusel carga su imagen por url.
        // si tarda en cargar, mostramos placeholder para no dejar hueco vacío.
        Glide.with(holder.itemView.context)
            .load(images[position])
            .placeholder(R.drawable.ic_launcher_background)
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = images.size
}
