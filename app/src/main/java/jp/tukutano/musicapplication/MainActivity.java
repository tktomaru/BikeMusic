package jp.tukutano.musicapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import jp.tukutano.musicapplication.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // ViewBinding 用バインディングオブジェクト
    private ActivityMainBinding binding;
    // パーミッション要求時のリクエストコード
    private static final int REQ_PERM = 1001;

    /**
     * ストレージおよび通知パーミッションをリクエストする
     */
    private void requestStoragePermission() {
        // Android 13 (T) 以降はメディア別パーミッション
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String perm = Manifest.permission.READ_MEDIA_AUDIO;
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                // オーディオ読み込み権限が未許可なら要求
                requestPermissions(new String[]{perm}, REQ_PERM);
            }
        } else {
            // Android 12 以下は外部ストレージ読み取り権限
            String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{perm}, REQ_PERM);
            }
        }
        // Android 13 以上は通知パーミッションも必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_PERM
                );
            }
        }
    }

    /**
     * パーミッションダイアログの結果コールバック
     * @param requestCode リクエストコード
     * @param perms       要求したパーミッション配列
     * @param grantResults 許可結果配列
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] perms, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, perms, grantResults);
        if (requestCode == REQ_PERM && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 権限が許可された場合の処理（例: 曲一覧のロード）
            // loadSongs();
        } else {
            // 権限拒否時はトーストで通知
            Toast.makeText(this, "権限が必要です", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Activity 初回作成時の処理
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ストレージ・通知パーミッションをリクエスト
        requestStoragePermission();

        // ViewBinding の初期化
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // BottomNavigationView の取得
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // 各メニューをトップレベルとして設定
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(
                        R.id.navigation_home,
                        R.id.navigation_dashboard,
                        R.id.navigation_notifications)
                        .build();
        // NavController と連携
        NavController navController = Navigation.findNavController(
                this, R.id.nav_host_fragment_activity_main);
        // BottomNavigationView と NavController を紐づけ
        NavigationUI.setupWithNavController(binding.navView, navController);

        // サポートアクションバーのタイトル非表示
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

}
