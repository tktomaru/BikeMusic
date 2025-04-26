package jp.tukutano.musicapplication.ui.home;

import static jp.tukutano.musicapplication.service.MusicService.ACTION_TITLE;
import static jp.tukutano.musicapplication.service.MusicService.ACTION_VOLUME_UP;
import static jp.tukutano.musicapplication.service.MusicService.EXTRA_VOLUME;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

import java.io.IOException;
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

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AppDatabase db;
    private FavoriteDao favoriteDao;
    private SettingDao settingDao;
    private MediaPlayer mediaPlayer;
    private List<Song> favSongs;
    private int currentIndex = 0;
    private SeekBar seekBarVolume;
    private Float currentVolume = 0.5f; // 0.0〜1.0 の範囲
    private BroadcastReceiver nowPlayingReceiver;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = Room.databaseBuilder(
                        getContext().getApplicationContext(),
                        AppDatabase.class, "music_app_db")
                .allowMainThreadQueries()
                .build();
        favoriteDao = db.favoriteDao();
        settingDao = db.settingDao();

        // 保存値があれば読み込み、なければデフォルト1.0f
        Float saved = settingDao.getValue("volume");
        currentVolume = (saved != null) ? saved : 0.5f;

        // RecyclerView
        binding.recyclerFav.setLayoutManager(new LinearLayoutManager(getContext()));

        // FavoriteSong → Song へのマッピング
        List<FavoriteSong> favs = favoriteDao.getAllFavorites();
        ArrayList<String> uriList = new ArrayList<>();
        ArrayList<String> titleList = new ArrayList<>();
        favSongs = new ArrayList<>();
        for (FavoriteSong f : favs) {
            favSongs.add(new Song(f.id, f.title, f.artist, f.album, f.uri, f.duration));
            uriList.add(f.uri);
            titleList.add(f.title);
        }


        // Adapter
        SongAdapter adapter = new SongAdapter(favSongs, this::onSongSelected, favoriteDao);
        binding.recyclerFav.setAdapter(adapter);

        // 3. MediaPlayer 初期化
        mediaPlayer = new MediaPlayer();
        seekBarVolume = binding.seekBarVolume;
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), currentVolume.toString());
        seekBarVolume.setProgress((int) (currentVolume * 100));
        mediaPlayer.setOnCompletionListener(mp -> playNext());

        // 4. 再生・停止ボタン
        binding.btnPlayFav.setOnClickListener(v -> {
            if (!favSongs.isEmpty()) {
                // Serviceにお気に入りのリストを渡す
                Intent intent = new Intent(requireContext(), MusicService.class)
                        .setAction(MusicService.ACTION_PLAYLIST_PLAY)
                        .putStringArrayListExtra(MusicService.EXTRA_PLAYLIST, uriList)
                        .putStringArrayListExtra(MusicService.EXTRA_PLAYLIST_TITLE, titleList)
                        .putExtra(MusicService.EXTRA_START_INDEX, currentIndex);
                ContextCompat.startForegroundService(requireContext(), intent);


                Intent volUpIntent = new Intent(getContext(), MusicService.class)
                        .setAction(ACTION_VOLUME_UP)
                        .putExtra(EXTRA_VOLUME, currentVolume);
                requireContext().startService(volUpIntent);
            }
        });
        binding.btnStopFav.setOnClickListener(v -> {
            Intent stop = new Intent(requireContext(), MusicService.class)
                    .setAction(MusicService.ACTION_STOP);
            requireContext().startService(stop);
            binding.tvNowPlayingFav.setText("—");
            mediaPlayer.reset();
        });
        return root;
    }

    private void onSongSelected(Song song) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(getContext(), Uri.parse(song.getUri()));
            mediaPlayer.prepare();
            binding.tvNowPlayingFav.setText(song.getTitle());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定インデックスの曲を再生
     */
    private void playAtIndex(int index) {
        if (index < 0 || index >= favSongs.size()) return;
        currentIndex = index;
        Song song = favSongs.get(index);
        binding.tvNowPlayingFav.setText(song.getTitle());
    }

    /**
     * 次の曲を再生（ループ）
     */
    private void playNext() {
        currentIndex = (currentIndex + 1) % favSongs.size();
        playAtIndex(currentIndex);
    }

    @Override
    public void onStart() {
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
        super.onStart();
        // 保存値があれば読み込み、なければデフォルト
        Float saved = settingDao.getValue("volume");
        currentVolume = (saved != null) ? saved : 0.5f;
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), currentVolume.toString());
        seekBarVolume.setProgress((int) (currentVolume * 100));

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentVolume = progress / 100f;
                // 音量↑用 Intent に currentVolume を添付
                Intent volUpIntent = new Intent(getContext(), MusicService.class)
                        .setAction(ACTION_VOLUME_UP)
                        .putExtra(EXTRA_VOLUME, currentVolume);
                requireContext().startService(volUpIntent);

                LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), currentVolume.toString());
                Setting setting = new Setting("volume", currentVolume);
                settingDao.insert(setting);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        // レシーバーを作成
        nowPlayingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String title = intent.getStringExtra(MusicService.EXTRA_NOW_PLAYING);
                if (title != null) {
                    Log.println(Log.DEBUG, "tktomaru", title);
                    binding.tvNowPlayingFav.setText(title);
                }
            }
        };
        IntentFilter filter = new IntentFilter(MusicService.ACTION_UPDATE_NOW_PLAYING);
        // インテントフィルタを作成して登録
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(nowPlayingReceiver, filter);
    }

    @Override
    public void onResume() {
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
        super.onResume();
        // 保存値があれば読み込み、なければデフォルト
        Float saved = settingDao.getValue("volume");
        currentVolume = (saved != null) ? saved : 0.5f;
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), currentVolume.toString());
        seekBarVolume.setProgress((int) (currentVolume * 100));

        // Title配信を依頼
        Intent titleIntent = new Intent(getContext(), MusicService.class)
                .setAction(ACTION_TITLE);
        requireContext().startService(titleIntent);
    }

    @Override
    public void onStop() {
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
        super.onStop();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        // レシーバー解除
        if (nowPlayingReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(nowPlayingReceiver);
            nowPlayingReceiver = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}