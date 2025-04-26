package jp.tukutano.musicapplication.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import jp.tukutano.musicapplication.databinding.FragmentNotificationsBinding;

/**
 * NotificationsFragment
 * - 通知タブ用のフラグメント
 * - ViewModel からの文字列を TextView に表示
 */
public class NotificationsFragment extends Fragment {

    // ViewBinding の参照
    private FragmentNotificationsBinding binding;

    /**
     * フラグメントのビュー生成
     * @param inflater レイアウトインフレータ
     * @param container 親ビューグループ
     * @param savedInstanceState 保存状態
     * @return フラグメントのルートビュー
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // ViewModel の取得
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        // ViewBinding の初期化
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // TextView の取得
        final TextView textView = binding.textNotifications;
        // LiveData を監視して更新
        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        return root;
    }

    /**
     * ビュー破棄時にバインディングをクリアしてリークを防止
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}