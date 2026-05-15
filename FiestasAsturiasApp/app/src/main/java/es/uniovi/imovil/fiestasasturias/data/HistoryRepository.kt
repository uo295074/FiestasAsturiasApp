package es.uniovi.imovil.fiestasasturias.data

import es.uniovi.imovil.fiestasasturias.data.local.HistorialDao
import es.uniovi.imovil.fiestasasturias.data.local.HistorialEntity

class HistoryRepository(
    private val historialDao: HistorialDao
) {

    suspend fun getHistoryNames(): List<String> = historialDao.getHistoryNames()

    suspend fun addToHistory(nombre: String) {
        // se usa upsert para que, si el elemento ya estaba, solo se refresque updatedAt
        // y suba a primera posición al ordenar por fecha.
        historialDao.upsertHistory(
            HistorialEntity(
                nombre = nombre,
                updatedAt = System.currentTimeMillis()
            )
        )
        // mantenemos el historial acotado para no crecer indefinidamente.
        historialDao.trimToLast20()
    }

    suspend fun clearAllHistory() {
        historialDao.clearHistory()
    }
}
