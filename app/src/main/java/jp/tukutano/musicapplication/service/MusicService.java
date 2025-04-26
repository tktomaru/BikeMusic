package jp.tukutano.musicapplication.service;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.tukutano.musicapplication.R;

public class MusicService extends Service {
    // 現在再生している曲をFragmentに通知する
    public static final String ACTION_UPDATE_NOW_PLAYING =
            "jp.tukutano.musicapplication.ACTION_UPDATE_NOW_PLAYING";
    public static final String EXTRA_NOW_PLAYING = "EXTRA_NOW_PLAYING";
    // Volume
    public static final String ACTION_VOLUME_UP = "ACTION_VOLUME_UP";
    public static final String ACTION_VOLUME_DOWN = "ACTION_VOLUME_DOWN";
    public static final String EXTRA_VOLUME = "EXTRA_VOLUME";
    public static final String ACTION_PLAYLIST_PLAY = "ACTION_PLAYLIST_PLAY";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_PLAYLIST = "EXTRA_PLAYLIST";
    public static final String EXTRA_PLAYLIST_TITLE = "EXTRA_PLAYLIST_TITLE";

    public static final String EXTRA_START_INDEX = "EXTRA_START_INDEX";
    private MediaPlayer mediaPlayer;
    private List<String> playlist = new ArrayList<>();
    private List<String> playTitlelist = new ArrayList<>();
    private int currentIndex = 0;
    private float currentVolume = 1.0f;  // 0.0f〜1.0f

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> stopSelf());
        mediaPlayer.setOnCompletionListener(mp -> playNext());
        // 初期音量設定
        mediaPlayer.setVolume(currentVolume, currentVolume);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            // システム再起動時などの null Intent は何もしないで継続起動
            return START_STICKY;
        }

        String action = intent.getAction();

        switch (action) {
            case ACTION_PLAYLIST_PLAY:
                // プレイリストと開始インデックスを受け取る
                playlist = intent.getStringArrayListExtra(EXTRA_PLAYLIST);
                playTitlelist = intent.getStringArrayListExtra(EXTRA_PLAYLIST_TITLE);
                currentIndex = intent.getIntExtra(EXTRA_START_INDEX, 0);
                if (playlist != null && !playlist.isEmpty()) {
                    playInService(playlist.get(currentIndex));
                }
                break;

            case ACTION_STOP:
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                break;

            case ACTION_VOLUME_UP:
            case ACTION_VOLUME_DOWN:
                // Intent から最新の currentVolume を取得
                float vol = intent.getFloatExtra(EXTRA_VOLUME, currentVolume);
                // +/-0.1 した後、範囲 clamp
                if (ACTION_VOLUME_UP.equals(action)) {
                    currentVolume = Math.min(1.0f, vol + 0.001f);
                } else {
                    currentVolume = Math.max(0.0f, vol - 0.001f);
                }
                // MediaPlayer に適用
                mediaPlayer.setVolume(currentVolume, currentVolume);
                // 通知を更新して新しいボリュームを添付
                startForeground(1, buildNotification());
                break;
        }
        return START_STICKY;
    }

    /**
     * 指定 URI で再生を開始し、Foreground 通知を出す
     */
    private void playInService(String uriString) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, Uri.parse(uriString));
            mediaPlayer.prepare();
            mediaPlayer.setLooping(false);  // 単一曲ループはOFF
            mediaPlayer.start();
            startForeground(1, buildNotification());

            // ▶▶▶ 追加：現在再生中の曲タイトルを Broadcast
            String title = playTitlelist.get(currentIndex); /* songタイトルをサービスで保持している変数から取得 */
            ;
            Intent update = new Intent(ACTION_UPDATE_NOW_PLAYING);
            update.putExtra(EXTRA_NOW_PLAYING, title);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(update);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 再生完了コールバック → 次の曲を再生
     */
    private void playNext() {
        if (playlist == null || playlist.isEmpty()) {
            stopSelf();
            return;
        }
        currentIndex = (currentIndex + 1) % playlist.size();
        playInService(playlist.get(currentIndex));
    }

    private Notification buildNotification() {
        String channelId = createNotificationChannel();
        Intent stopIntent = new Intent(this, MusicService.class)
                .setAction(ACTION_STOP);
        PendingIntent pStop = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        String title = playTitlelist.get(currentIndex);
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("再生中の音楽")
                .setContentText( title )
                .setSmallIcon(R.drawable.ic_favorite)
                .addAction(new NotificationCompat.Action(
                        android.R.drawable.ic_media_pause,
                        "停止",
                        pStop))
                .setOngoing(true)
                .build();
    }

    private String createNotificationChannel() {
        String id = "music_playback_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    id, "Music Playback", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(chan);
        }
        return id;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}