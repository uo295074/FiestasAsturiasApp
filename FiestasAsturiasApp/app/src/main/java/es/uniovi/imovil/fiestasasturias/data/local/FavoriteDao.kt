package es.uniovi.imovil.fiestasasturias.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavoriteDao {

    // con esta consulta ligera solo movemos nombres, no objetos completos.
    @Query("SELECT nombre FROM favorites")
    suspend fun getFavoriteNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE nombre = :nombre")
    suspend fun deleteFavoriteByName(nombre: String)

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()
}
