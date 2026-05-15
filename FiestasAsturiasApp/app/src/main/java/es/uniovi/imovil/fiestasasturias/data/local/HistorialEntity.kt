package es.uniovi.imovil.fiestasasturias.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistorialEntity(
    // la clave evita duplicados; updatedAt nos permite ordenar por lo mas reciente que sea.
    @PrimaryKey val nombre: String,
    val updatedAt: Long
)
