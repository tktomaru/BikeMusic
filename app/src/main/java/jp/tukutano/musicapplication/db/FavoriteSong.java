package jp.tukutano.musicapplication.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_songs")
public class FavoriteSong {
    @PrimaryKey
    @NonNull
    public String id;       // Song.id と同じ値

    public String title;
    public String artist;
    public String album;
    public String uri;
    public long duration;
}
