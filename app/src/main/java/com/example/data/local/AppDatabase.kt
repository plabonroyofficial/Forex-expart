package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.PositionStatus
import com.example.data.model.TradePosition
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_pairs")
data class FavoritePairEntity(
    @PrimaryKey val symbol: String
)

@Dao
interface TradeDao {
    @Query("SELECT * FROM trade_positions ORDER BY openTimestamp DESC")
    fun getAllPositions(): Flow<List<TradePosition>>

    @Query("SELECT * FROM trade_positions WHERE status = :status ORDER BY openTimestamp DESC")
    fun getPositionsByStatus(status: PositionStatus): Flow<List<TradePosition>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: TradePosition): Long

    @Update
    suspend fun updatePosition(position: TradePosition)

    @Query("DELETE FROM trade_positions WHERE id = :id")
    suspend fun deletePosition(id: Long)

    @Query("DELETE FROM trade_positions")
    suspend fun clearAllTrades()

    // Favorites
    @Query("SELECT symbol FROM favorite_pairs")
    fun getFavorites(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoritePairEntity)

    @Query("DELETE FROM favorite_pairs WHERE symbol = :symbol")
    suspend fun removeFavorite(symbol: String)
}

@Database(
    entities = [TradePosition::class, FavoritePairEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
}
