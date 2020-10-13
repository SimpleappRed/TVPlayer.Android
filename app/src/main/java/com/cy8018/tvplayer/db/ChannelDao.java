package com.cy8018.tvplayer.db;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ChannelDao {
    @Query("SELECT * FROM channels")
    List<ChannelData> getAll();

    @Query("SELECT * FROM channels WHERE is_favorite = 1")
    List<ChannelData> loadAllFavorites();

    @Query("SELECT last_source FROM channels WHERE name = :name")
    int getLastSource(String name);

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :name || '%'")
    List<ChannelData> findAllByName(String name);

    @Query("UPDATE channels Set is_favorite = 1 WHERE name = :name")
    void addToFavorites(String name);

    @Query("UPDATE channels Set is_favorite = 0 WHERE name = :name")
    void removeFromFavorites(String name);

    @Query("UPDATE channels Set last_source = :source WHERE name = :name")
    void setLastSource(String name, int source);

    @Query("delete from channels")
    void removeAll();

    @Insert
    void insert(ChannelData channel);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChannelData> channels);

    @Update
    void update(ChannelData channel);

    @Delete
    void delete(ChannelData channel);
}
