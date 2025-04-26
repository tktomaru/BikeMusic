package jp.tukutano.musicapplication.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import jp.tukutano.musicapplication.R;
import jp.tukutano.musicapplication.db.FavoriteDao;
import jp.tukutano.musicapplication.db.FavoriteSong;
import jp.tukutano.musicapplication.model.Song;

/***
 - SongAdapter
 - RecyclerView に楽曲リストを表示し、
 - タップで再生、ハートボタンでお気に入り登録・解除を行う \*/
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    /**
     * 表示する楽曲のリスト
     */
    private List<Song> songList;
    /**
     * 楽曲タップ時のコールバック
     */
    private final OnSongClickListener listener;
    /**
     * お気に入り操作用 DAO
     */
    private final FavoriteDao favoriteDao;

    /**
     * インターフェース：楽曲選択時のコールバック
     */
    public interface OnSongClickListener {
        /**
         * @param position タップされた楽曲の位置
         * @param song     タップされた楽曲オブジェクト
         */
        void onSongClick(int position, Song song);
    }

    /**
     * コンストラクタ
     *
     * @param songList 楽曲リスト
     * @param listener 楽曲タップ時コールバック
     * @param dao      お気に入り操作用 DAO
     */
    public SongAdapter(List<Song> songList, OnSongClickListener listener, FavoriteDao dao) {
        this.songList = songList;
        this.listener = listener;
        this.favoriteDao = dao;
    }

    /**
     * フィルタリング後などでリストを更新する
     *
     * @param filtered 更新後の楽曲リスト
     */
    public void updateList(List<Song> filtered) {
        this.songList = filtered;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // item_song.xml を inflate
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        // タイトルとアーティスト名をセット
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        // お気に入り状態をアイコンで表示
        boolean isFav = favoriteDao.isFavorite(song.getId());
        holder.btnFav.setImageResource(
                isFav ? R.drawable.ic_favorite_fill : R.drawable.ic_favorite
        );

        // アイテムタップで再生コールバック
        holder.itemView.setOnClickListener(v -> listener.onSongClick(position, song));

        // お気に入りボタン押下で登録/解除
        holder.btnFav.setOnClickListener(v -> {
            if (favoriteDao.isFavorite(song.getId())) {
                // 既にお気に入りなら削除
                favoriteDao.delete(toFavoriteSong(song));
                holder.btnFav.setImageResource(R.drawable.ic_favorite);
            } else {
                // お気に入りに追加
                favoriteDao.insert(toFavoriteSong(song));
                holder.btnFav.setImageResource(R.drawable.ic_favorite_fill);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    /**
     * Song モデルから FavoriteSong Entity に変換
     *
     * @param s Song オブジェクト
     * @return FavoriteSong オブジェクト
     */
    private FavoriteSong toFavoriteSong(Song s) {
        FavoriteSong f = new FavoriteSong();
        f.id = s.getId();
        f.title = s.getTitle();
        f.artist = s.getArtist();
        f.album = s.getAlbum();
        f.uri = s.getUri();
        f.duration = s.getDuration();
        return f;
    }

    /**
     * ViewHolder：各アイテムのビューを保持
     */
    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvArtist;
        ImageButton btnFav;

        /**
         * コンストラクタ：ビューIDと紐づけ
         */
        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            btnFav = itemView.findViewById(R.id.btnFavorite);
        }
    }
}

