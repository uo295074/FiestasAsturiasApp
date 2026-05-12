package es.uniovi.imovil.fiestasasturias.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistorialEntity(
    @PrimaryKey val nombre: String,
    val updatedAt: Long
)
