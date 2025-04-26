package jp.tukutano.musicapplication.service;

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

/**
 * Foreground Service で音楽をバックグラウンド再生するサービスクラス
 * - プレイリストのループ再生
 * - 通知領域へのコントロールボタン表示
 * - 音量調整、現在再生中タイトルのブロードキャスト通知
 */
public class MusicService extends Service {
    // フラグメントへ送るアクション／エクストラ定義
    public static final String ACTION_UPDATE_NOW_PLAYING =
            "jp.tukutano.musicapplication.ACTION_UPDATE_NOW_PLAYING";  // 現在再生中タイトル更新
    public static final String EXTRA_NOW_PLAYING = "EXTRA_NOW_PLAYING";  // タイトル文字列
    public static final String EXTRA_NOW_LOOP_POS = "EXTRA_NOW_LOOP_POS"; // プレイリスト内インデックス

    // 音量操作アクション
    public static final String ACTION_VOLUME_UP = "ACTION_VOLUME_UP";
    public static final String ACTION_VOLUME_DOWN = "ACTION_VOLUME_DOWN";
    // タイトル更新リクエスト
    public static final String ACTION_TITLE = "ACTION_TITLE";
    public static final String EXTRA_VOLUME = "EXTRA_VOLUME";
    // プレイリスト再生
    public static final String ACTION_PLAYLIST_PLAY = "ACTION_PLAYLIST_PLAY";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_PLAYLIST = "EXTRA_PLAYLIST";
    public static final String EXTRA_PLAYLIST_TITLE = "EXTRA_PLAYLIST_TITLE";
    public static final String EXTRA_START_INDEX = "EXTRA_START_INDEX";

    // MediaPlayer とプレイリスト関連
    private MediaPlayer mediaPlayer;
    private List<String> playlist = new ArrayList<>();           // 音源URIリスト
    private List<String> playTitlelist = new ArrayList<>();      // タイトルリスト
    private int currentIndex = 0;                                // 再生中曲のインデックス
    private float currentVolume = 1.0f;                          // 音量 (0.0〜1.0)

    /**
     * Service 作成時に呼ばれる
     * - MediaPlayer 初期化
     * - 曲終了時に次曲再生をセット
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        // 曲が終わったら playNext() を呼ぶ
        mediaPlayer.setOnCompletionListener(mp -> playNext());
        // 初期音量を設定
        mediaPlayer.setVolume(currentVolume, currentVolume);
    }

    /**
     * startService や startForegroundService の呼び出し
     * @return 再起動時の動作指示
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Intent が null なら何もしないで継続
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }
        String action = intent.getAction();

        switch (action) {
            case ACTION_PLAYLIST_PLAY:
                // プレイリスト再生開始
                playlist = intent.getStringArrayListExtra(EXTRA_PLAYLIST);
                playTitlelist = intent.getStringArrayListExtra(EXTRA_PLAYLIST_TITLE);
                currentIndex = intent.getIntExtra(EXTRA_START_INDEX, 0);
                if (playlist != null && !playlist.isEmpty()) {
                    playInService(playlist.get(currentIndex));
                }
                break;

            case ACTION_STOP:
                // フォアグラウンド通知を消してサービス停止
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                break;

            case ACTION_VOLUME_UP:
            case ACTION_VOLUME_DOWN:
                // 音量調整
                float vol = intent.getFloatExtra(EXTRA_VOLUME, currentVolume);
                if (ACTION_VOLUME_UP.equals(action)) {
                    currentVolume = Math.min(1.0f, vol + 0.001f);
                } else {
                    currentVolume = Math.max(0.0f, vol - 0.001f);
                }
                mediaPlayer.setVolume(currentVolume, currentVolume);
                // 通知を再構築して更新
                startForeground(1, buildNotification());
                break;

            case ACTION_TITLE:
                // フラグメントへ現在のタイトルとインデックスをブロードキャスト
                if (!playTitlelist.isEmpty()) {
                    String title = playTitlelist.get(currentIndex);
                    Intent update = new Intent(ACTION_UPDATE_NOW_PLAYING);
                    update.putExtra(EXTRA_NOW_PLAYING, title);
                    update.putExtra(EXTRA_NOW_LOOP_POS, currentIndex);
                    LocalBroadcastManager.getInstance(this)
                            .sendBroadcast(update);
                }
                break;
        }
        return START_STICKY;
    }

    /**
     * 実際の再生処理
     * @param uriString 再生する音源の URI 文字列
     */
    private void playInService(String uriString) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, Uri.parse(uriString));
            mediaPlayer.prepare();
            mediaPlayer.setLooping(false);
            mediaPlayer.start();
            // フォアグラウンド通知を表示
            startForeground(1, buildNotification());

            // タイトル更新をブロードキャスト
            String title = playTitlelist.get(currentIndex);
            Intent update = new Intent(ACTION_UPDATE_NOW_PLAYING);
            update.putExtra(EXTRA_NOW_PLAYING, title);
            update.putExtra(EXTRA_NOW_LOOP_POS, currentIndex);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 次の曲を再生 (ループ再生)
     */
    private void playNext() {
        if (playlist == null || playlist.isEmpty()) {
            stopSelf();
            return;
        }
        currentIndex = (currentIndex + 1) % playlist.size();
        playInService(playlist.get(currentIndex));
    }

    /**
     * 通知を構築
     * @return Notification オブジェクト
     */
    private Notification buildNotification() {
        String channelId = createNotificationChannel();
        // 停止用 PendingIntent
        Intent stopIntent = new Intent(this, MusicService.class)
                .setAction(ACTION_STOP);
        PendingIntent pStop = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        String title = playTitlelist.get(currentIndex);
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("再生中の音楽")
                .setContentText(title)
                .setSmallIcon(R.drawable.ic_notification)
                .addAction(android.R.drawable.ic_media_pause, "停止", pStop)
                .setOngoing(true)
                .build();
    }

    /**
     * 通知チャンネルを作成 (Android O+)
     * @return チャンネルID
     */
    private String createNotificationChannel() {
        String id = "music_playback_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    id, "Music Playback", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(chan);
        }
        return id;
    }

    /**
     * Service 解放時に MediaPlayer を停止・リリース
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    /**
     * バインドサービスではないため null を返却
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}