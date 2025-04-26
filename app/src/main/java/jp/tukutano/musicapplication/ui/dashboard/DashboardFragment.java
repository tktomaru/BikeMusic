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

/**
 * ダッシュボード画面
 * - 全曲リスト表示
 * - 検索・アーティスト絞り込み
 * - 再生・停止・音量調整
 * - お気に入り登録
 */
public class DashboardFragment extends Fragment {

    // ViewBinding オブジェクト
    private FragmentDashboardBinding binding;
    // バックグラウンド再生用 MediaPlayer
    private MediaPlayer mediaPlayer;
    // 選択中の楽曲
    private Song selectedSong;
    // 音量調整用 SeekBar
    private SeekBar seekBarVolume;
    // 現在のアプリ内再生音量 (0.0〜1.0)
    private Float currentVolume = 0.5f;

    // 全楽曲リスト
    private List<Song> songList;
    // フィルタ後リスト
    private List<Song> filteredList;
    // RecyclerView 用アダプタ
    private SongAdapter adapter;

    // アーティスト絞り込み用 Spinner
    private Spinner spinnerArtist;
    // アーティスト名一覧（重複なし）
    private List<String> artistNames;
    private ArrayAdapter<String> spinnerAdapter;

    // Room DB
    private AppDatabase db;
    private FavoriteDao favoriteDao;
    private SettingDao settingDao;
    // 再生中曲情報受信用レシーバー
    private BroadcastReceiver nowPlayingReceiver;

    /**
     * フラグメントのビュー生成処理
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // デバッグ用ログ
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "Dashboard onCreateView");
        // ViewModel 初期化（不要なら削除可）
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);
        // ViewBinding のセット
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // --- Room DB 初期化 ---
        db = Room.databaseBuilder(
                        requireContext().getApplicationContext(),
                        AppDatabase.class, "music_app_db")
                .allowMainThreadQueries()  // デモ用: 本番は非同期推奨
                .build();
        favoriteDao = db.favoriteDao();
        settingDao = db.settingDao();

        // 保存音量読み込み or デフォルト
        Float saved = settingDao.getValue("volume");
        currentVolume = (saved != null) ? saved : 0.5f;

        // MediaPlayer 初期化
        mediaPlayer = new MediaPlayer();
        seekBarVolume = binding.seekBarVolume;
        // SeekBar に初期音量をセット
        seekBarVolume.setProgress((int)(currentVolume * 100));

        // --- 曲リスト読み込み ---
        songList = MusicUtils.loadAllAudio(requireContext());
        filteredList = new ArrayList<>(songList);

        // --- 検索バー設定 ---
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // 検索実行
                applyFilters(query, (String) spinnerArtist.getSelectedItem());
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                // 入力ごとにフィルタ更新
                applyFilters(newText, (String) spinnerArtist.getSelectedItem());
                return true;
            }
        });

        // --- RecyclerView 設定 ---
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SongAdapter(filteredList, this::onSongSelected, favoriteDao);
        binding.recyclerView.setAdapter(adapter);

        // --- 再生/停止 ボタン処理 ---
        binding.btnPlay.setOnClickListener(v -> playSelectedSong());
        binding.btnStop.setOnClickListener(v -> stopPlayback());

        // --- アーティスト Spinner 設定 ---
        spinnerArtist = binding.spinnerArtist;
        // 重複排除してアーティスト名を抽出
        Set<String> set = new TreeSet<>();
        for (Song s : songList) set.add(s.getArtist());
        artistNames = new ArrayList<>();
        artistNames.add("すべて");
        artistNames.addAll(set);
        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                artistNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerArtist.setAdapter(spinnerAdapter);
        spinnerArtist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // 選択アーティストで絞り込み
                applyFilters(binding.searchView.getQuery().toString(), artistNames.get(pos));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                // デフォルト表示
                applyFilters(binding.searchView.getQuery().toString(), "すべて");
            }
        });

        return root;
    }

    /**
     * 選択された楽曲を再生リクエスト
     */
    private void playSelectedSong() {
        if (selectedSong == null) return;
        // Intent に URI とタイトルをセットし Service 起動
        ArrayList<String> uriList = new ArrayList<>();
        ArrayList<String> titleList = new ArrayList<>();
        uriList.add(selectedSong.getUri());
        titleList.add(selectedSong.getTitle());
        Intent intent = new Intent(requireContext(), MusicService.class)
                .setAction(MusicService.ACTION_PLAYLIST_PLAY)
                .putStringArrayListExtra(MusicService.EXTRA_PLAYLIST, uriList)
                .putStringArrayListExtra(MusicService.EXTRA_PLAYLIST_TITLE, titleList)
                .putExtra(MusicService.EXTRA_START_INDEX, 0);
        ContextCompat.startForegroundService(requireContext(), intent);
        // 初期音量を Service に通知
        Intent volUp = new Intent(requireContext(), MusicService.class)
                .setAction(ACTION_VOLUME_UP)
                .putExtra(EXTRA_VOLUME, currentVolume);
        ContextCompat.startForegroundService(requireContext(), volUp);
    }

    /**
     * 再生中の楽曲を停止
     */
    private void stopPlayback() {
        Intent stop = new Intent(requireContext(), MusicService.class)
                .setAction(MusicService.ACTION_STOP);
        ContextCompat.startForegroundService(requireContext(), stop);
    }

    /**
     * 項目タップ時コールバック
     */
    private void onSongSelected(int position, Song song) {
        selectedSong = song;
        binding.tvNowPlaying.setText(song.getTitle());
    }

    /**
     * 検索キーワードとアーティスト絞り込みを同時に適用
     */
    private void applyFilters(String keyword, String artist) {
        String q = keyword.trim().toLowerCase();
        filteredList.clear();
        for (Song s : songList) {
            boolean matchKey = q.isEmpty() || s.getTitle().toLowerCase().contains(q) || s.getArtist().toLowerCase().contains(q);
            boolean matchArtist = "すべて".equals(artist) || s.getArtist().equals(artist);
            if (matchKey && matchArtist) filteredList.add(s);
        }
        adapter.updateList(filteredList);
    }

    /**
     * フラグメント開始時: レシーバ登録 & SeekBar リスナー
     */
    @Override
    public void onStart() {
        super.onStart();
        // 音量設定の再読み込み
        Float saved = settingDao.getValue("volume");
        currentVolume = (saved != null) ? saved : 0.5f;
        seekBarVolume.setProgress((int)(currentVolume * 100));
        // BroadcastReceiver 登録: Service から現在曲タイトルを受信
        nowPlayingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String title = intent.getStringExtra(MusicService.EXTRA_NOW_PLAYING);
                if (title != null) binding.tvNowPlaying.setText(title);
            }
        };
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(nowPlayingReceiver, new IntentFilter(MusicService.ACTION_UPDATE_NOW_PLAYING));
        // SeekBar の音量変更リスナー
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentVolume = progress / 100f;
                Intent volIntent = new Intent(requireContext(), MusicService.class)
                        .setAction(ACTION_VOLUME_UP)
                        .putExtra(EXTRA_VOLUME, currentVolume);
                ContextCompat.startForegroundService(requireContext(), volIntent);
                // Room に音量保存
                settingDao.insert(new Setting("volume", currentVolume));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * フラグメント再開時: Service にタイトル更新要求
     */
    @Override
    public void onResume() {
        super.onResume();
        Intent intent = new Intent(requireContext(), MusicService.class)
                .setAction(ACTION_TITLE);
        ContextCompat.startForegroundService(requireContext(), intent);
    }

    /**
     * フラグメント停止時: リソース解放 & レシーバ解除
     */
    @Override
    public void onStop() {
        super.onStop();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (nowPlayingReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(nowPlayingReceiver);
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