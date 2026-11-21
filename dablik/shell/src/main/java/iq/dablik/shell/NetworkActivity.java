package iq.dablik.shell;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * يكتشف شبكة الواي فاي الحالية (اسم الشبكة، عنوان الراوتر، نوعه المحتمل) ثم يفتح
 * لوحة إدارة الراوتر الحقيقية (صفحته هو، لا شيء مزيف) ليسجّل المستخدم دخوله بمعلوماته الفعلية
 * ويدير شبكته من واجهة الراوتر نفسها (اسم الشبكة، كلمة السر، الأجهزة المسموح لها).
 */
public class NetworkActivity extends Activity {
    private static final String PREF = "wbl_net";

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private static String ipToString(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

    /** تخمين الشركة المصنّعة من أول نصف عنوان MAC (OUI). ليس دقيقاً دائماً، فقط مؤشر. */
    private static String vendorFromMac(String mac) {
        if (mac == null) return null;
        String oui = mac.replace(":", "").toUpperCase().substring(0, Math.min(6, mac.replace(":", "").length()));
        String[][] table = {
                {"EC086B", "TP-Link"}, {"F4F26D", "TP-Link"}, {"A42BB0", "TP-Link"}, {"50C7BF", "TP-Link"},
                {"001E42", "Netgear"}, {"204E7F", "Netgear"}, {"E0469A", "Netgear"},
                {"F8A2D6", "Huawei"}, {"A4919C", "Huawei"}, {"48DAF4", "Huawei"}, {"00E0FC", "Huawei"},
                {"D4A1D1", "Xiaomi/Mi"}, {"7C49EB", "Xiaomi/Mi"}, {"28E31F", "Xiaomi/Mi"},
                {"B0486A", "ASUS"}, {"1C872C", "ASUS"}, {"04D4C4", "ASUS"},
                {"001BFC", "D-Link"}, {"14D64D", "D-Link"}, {"C4A81D", "D-Link"},
                {"649EF3", "MikroTik"}, {"4C5E0C", "MikroTik"}, {"6C3B6B", "MikroTik"},
                {"000FB5", "ZTE"}, {"ACE87B", "ZTE"}, {"CC1FC6", "ZTE"},
                {"002722", "Linksys"}, {"48F8B3", "Linksys"},
                {"A0F3C1", "Huawei/ZTE (برج اتصالات)"},
        };
        for (String[] e : table) if (oui.equals(e[0])) return e[1];
        return null;
    }

    private TextView line(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(26), dp(50), dp(26), dp(26));
        setContentView(root);

        root.addView(line("التحكم في شبكتي", 26, Color.WHITE, true));

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network active = cm.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
        boolean isWifi = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);

        if (!isWifi) {
            TextView warn = line("جهازك غير متصل بشبكة واي فاي حالياً. اتصل بالراوتر ثم أعد الفتح.", 15, 0xFFFF5A5A, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(16);
            root.addView(warn, lp);
            return;
        }

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wm.getConnectionInfo();
        DhcpInfo dhcp = wm.getDhcpInfo();
        String gateway = dhcp != null && dhcp.gateway != 0 ? ipToString(dhcp.gateway) : null;
        String ssid = info != null ? info.getSSID() : null;
        if (ssid != null) ssid = ssid.replace("\"", "");
        String bssid = info != null ? info.getBSSID() : null;
        String vendor = vendorFromMac(bssid);

        boolean knownHomeGateway = gateway != null && (gateway.equals("192.168.0.1") || gateway.equals("192.168.1.1")
                || gateway.startsWith("192.168.100.") || gateway.equals("192.168.1.254"));
        String kind = knownHomeGateway ? "شبكة منزلية/محلية معروفة" : "شبكة غير معرّفة (تأكد أنها شبكتك)";

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF1C1C1E);
        bg.setCornerRadius(dp(22));
        card.setBackground(bg);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.topMargin = dp(18);
        card.addView(line("اسم الشبكة (SSID)", 12, 0xFFA0A0A5, false));
        card.addView(line(ssid != null ? ssid : "غير معروف", 17, Color.WHITE, true));
        TextView gLine = line("عنوان الراوتر (Gateway)", 12, 0xFFA0A0A5, false); gLine.setPadding(0, dp(12), 0, 0);
        card.addView(gLine);
        card.addView(line(gateway != null ? gateway : "غير معروف", 17, Color.WHITE, true));
        TextView kLine = line("نوع الشبكة", 12, 0xFFA0A0A5, false); kLine.setPadding(0, dp(12), 0, 0);
        card.addView(kLine);
        card.addView(line(kind, 15, knownHomeGateway ? 0xFF3DDC84 : 0xFFFFB020, false));
        if (vendor != null) {
            TextView vLine = line("الشركة المصنّعة المحتملة", 12, 0xFFA0A0A5, false); vLine.setPadding(0, dp(12), 0, 0);
            card.addView(vLine);
            TextView vv = line(vendor + "  (تخمين من عنوان الجهاز، غير مؤكد 100٪)", 15, Color.WHITE, false);
            card.addView(vv);
        }
        root.addView(card, clp);

        TextView note = line("سيُفتح موقع الراوتر الحقيقي. سجّل دخولك باسم المستخدم وكلمة السر الخاصين بشبكتك (المطبوعين غالباً على ملصق أسفل الراوتر)، ومن هناك تقدر تغيّر اسم الشبكة وكلمة السر، وتدير الأجهزة المسموح لها بالاتصال.", 13, 0xFFA0A0A5, false);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-1, -2); nlp.topMargin = dp(16);
        root.addView(note, nlp);

        if (gateway != null) {
            LinearLayout btn = new LinearLayout(this);
            android.graphics.drawable.GradientDrawable bbg = new android.graphics.drawable.GradientDrawable();
            bbg.setColor(0xFF3E91FF);
            bbg.setCornerRadius(dp(26));
            btn.setBackground(bbg);
            btn.setGravity(Gravity.CENTER);
            TextView t = line("فتح لوحة الراوتر  " + gateway, 16, Color.WHITE, true);
            btn.addView(t);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(54)); blp.topMargin = dp(22);
            root.addView(btn, blp);
            final String gw = gateway;
            btn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openRouterPanel(root, gw); }
            });
        }
    }

    /** يفتح واجهة الراوتر الحقيقية بداخل WebView، ويحافظ على جلسة الدخول (كوكيز) بين المرات */
    private void openRouterPanel(LinearLayout root, String gateway) {
        root.removeAllViews();
        CookieManager.getInstance().setAcceptCookie(true);
        final WebView web = new WebView(this);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        final ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);

        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                getSharedPreferences(PREF, MODE_PRIVATE).edit().putString("last_gateway", url).apply();
            }
        });
        web.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) { bar.setProgress(p); bar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE); }
        });

        String last = getSharedPreferences(PREF, MODE_PRIVATE).getString("last_gateway", null);
        String url = last != null && last.contains(gateway) ? last : "http://" + gateway + "/";
        web.loadUrl(url);

        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(4));
        root.addView(bar, blp);
        root.addView(web, new LinearLayout.LayoutParams(-1, -1));
    }

    @Override
    public void onBackPressed() { super.onBackPressed(); }
}
