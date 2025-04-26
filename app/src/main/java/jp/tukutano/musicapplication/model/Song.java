package jp.tukutano.musicapplication.model;

/**
 * 音楽ファイルを表すモデルクラス
 */
public class Song {
    private final String id;
    private final String title;
    private final String artist;
    private final String album;
    private final String uri;
    private final long duration;

    /**
     * コンストラクタ
     *
     * @param id       MediaStore での ID
     * @param title    曲タイトル
     * @param artist   アーティスト名
     * @param album    アルバム名
     * @param uri      再生用の URI
     * @param duration 再生時間（ミリ秒）
     */
    public Song(String id, String title, String artist, String album, String uri, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.uri = uri;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getUri() {
        return uri;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "Song{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", uri='" + uri + '\'' +
                ", duration=" + duration +
                '}';
    }
}
