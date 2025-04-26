package jp.tukutano.musicapplication;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import jp.tukutano.musicapplication.db.FavoriteDao;
import jp.tukutano.musicapplication.db.FavoriteSong;
import jp.tukutano.musicapplication.db.SettingDao;
import jp.tukutano.musicapplication.db.Setting;

@Database(entities = {FavoriteSong.class, Setting.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract FavoriteDao favoriteDao();

    public abstract SettingDao settingDao();
}
