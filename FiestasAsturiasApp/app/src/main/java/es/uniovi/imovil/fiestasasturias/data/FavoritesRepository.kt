package es.uniovi.imovil.fiestasasturias.data

import es.uniovi.imovil.fiestasasturias.data.local.FavoriteDao
import es.uniovi.imovil.fiestasasturias.data.local.FavoriteEntity

class FavoritesRepository(
    private val favoriteDao: FavoriteDao
) {

    suspend fun getFavoriteNames(): Set<String> = favoriteDao.getFavoriteNames().toSet()

    suspend fun setFavorite(nombre: String, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteDao.insertFavorite(FavoriteEntity(nombre))
        } else {
            favoriteDao.deleteFavoriteByName(nombre)
        }
    }
}
