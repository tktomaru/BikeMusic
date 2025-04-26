package jp.tukutano.musicapplication.ui.home;

import static jp.tukutano.musicapplication.service.MusicService.ACTION_TITLE;
import static jp.tukutano.musicapplication.service.MusicService.ACTION_VOLUME_UP;
import static jp.tukutano.musicapplication.service.MusicService.EXTRA_VOLUME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;

import jp.tukutano.musicapplication.databinding.FragmentHomeBinding;
import jp.tukutano.musicapplication.db.FavoriteDao;
import jp.tukutano.musicapplication.db.FavoriteSong;
import jp.tukutano.musicapplication.db.Setting;
import jp.tukutano.musicapplication.db.SettingDao;
import jp.tukutano.musicapplication.model.Song;
import jp.tukutano.musicapplication.AppDatabase;
import jp.tukutano.musicapplication.service.MusicService;
import jp.tukutano.musicapplication.ui.dashboard.SongAdapter;
import jp.tukutano.musicapplication.util.LogUtils;

/**
 * HomeFragment
 * - お気に入り曲のリスト表示
 * - 再生/停止/音量調整
 * - 現在再生中の曲タイトルを通知領域で受信
 */
public class HomeFragment extends Fragment {

    // ViewBinding オブジェクト
    private FragmentHomeBinding binding;
    // Room データベース
    private AppDatabase db;
    // お気に入り DAO
    private FavoriteDao favoriteDao;
    // 設定 DAO（音量保存用）
    private SettingDao settingDao;
    // MediaPlayer（UI上のプレイ止め用、一時的に利用）
    private MediaPlayer mediaPlayer;
    // お気に入り曲リスト
    private List<Song> favSongs;
    // 再生中の曲インデックス
    private int currentIndex = 0;
    // 音量調整用 SeekBar
    private SeekBar seekBarVolume;
    // 現在のアプリ内音量（0.0〜1.0）
    private Float currentVolume = 0.5f;
    // 曲タイトル受信用 BroadcastReceiver
    private BroadcastReceiver nowPlayingReceiver;

    /**
     * フラグメントのビュー生成
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // ログ出力（デバッグ）
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "onCreateView");

        // ViewModel の初期化（必要に応じて利用）
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        // ViewBinding の初期化
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Room データベースのビルダー
        db = Room.databaseBuilder(
                        requireContext().getApplicationContext(),
                        AppDatabase.class, "music_app_db")
                .allowMainThreadQueries()  // 簡易実装: メインスレッドで許可
                .build();
        favoriteDao = db.favoriteDao();
        settingDao = db.settingDao();

        // 保存された音量を読み込み、なければデフォルト0.5f
        Float saved = settingDao.getValue("volume");
        currentVolume = (saved != null) ? saved : 0.5f;

        // RecyclerView のレイアウトマネージャ設定
        binding.recyclerFav.setLayoutManager(new LinearLayoutManager(getContext()));

        // 永続化された FavoriteSong をモデルト変換
        List<FavoriteSong> favs = favoriteDao.getAllFavorites();
        ArrayList<String> uriList = new ArrayList<>();
        ArrayList<String> titleList = new ArrayList<>();
        favSongs = new ArrayList<>();
        for (FavoriteSong f : favs) {
            favSongs.add(new Song(f.id, f.title, f.artist, f.album, f.uri, f.duration));
            uriList.add(f.uri);
            titleList.add(f.title);
        }

        // RecyclerView にアダプタ設定
        SongAdapter adapter = new SongAdapter(favSongs, this::onSongSelected, favoriteDao);
        binding.recyclerFav.setAdapter(adapter);

        // SeekBar 初期値セット
        seekBarVolume = binding.seekBarVolume;
        seekBarVolume.setProgress((int) (currentVolume * 100));

        // 再生ボタン押下時の処理
        binding.btnPlayFav.setOnClickListener(v -> {
            if (!favSongs.isEmpty()) {
                // プレイリストと開始インデックスを Service へ渡す Intent
                Intent intent = new Intent(requireContext(), MusicService.class)
                        .setAction(MusicService.ACTION_PLAYLIST_PLAY)
                        .putStringArrayListExtra(MusicService.EXTRA_PLAYLIST, uriList)
                        .putStringArrayListExtra(MusicService.EXTRA_PLAYLIST_TITLE, titleList)
                        .putExtra(MusicService.EXTRA_START_INDEX, currentIndex);
                ContextCompat.startForegroundService(requireContext(), intent);

                // 直後に現在音量を Service に通知
                Intent volUpIntent = new Intent(requireContext(), MusicService.class)
                        .setAction(ACTION_VOLUME_UP)
                        .putExtra(EXTRA_VOLUME, currentVolume);
                ContextCompat.startForegroundService(requireContext(), volUpIntent);
            }
        });

        // 停止ボタン押下時の処理
        binding.btnStopFav.setOnClickListener(v -> {
            Intent stop = new Intent(requireContext(), MusicService.class)
                    .setAction(MusicService.ACTION_STOP);
            ContextCompat.startForegroundService(requireContext(), stop);
        });

        return root;
    }

    /**
     * アイテム選択コールバック
     * @param position 選択された曲の位置
     * @param song     選択された Song オブジェクト
     */
    private void onSongSelected(int position, Song song) {
        currentIndex = position;
        binding.tvNowPlayingFav.setText(song.getTitle());
    }

    /**
     * フラグメント開始時: 音量設定・受信レシーバ登録
     */
    @Override
    public void onStart() {
        super.onStart();
        // 保存音量を再設定
        Float saved = settingDao.getValue("volume");
        currentVolume = (saved != null) ? saved : 0.5f;
        seekBarVolume.setProgress((int) (currentVolume * 100));

        // SeekBar の変更リスナー
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 音量計算（0〜1）
                currentVolume = progress / 100f;
                // Service に音量更新を送信
                Intent volUpIntent = new Intent(requireContext(), MusicService.class)
                        .setAction(ACTION_VOLUME_UP)
                        .putExtra(EXTRA_VOLUME, currentVolume);
                ContextCompat.startForegroundService(requireContext(), volUpIntent);
                // 設定を永続化
                settingDao.insert(new Setting("volume", currentVolume));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // BroadcastReceiver 登録: Service → 曲タイトル受信
        nowPlayingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Intent から現在の再生タイトル取得
                String title = intent.getStringExtra(MusicService.EXTRA_NOW_PLAYING);
                if (title != null) {
                    binding.tvNowPlayingFav.setText(title);
                }
            }
        };
        IntentFilter filter = new IntentFilter(MusicService.ACTION_UPDATE_NOW_PLAYING);
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(nowPlayingReceiver, filter);
    }

    /**
     * フラグメント再開時: Service から最新タイトルを求める
     */
    @Override
    public void onResume() {
        super.onResume();
        // Service にタイトル更新をリクエスト
        Intent titleIntent = new Intent(requireContext(), MusicService.class)
                .setAction(ACTION_TITLE);
        ContextCompat.startForegroundService(requireContext(), titleIntent);
    }

    /**
     * フラグメント停止時: MediaPlayer 解放 & レシーバ解除
     */
    @Override
    public void onStop() {
        super.onStop();
        // MediaPlayer があれば停止・解放
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        // BroadcastReceiver を解除
        if (nowPlayingReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(nowPlayingReceiver);
            nowPlayingReceiver = null;
        }
    }

    /**
     * ビュー破棄時に Binding をクリア
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}