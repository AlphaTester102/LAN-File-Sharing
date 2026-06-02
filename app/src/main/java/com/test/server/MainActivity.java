package com.test.server;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Enumeration;

// MainActivity serves as the entry point for the app, managing the WebView and server interactions.
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private WebView webView;
    private EmbeddedServer server;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private int serverPort = 8080;

    private final ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String scannedUrl = result.getContents();
                    runOnUiThread(() -> {
                        webView.loadUrl(scannedUrl);
                    Toast.makeText(this, "Scanned: " + scannedUrl, Toast.LENGTH_SHORT).show();
                    });
                }
            });

    public void scanQRCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan Server QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        qrLauncher.launch(options);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        webView = findViewById(R.id.webview);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                try {
                    startActivityForResult(Intent.createChooser(intent, "Select files"), FILE_CHOOSER_REQUEST_CODE);
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            android.app.DownloadManager.Request request =
                    new android.app.DownloadManager.Request(Uri.parse(url));

            request.addRequestHeader("User-Agent", userAgent);

            request.setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            request.setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                    android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
            );

            android.app.DownloadManager dm =
                    (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            dm.enqueue(request);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });

        webView.loadUrl("file:///android_asset/home.html");
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void startServer(String mode, String password) {
            runOnUiThread(() -> {
                try {
                    if (server == null) {
                        serverPort = findAvailablePort(8080);
                        server = new EmbeddedServer(MainActivity.this, serverPort);
                    }
                    if (!server.isAlive()) {
                        server.start();
                    }

                    EmbeddedServer.ServerMode serverMode = "private".equals(mode)
                            ? EmbeddedServer.ServerMode.PRIVATE
                            : EmbeddedServer.ServerMode.PUBLIC;
                    server.setServerConfig(serverMode, password);

                    webView.clearCache(true);
                    webView.loadUrl("http://localhost:" + serverPort + "/");

                    String label = "private".equals(mode) ? "Private Server Started" : "Public Server Started";
                    Toast.makeText(MainActivity.this, label, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e(TAG, "Server failed to start", e);
                    Toast.makeText(MainActivity.this, "Failed to start server", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void joinServer(String ip) {
            runOnUiThread(() -> {
                if (ip == null || ip.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter an IP address", Toast.LENGTH_SHORT).show();
                    return;
                }
                String url = ip.startsWith("http") ? ip : "http://" + ip + ":" + serverPort + "/";
                webView.loadUrl(url);
            });
        }

        @JavascriptInterface
        public void scanQRCode() {
            MainActivity.this.scanQRCode();
        }

        @JavascriptInterface
        public void requestQRCode() {
            runOnUiThread(() -> {
                try {
                    String base = "http://" + getLocalIpAddress() + ":" + serverPort;
                    String qrToken = (server != null) ? server.generateQrToken() : null;
                    String serverUrl = (qrToken != null) ? base + "/?qr_token=" + qrToken : base;
                    String base64QR = generateQRCodeBase64(serverUrl);
                    webView.evaluateJavascript(
                            "displayQRCode('data:image/png;base64," + base64QR + "')",
                            null
                    );
                } catch (Exception e) {
                    Log.e(TAG, "QR Code request error", e);
                }
            });
        }
    }

    private String generateQRCodeBase64(String text) throws Exception {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                500,
                500
        );

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(
                        x,
                        y,
                        bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF
                );
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();

        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.clearUploads();
            server.stop();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST_CODE || filePathCallback == null) {
            return;
        }

        Uri[] results = null;

        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                results = new Uri[0];
            } else if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) {
                    results[i] = data.getClipData().getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                results = new Uri[] { data.getData() };
            }
        }

        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    private int findAvailablePort(int startPort) {
        for (int port = startPort; port < startPort + 20; port++) {
            try (ServerSocket ss = new ServerSocket(port)) {
                return ss.getLocalPort();
            } catch (IOException ignored) {
                // port in use, try next
            }
        }
        // fallback: let OS assign an ephemeral port
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        } catch (IOException e) {
            return startPort; // last resort
        }
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting IP address", e);
        }
        return "127.0.0.1";
    }
}
