package es.uniovi.imovil.fiestasasturias.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FavoriteEntity::class, HistorialEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun historialDao(): HistorialDao

    //version 1 tenia solo la tabla favorites, version 2 añado tambien la tabla histtorial
    private class Migration1To2 : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // al subir de versión añadimos la tabla de historial sin perder favoritos existentes.
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS history (nombre TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(nombre))"
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // patrón singleton para reutilizar una única instancia de room en toda la app.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fiestas_db"
                ).addMigrations(Migration1To2()).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
