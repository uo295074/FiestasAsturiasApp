package es.uniovi.imovil.fiestasasturias.data

import es.uniovi.imovil.fiestasasturias.data.local.HistorialDao
import es.uniovi.imovil.fiestasasturias.data.local.HistorialEntity

class HistoryRepository(
    private val historialDao: HistorialDao
) {

    suspend fun getHistoryNames(): List<String> = historialDao.getHistoryNames()

    suspend fun addToHistory(nombre: String) {
        historialDao.upsertHistory(
            HistorialEntity(
                nombre = nombre,
                updatedAt = System.currentTimeMillis()
            )
        )
        historialDao.trimToLast20()
    }
}
