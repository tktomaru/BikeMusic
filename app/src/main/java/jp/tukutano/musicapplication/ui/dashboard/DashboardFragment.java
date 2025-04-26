package jp.tukutano.musicapplication.ui.dashboard;

import static jp.tukutano.musicapplication.service.MusicService.ACTION_TITLE;
import static jp.tukutano.musicapplication.service.MusicService.ACTION_VOLUME_UP;
import static jp.tukutano.musicapplication.service.MusicService.EXTRA_VOLUME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.net.Uri;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jp.tukutano.musicapplication.databinding.FragmentDashboardBinding;
import jp.tukutano.musicapplication.db.FavoriteDao;
import jp.tukutano.musicapplication.db.Setting;
import jp.tukutano.musicapplication.db.SettingDao;
import jp.tukutano.musicapplication.model.Song;
import jp.tukutano.musicapplication.AppDatabase;
import jp.tukutano.musicapplication.service.MusicService;
import jp.tukutano.musicapplication.util.LogUtils;
import jp.tukutano.musicapplication.util.MusicUtils;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private MediaPlayer mediaPlayer;
    private Song selectedSong;
    private SeekBar seekBarVolume;
    private Float currentVolume = 0.5f; // 0.0〜1.0 の範囲

    private List<Song> songList;         // 全曲リスト
    private List<Song> filteredList;     // フィルタ後リスト
    private SongAdapter adapter;         // フィールド化

    private Spinner spinnerArtist;
    private List<String> artistNames;     // 全アーティスト名（重複なし）
    private ArrayAdapter<String> spinnerAdapter;

    private AppDatabase db;
    private FavoriteDao favoriteDao;
    private SettingDao settingDao;
    private BroadcastReceiver nowPlayingReceiver;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        // DB 初期化
        db = Room.databaseBuilder(
                        getContext().getApplicationContext(),
                        AppDatabase.class, "music_app_db")
                .allowMainThreadQueries()  // 簡易実装。大量データならバックグラウンド推奨
                .build();
        favoriteDao = db.favoriteDao();
        settingDao = db.settingDao();

        // 保存値があれば読み込み、なければデフォルト1.0f
        Float saved = settingDao.getValue("volume");
        currentVolume = (saved != null) ? saved : 0.5f;


        // MediaPlayer 初期化
        mediaPlayer = new MediaPlayer();

        seekBarVolume = binding.seekBarVolume;
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), currentVolume.toString());
        seekBarVolume.setProgress((int) (currentVolume * 100));

        // 曲一覧ロード（MusicUtils は自作ユーティリティ想定）
        songList = MusicUtils.loadAllAudio(getContext());
        filteredList = new ArrayList<>(songList);


        // SearchView リスナー設定
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                applyFilters(query, (String) spinnerArtist.getSelectedItem());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilters(newText, (String) spinnerArtist.getSelectedItem());
                return true;
            }
        });

        // RecyclerView セットアップ
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SongAdapter(filteredList, this::onSongSelected, favoriteDao);
        binding.recyclerView.setAdapter(adapter);

        // 再生ボタン
        binding.btnPlay.setOnClickListener(v -> {
            ArrayList<String> uriList = new ArrayList<>();
            ArrayList<String> titleList = new ArrayList<>();


            if (null != selectedSong) {
                uriList.add(selectedSong.getUri());
                titleList.add(selectedSong.getTitle());
                Intent intent = new Intent(requireContext(), MusicService.class)
                        .setAction(MusicService.ACTION_PLAYLIST_PLAY)
                        .putStringArrayListExtra(MusicService.EXTRA_PLAYLIST, uriList)
                        .putStringArrayListExtra(MusicService.EXTRA_PLAYLIST_TITLE, titleList)
                        .putExtra(MusicService.EXTRA_START_INDEX, 0);
                ContextCompat.startForegroundService(requireContext(), intent);

                Intent volUpIntent = new Intent(getContext(), MusicService.class)
                        .setAction(ACTION_VOLUME_UP)
                        .putExtra(EXTRA_VOLUME, currentVolume);
                requireContext().startService(volUpIntent);
            }
        });

        // 停止ボタン
        binding.btnStop.setOnClickListener(v -> {
            Intent stop = new Intent(requireContext(), MusicService.class)
                    .setAction(MusicService.ACTION_STOP);
            requireContext().startService(stop);
            mediaPlayer.reset();
        });


        // --- Spinner 準備 ---
        spinnerArtist = binding.spinnerArtist;

        // ① 全アーティスト名を重複排除で抽出
        Set<String> set = new TreeSet<>();
        for (Song s : songList) {
            set.add(s.getArtist());
        }
        artistNames = new ArrayList<>();
        artistNames.add("すべて");    // デフォルト（絞り込みなし）
        artistNames.addAll(set);

        // ② ArrayAdapter を作成して Spinner にセット
        spinnerAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                artistNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerArtist.setAdapter(spinnerAdapter);

        // ③ 選択リスナー登録
        spinnerArtist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedArtist = artistNames.get(pos);
                applyFilters(binding.searchView.getQuery().toString(), selectedArtist);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                applyFilters(binding.searchView.getQuery().toString(), "すべて");
            }
        });

        return root;
    }

    private void onSongSelected(Song song) {
        selectedSong = song;
        binding.tvNowPlaying.setText(song.getTitle());
    }

    /**
     * キーワード検索 と アーティスト絞り込み を同時に適用するメソッド
     *
     * @param keyword 曲タイトル or アーティストの部分一致
     * @param artist  絞り込み対象アーティスト名（"すべて" は全件）
     */
    private void applyFilters(String keyword, String artist) {
        String q = keyword.trim().toLowerCase();
        filteredList.clear();
        for (Song s : songList) {
            boolean matchesKeyword = q.isEmpty()
                    || s.getTitle().toLowerCase().contains(q)
                    || s.getArtist().toLowerCase().contains(q);
            boolean matchesArtist = artist.equals("すべて")
                    || s.getArtist().equals(artist);
            if (matchesKeyword && matchesArtist) {
                filteredList.add(s);
            }
        }
        adapter.updateList(filteredList);
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
        // レシーバーを作成
        nowPlayingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String title = intent.getStringExtra(MusicService.EXTRA_NOW_PLAYING);
                if (title != null) {
                    Log.println(Log.DEBUG, "tktomaru", title);
                    binding.tvNowPlaying.setText(title);
                }
            }
        };
        IntentFilter filter = new IntentFilter(MusicService.ACTION_UPDATE_NOW_PLAYING);
        // インテントフィルタを作成して登録
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(nowPlayingReceiver, filter);


        //シークバー
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentVolume = progress / 100f;
                Intent volUpIntent = new Intent(getContext(), MusicService.class)
                        .setAction(ACTION_VOLUME_UP)
                        .putExtra(EXTRA_VOLUME, currentVolume);
                requireContext().startService(volUpIntent);

                // 設定値保存
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