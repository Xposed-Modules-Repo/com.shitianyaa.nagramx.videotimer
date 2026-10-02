package com.shitianyaa.nagramx.videotimer;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 模块状态展示页面。
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        status.setPadding(padding, padding, padding, padding);
        status.setTextIsSelectable(true);
        status.setTextSize(16f);

        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.CENTER);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(status);
        setContentView(layout);

        status.setText("NagramX Video Timer\n\n"
                + "请在 LSPosed 中启用模块并设置作用域\n\n"
                + targetStatus());
    }

    private String targetStatus() {
        PackageInfo packageInfo = getTargetPackageInfo();
        if (packageInfo == null) {
            return "未检测到 " + HostProfile.TARGET_PACKAGE;
        }
        long versionCode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            versionCode = packageInfo.getLongVersionCode();
        } else {
            versionCode = packageInfo.versionCode;
        }
        String versionName = packageInfo.versionName != null ? packageInfo.versionName : "unknown";
        return String.format(
                "目标：%s %s (%d)\n已验证宿主：12.9.2-4335a2e (1260)；其他版本需测试",
                HostProfile.TARGET_PACKAGE,
                versionName,
                versionCode
        );
    }

    private PackageInfo getTargetPackageInfo() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return getPackageManager().getPackageInfo(
                        HostProfile.TARGET_PACKAGE,
                        PackageManager.PackageInfoFlags.of(0)
                );
            } else {
                return getPackageManager().getPackageInfo(HostProfile.TARGET_PACKAGE, 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
