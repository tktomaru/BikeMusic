package jp.tukutano.musicapplication.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

import jp.tukutano.musicapplication.model.Song;

/**
 * MediaStore からオーディオファイル情報を取得するユーティリティクラス
 */
public class MusicUtils {

    /**
     * ストレージから全オーディオファイルのリストを取得する
     *
     * @param context コンテキスト
     * @return Song オブジェクトのリスト
     */
    public static List<Song> loadAllAudio(Context context) {
        List<Song> songs = new ArrayList<>();

        // Android 10+ では VOLUME 外部／内部を使い分け、Android 9 以下では従来の CONTENT_URI を使用
        String[] volumes;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            volumes = new String[]{
                    MediaStore.VOLUME_EXTERNAL_PRIMARY,
                    MediaStore.VOLUME_INTERNAL
            };
        } else {
            volumes = new String[]{
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(),
                    MediaStore.Audio.Media.INTERNAL_CONTENT_URI.toString()
            };
        }

        // 取得したいカラム
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION
        };
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        for (String vol : volumes) {
            // ボリュームごとに適切な collection URI を生成
            Uri collection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collection = MediaStore.Audio.Media.getContentUri(vol);
            } else {
                collection = Uri.parse(vol);
            }

            try (Cursor cursor = context.getContentResolver()
                    .query(collection, projection, null, null, sortOrder)) {
                if (cursor == null) continue;

                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String album = cursor.getString(albumCol);
                    long duration = cursor.getLong(durationCol);

                    // collection（外部 or 内部）ごとのベース URI で contentUri を生成
                    Uri contentUri = ContentUris.withAppendedId(collection, id);

                    songs.add(new Song(
                            String.valueOf(id),
                            title != null ? title : "Unknown",
                            artist != null ? artist : "Unknown",
                            album != null ? album : "Unknown",
                            contentUri.toString(),
                            duration
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return songs;
    }
}
