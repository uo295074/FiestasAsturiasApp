package es.uniovi.imovil.fiestasasturias.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    // usamos nombre como clave porque en el dataset actual identifica la fiesta.
    @PrimaryKey val nombre: String
)
