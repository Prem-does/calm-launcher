package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.calmlauncher.data.db.entity.AppMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppMetaDao {

    @Query("SELECT * FROM app_meta")
    fun observeAll(): Flow<List<AppMetaEntity>>

    @Query("SELECT * FROM app_meta")
    suspend fun getAll(): List<AppMetaEntity>

    @Query("SELECT * FROM app_meta WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): AppMetaEntity?

    @Query("SELECT * FROM app_meta WHERE isFavorite = 1 ORDER BY favoriteOrder ASC")
    suspend fun getFavorites(): List<AppMetaEntity>

    @Upsert
    suspend fun upsert(entity: AppMetaEntity)

    @Upsert
    suspend fun upsertAll(entities: List<AppMetaEntity>)

    /** Insert only rows that don't yet exist (used to seed metadata for newly installed apps). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entities: List<AppMetaEntity>)

    @Query("UPDATE app_meta SET isHidden = :hidden WHERE packageName = :packageName")
    suspend fun setHidden(packageName: String, hidden: Boolean)

    @Query("UPDATE app_meta SET isFavorite = :favorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, favorite: Boolean)

    @Query("UPDATE app_meta SET favoriteOrder = :order WHERE packageName = :packageName")
    suspend fun setFavoriteOrder(packageName: String, order: Int)

    @Query("UPDATE app_meta SET category = :category, isDistractingOverride = NULL WHERE packageName = :packageName")
    suspend fun setCategory(packageName: String, category: String)

    @Query("DELETE FROM app_meta WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
