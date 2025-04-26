package jp.tukutano.musicapplication.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteSong song);

    @Delete
    void delete(FavoriteSong song);

    @Query("SELECT * FROM favorite_songs ORDER BY title")
    List<FavoriteSong> getAllFavorites();

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE id = :id)")
    boolean isFavorite(String id);
}