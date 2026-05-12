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

    private class Migration1To2 : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS history (nombre TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(nombre))"
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
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
