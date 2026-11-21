package iq.dablik.shell;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** غلاف يعرض موقع HTML (من assets/www أو من آخر تحديث عن بعد) مع أدوات اختيارية يضبطها wbl_config.json */
public class MainActivity extends Activity {
    private static final int RC_PERM = 77;
    private static final int RC_FILE = 78;

    private WebView web;
    private FrameLayout root;
    private JSONObject cfg = new JSONObject();
    private SharedPreferences sp;
    private ValueCallback<Uri[]> fileCb;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("wbl", 0);
        try (InputStream in = getAssets().open("wbl_config.json")) {
            cfg = new JSONObject(new String(readAll(in), "UTF-8"));
        } catch (Exception ignored) { }

        if (cfg.optBoolean("fullscreen")) getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (cfg.optBoolean("keepAwake")) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        int o = cfg.optInt("orientation", 0);
        if (o == 1) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else if (o == 2) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        String sb = cfg.optString("statusBar", "");
        if (!sb.isEmpty() && Build.VERSION.SDK_INT >= 21) {
            try {
                int c = Color.parseColor(sb);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(c);
                getWindow().setNavigationBarColor(c);
                double lum = 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c);
                if (lum > 186 && Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } catch (Exception ignored) { }
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        final Runnable next = new Runnable() {
            @Override public void run() {
                if (cfg.optBoolean("login") && sp.getString("user", "").isEmpty()) showLogin(); else startWeb();
            }
        };
        if (cfg.optBoolean("splash")) {
            showSplash();
            root.postDelayed(next, cfg.optInt("splashMs", 1200));
        } else next.run();
    }

    private void showSplash() {
        int bgc = Color.BLACK;
        try { String c = cfg.optString("splashColor", ""); if (!c.isEmpty()) bgc = Color.parseColor(c); } catch (Exception ignored) { }
        double lum = 0.299 * Color.red(bgc) + 0.587 * Color.green(bgc) + 0.114 * Color.blue(bgc);
        FrameLayout f = new FrameLayout(this);
        f.setBackgroundColor(bgc);
        TextView t = new TextView(this);
        t.setText(cfg.optString("name", ""));
        t.setTextColor(lum > 186 ? Color.BLACK : Color.WHITE);
        t.setTextSize(34);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        f.addView(t, new FrameLayout.LayoutParams(-1, -1));
        root.removeAllViews();
        root.addView(f, new FrameLayout.LayoutParams(-1, -1));
    }

    private long lastBack = 0;

    // ---------------- الويب ----------------
    private void startWeb() {
        web = new WebView(this);
        root.removeAllViews();
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setGeolocationEnabled(true);
        s.setDatabaseEnabled(true);
        if (cfg.optBoolean("textZoom", true)) s.setTextZoom(100);
        s.setSupportZoom(cfg.optBoolean("zoom", false));
        s.setBuiltInZoomControls(false);
        if (cfg.optBoolean("debug", false)) WebView.setWebContentsDebuggingEnabled(true);
        if (cfg.optBoolean("bridge", true)) web.addJavascriptInterface(new Bridge(), "Dablik");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView v, int code, String desc, String failingUrl) {
                if (cfg.optBoolean("offlinePage", true) && failingUrl != null && failingUrl.startsWith("http")
                        && (code == ERROR_HOST_LOOKUP || code == ERROR_CONNECT || code == ERROR_TIMEOUT || code == ERROR_IO)) {
                    String u = JSONObject.quote(failingUrl.replace("'", "%27"));
                    v.loadDataWithBaseURL(null, "<html dir='rtl'><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                            + "<body style='background:#000;color:#fff;font-family:sans-serif;text-align:center;padding-top:28vh'>"
                            + "<h2>لا يوجد اتصال بالإنترنت</h2><p style='color:#999'>تحقق من الشبكة ثم حاول مجدداً</p>"
                            + "<button style='background:#3e91ff;color:#fff;border:0;border-radius:26px;padding:14px 34px;font-size:17px' onclick='location.href=" + u + "'>إعادة المحاولة</button>"
                            + "</body></html>", "text/html", "UTF-8", null);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.startsWith("http") && cfg.optBoolean("external", false)) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
                    return true;
                }
                if (url.startsWith("http") || url.startsWith("file")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest req) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        List<String> need = new ArrayList<>();
                        for (String r : req.getResources()) {
                            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) need.add(android.Manifest.permission.CAMERA);
                            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) need.add(android.Manifest.permission.RECORD_AUDIO);
                        }
                        askIfMissing(need);
                        req.grant(req.getResources());
                    }
                });
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                List<String> need = new ArrayList<>();
                need.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
                askIfMissing(need);
                cb.invoke(origin, true, false);
            }

            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try { startActivityForResult(p.createIntent(), RC_FILE); }
                catch (Exception e) { fileCb = null; return false; }
                return true;
            }
        });
        web.setDownloadListener(new android.webkit.DownloadListener() {
            @Override
            public void onDownloadStart(String url, String ua, String cd, String mime, long len) {
                try {
                    DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                    r.setMimeType(mime);
                    r.addRequestHeader("User-Agent", ua);
                    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, cd, mime));
                    ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
                    Toast.makeText(MainActivity.this, "بدأ التنزيل", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
                }
            }
        });

        web.loadUrl(localUrl());
        if (cfg.optBoolean("askPerms", true)) askAllPermissions();
        checkUpdate();
    }

    private String localUrl() {
        File remote = new File(getFilesDir(), "www_remote/index.html");
        return remote.exists() ? "file://" + remote.getAbsolutePath() : "file:///android_asset/www/index.html";
    }

    // ---------------- الصلاحيات ----------------
    private void askIfMissing(List<String> perms) {
        if (Build.VERSION.SDK_INT < 23) return;
        List<String> need = new ArrayList<>();
        for (String p : perms) if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) need.add(p);
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), RC_PERM);
    }

    /** يطلب كل الصلاحيات الخطرة المعلنة في المانيفست (الموقع في الخلفية يُستثنى لأنه يُطلب منفصلاً) */
    private void askAllPermissions() {
        if (Build.VERSION.SDK_INT < 23) return;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            List<String> need = new ArrayList<>();
            if (pi.requestedPermissions != null) {
                for (String p : pi.requestedPermissions) {
                    if (p.equals("android.permission.ACCESS_BACKGROUND_LOCATION")) continue;
                    try {
                        PermissionInfo info = getPackageManager().getPermissionInfo(p, 0);
                        int base = info.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
                        if (base == PermissionInfo.PROTECTION_DANGEROUS && checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) need.add(p);
                    } catch (Exception ignored) { }
                }
            }
            if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), RC_PERM);
        } catch (Exception ignored) { }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == RC_FILE && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCb = null;
        }
    }

    // ---------------- التحديث عن بعد ----------------
    private HttpURLConnection open(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "DablikApp");
        return c;
    }

    private void checkUpdate() {
        final String url = cfg.optString("updateUrl", "");
        if (!cfg.optBoolean("update") || url.isEmpty()) return;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    boolean applied = false;
                    HttpURLConnection c = open(url);
                    if (url.toLowerCase().endsWith(".zip")) {
                        String tag = c.getHeaderField("ETag");
                        if (tag == null) tag = c.getHeaderField("Last-Modified");
                        if (tag == null || !tag.equals(sp.getString("upd_tag", ""))) {
                            applyZip(c.getInputStream());
                            sp.edit().putString("upd_tag", tag == null ? "" : tag).apply();
                            applied = true;
                        }
                    } else {
                        JSONObject j = new JSONObject(new String(readAll(c.getInputStream()), "UTF-8"));
                        int v = j.optInt("version", 0);
                        String zip = j.optString("zip", "");
                        if (v > sp.getInt("upd_ver", 0) && !zip.isEmpty()) {
                            applyZip(open(zip).getInputStream());
                            sp.edit().putInt("upd_ver", v).apply();
                            applied = true;
                        }
                    }
                    if (applied) runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(MainActivity.this, "تم تحديث المحتوى", Toast.LENGTH_SHORT).show();
                            if (web != null) web.loadUrl(localUrl());
                        }
                    });
                } catch (Exception ignored) { }
            }
        }).start();
    }

    private void applyZip(InputStream in) throws Exception {
        File tmp = new File(getFilesDir(), "www_tmp");
        deleteRec(tmp);
        tmp.mkdirs();
        String tmpPath = tmp.getCanonicalPath() + File.separator;
        List<String> names = new ArrayList<>();
        ZipInputStream zi = new ZipInputStream(in);
        ZipEntry e;
        while ((e = zi.getNextEntry()) != null) {
            String n = e.getName().replace('\\', '/');
            if (e.isDirectory() || n.contains("..") || n.startsWith("__MACOSX/")) continue;
            File f = new File(tmp, n);
            if (!f.getCanonicalPath().startsWith(tmpPath)) continue;
            f.getParentFile().mkdirs();
            FileOutputStream o = new FileOutputStream(f);
            byte[] buf = new byte[8192];
            int r;
            while ((r = zi.read(buf)) > 0) o.write(buf, 0, r);
            o.close();
            names.add(n);
        }
        zi.close();
        String best = null;
        for (String n : names) {
            if (n.toLowerCase().endsWith("index.html") && (n.equalsIgnoreCase("index.html") || n.toLowerCase().endsWith("/index.html"))) {
                if (best == null || count(n, '/') < count(best, '/')) best = n;
            }
        }
        if (best == null) throw new Exception("no index.html");
        String prefix = best.contains("/") ? best.substring(0, best.lastIndexOf('/')) : "";
        File src = prefix.isEmpty() ? tmp : new File(tmp, prefix);
        File dst = new File(getFilesDir(), "www_remote");
        deleteRec(dst);
        if (!src.renameTo(dst)) throw new Exception("rename failed");
        deleteRec(tmp);
    }

    private static int count(String s, char c) { int k = 0; for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) k++; return k; }

    private static void deleteRec(File f) {
        if (!f.exists()) return;
        File[] ch = f.listFiles();
        if (ch != null) for (File c : ch) deleteRec(c);
        f.delete();
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) > 0) bo.write(buf, 0, r);
        return bo.toByteArray();
    }

    // ---------------- تسجيل الدخول (حسابات محلية على الجهاز) ----------------
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private GradientDrawable bg(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private String hash(String pass, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pass.toCharArray(), salt, 30000, 256);
        return Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded(), Base64.NO_WRAP);
    }

    private void showLogin() {
        final boolean[] signup = {false};
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(dp(28), dp(28), dp(28), dp(28));
        l.setBackgroundColor(0xFF000000);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText(cfg.optString("name", "تسجيل الدخول"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        l.addView(title);

        final TextView sub = new TextView(this);
        sub.setTextColor(0xFFA0A0A5);
        sub.setTextSize(15);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, dp(26));
        l.addView(sub);

        final EditText u = new EditText(this);
        u.setHint("اسم المستخدم");
        final EditText p = new EditText(this);
        p.setHint("كلمة المرور");
        p.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        for (EditText t : new EditText[]{u, p}) {
            t.setTextColor(Color.WHITE);
            t.setHintTextColor(0xFF6E6E73);
            t.setBackground(bg(0xFF1C1C1E, 22));
            t.setPadding(dp(20), dp(16), dp(20), dp(16));
            t.setSingleLine(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(12);
            l.addView(t, lp);
        }

        final TextView err = new TextView(this);
        err.setTextColor(0xFFFF5A5A);
        err.setTextSize(14);
        l.addView(err);

        final Button go = new Button(this);
        go.setTextColor(Color.WHITE);
        go.setTextSize(17);
        go.setBackground(bg(0xFF3E91FF, 28));
        go.setAllCaps(false);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, dp(56));
        glp.topMargin = dp(8);
        l.addView(go, glp);

        final TextView sw = new TextView(this);
        sw.setTextColor(0xFF3E91FF);
        sw.setTextSize(15);
        sw.setGravity(Gravity.CENTER);
        sw.setPadding(0, dp(18), 0, 0);
        l.addView(sw);

        final Runnable refresh = new Runnable() {
            @Override public void run() {
                sub.setText(signup[0] ? "أنشئ حسابك للمتابعة" : "سجّل الدخول للمتابعة");
                go.setText(signup[0] ? "إنشاء حساب" : "دخول");
                sw.setText(signup[0] ? "لدي حساب، تسجيل الدخول" : "ليس لدي حساب، إنشاء حساب");
                err.setText("");
            }
        };
        refresh.run();
        sw.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { signup[0] = !signup[0]; refresh.run(); }
        });
        go.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String name = u.getText().toString().trim().toLowerCase();
                String pass = p.getText().toString();
                try {
                    if (name.length() < 3) { err.setText("اسم المستخدم 3 أحرف على الأقل"); return; }
                    if (pass.length() < 6) { err.setText("كلمة المرور 6 أحرف على الأقل"); return; }
                    String key = "acc_" + name;
                    if (signup[0]) {
                        if (sp.contains(key)) { err.setText("اسم المستخدم مستخدم"); return; }
                        byte[] salt = new byte[16];
                        new SecureRandom().nextBytes(salt);
                        sp.edit().putString(key, Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + hash(pass, salt)).apply();
                    } else {
                        String rec = sp.getString(key, null);
                        if (rec == null) { err.setText("الحساب غير موجود"); return; }
                        String[] parts = rec.split(":");
                        if (!hash(pass, Base64.decode(parts[0], Base64.NO_WRAP)).equals(parts[1])) { err.setText("كلمة المرور غير صحيحة"); return; }
                    }
                    sp.edit().putString("user", name).apply();
                    startWeb();
                } catch (Exception e) { err.setText("تعذر إكمال العملية"); }
            }
        });
        root.removeAllViews();
        root.addView(l, new FrameLayout.LayoutParams(-1, -1));
    }

    // ---------------- أدوات يستدعيها موقعك: Dablik.toast("..") ----------------
    private class Bridge {
        @JavascriptInterface public void toast(final String m) { runOnUiThread(new Runnable() { @Override public void run() { Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show(); } }); }
        @JavascriptInterface public void share(String t) {
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, t);
            startActivity(Intent.createChooser(i, "مشاركة").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        @JavascriptInterface public void copy(final String t) {
            runOnUiThread(new Runnable() { @Override public void run() {
                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("text", t));
                Toast.makeText(MainActivity.this, "تم النسخ", Toast.LENGTH_SHORT).show();
            } });
        }
        @JavascriptInterface public void vibrate(int ms) { try { ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(ms); } catch (Exception ignored) { } }
        @JavascriptInterface public void openUrl(String u) { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) { } }
        @JavascriptInterface public void notify(String title, String text) {
            try {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification.Builder nb;
                if (Build.VERSION.SDK_INT >= 26) {
                    nm.createNotificationChannel(new NotificationChannel("wbl", "الإشعارات", NotificationManager.IMPORTANCE_DEFAULT));
                    nb = new Notification.Builder(MainActivity.this, "wbl");
                } else nb = new Notification.Builder(MainActivity.this);
                PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, new Intent(MainActivity.this, MainActivity.class), Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
                nb.setContentTitle(title).setContentText(text).setSmallIcon(getApplicationInfo().icon).setAutoCancel(true).setContentIntent(pi);
                nm.notify((int) (System.currentTimeMillis() % 100000), nb.build());
            } catch (Exception ignored) { }
        }
        @JavascriptInterface public String info() {
            try {
                JSONObject j = new JSONObject();
                j.put("package", getPackageName());
                j.put("android", Build.VERSION.SDK_INT);
                j.put("model", Build.MODEL);
                j.put("brand", Build.MANUFACTURER);
                j.put("version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
                return j.toString();
            } catch (Exception e) { return "{}"; }
        }
        @JavascriptInterface public String user() { return sp.getString("user", ""); }
        @JavascriptInterface public void logout() {
            sp.edit().remove("user").apply();
            runOnUiThread(new Runnable() { @Override public void run() { if (cfg.optBoolean("login")) showLogin(); } });
        }
        @JavascriptInterface public void openSettings() {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        @JavascriptInterface public void exit() { runOnUiThread(new Runnable() { @Override public void run() { finish(); } }); }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) { web.goBack(); return; }
        if (cfg.optBoolean("doubleBack", false) && System.currentTimeMillis() - lastBack > 2000) {
            lastBack = System.currentTimeMillis();
            Toast.makeText(this, "اضغط مرة أخرى للخروج", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    @Override protected void onPause() { super.onPause(); if (web != null) web.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
}
