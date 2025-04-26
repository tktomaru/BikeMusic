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

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    /**
     * 曲リスト
     */
    private List<Song> songList;
    /**
     * 曲タップ時のコールバック
     */
    private final OnSongClickListener listener;

    private final FavoriteDao favoriteDao;

    /**
     * インターフェース：曲選択コールバック
     */
    public interface OnSongClickListener {
        void onSongClick(int position, Song song);
    }

    public SongAdapter(List<Song> songList, OnSongClickListener listener, FavoriteDao dao) {
        this.songList = songList;
        this.listener = listener;
        this.favoriteDao = dao;
    }

    /**
     * リスト更新用メソッド
     */
    public void updateList(List<Song> filtered) {
        this.songList = filtered;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        // お気に入り状態の反映
        boolean isFav = favoriteDao.isFavorite(song.getId());
        holder.btnFav.setImageResource(
                isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_fill
        );

        // タップ時再生
        holder.itemView.setOnClickListener(v -> listener.onSongClick(position, song));

        // お気に入りボタン
        holder.btnFav.setOnClickListener(v -> {
            if (favoriteDao.isFavorite(song.getId())) {
                favoriteDao.delete(toFavoriteSong(song));
                holder.btnFav.setImageResource(R.drawable.ic_favorite);
            } else {
                favoriteDao.insert(toFavoriteSong(song));
                holder.btnFav.setImageResource(R.drawable.ic_favorite_fill);
            }
        });
    }

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

    @Override
    public int getItemCount() {
        return songList.size();
    }

    /**
     * ビューホルダー
     */
    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvArtist;
        ImageButton btnFav;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            btnFav = itemView.findViewById(R.id.btnFavorite);
        }
    }
}
