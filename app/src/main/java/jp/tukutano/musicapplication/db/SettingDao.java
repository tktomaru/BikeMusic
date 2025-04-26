package jp.tukutano.musicapplication.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SettingDao {
    @Query("SELECT value FROM settings WHERE keyid = :key LIMIT 1")
    Float getValue(String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Setting setting);
}
