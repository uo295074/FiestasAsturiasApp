package es.uniovi.imovil.fiestasasturias.data

import es.uniovi.imovil.fiestasasturias.data.local.FavoriteDao
import es.uniovi.imovil.fiestasasturias.data.local.FavoriteEntity

class FavoritesRepository(
    private val favoriteDao: FavoriteDao
) {

    // devolvemos solo nombres porque es suficiente para marcar el estado favorito
    // sobre la lista principal que ya viene del repositorio remoto.
    suspend fun getFavoriteNames(): Set<String> = favoriteDao.getFavoriteNames().toSet()

    suspend fun setFavorite(nombre: String, isFavorite: Boolean) {
        // si ya existe el nombre, replace evita duplicados.
        if (isFavorite) {
            favoriteDao.insertFavorite(FavoriteEntity(nombre))
        } else {
            favoriteDao.deleteFavoriteByName(nombre)
        }
    }

    suspend fun clearAllFavorites() {
        favoriteDao.clearFavorites()
    }
}
