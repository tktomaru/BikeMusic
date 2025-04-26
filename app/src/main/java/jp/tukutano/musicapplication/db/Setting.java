package jp.tukutano.musicapplication.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "settings")
public class Setting {
    @PrimaryKey
    @NonNull
    public String keyid;      // 例："volume"
    public float value;     // 0.0f 〜 1.0f

    /**
     * Room がインスタンスを生成できるように必須の空コンストラクタ
     */
    public Setting() {
    }

    public Setting(@NonNull String key, float value) {
        this.keyid = key;
        this.value = value;
    }
}
