package es.uniovi.imovil.fiestasasturias.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HistorialDao {

    // devolvemos ya ordenado para que la ui muestre primero lo último visitado.
    @Query("SELECT nombre FROM history ORDER BY updatedAt DESC LIMIT 20")
    suspend fun getHistoryNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(item: HistorialEntity)

    @Query("DELETE FROM history WHERE nombre NOT IN (SELECT nombre FROM history ORDER BY updatedAt DESC LIMIT 20)")
    suspend fun trimToLast20()

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
