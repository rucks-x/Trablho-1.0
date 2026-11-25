package com.vagabundogames.relatorio;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private WebView web;
    private ValueCallback<Uri[]> chooser;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
        });
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                try {
                    startActivityForResult(p.createIntent(), 1);
                } catch (Exception e) {
                    chooser = null;
                    return false;
                }
                return true;
            }
        });
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1 && chooser != null) {
            chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            chooser = null;
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.goBack?window.goBack():false", new ValueCallback<String>() {
            public void onReceiveValue(String v) {
                if (!"true".equals(v)) MainActivity.super.onBackPressed();
            }
        });
    }

    private void toast(final String t) {
        runOnUiThread(new Runnable() { public void run() { Toast.makeText(MainActivity.this, t, Toast.LENGTH_LONG).show(); } });
    }

    private class Bridge {
        @JavascriptInterface
        public void saveFile(String name, String b64) {
            try {
                byte[] d = Base64.decode(b64, Base64.DEFAULT);
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, new File(name).getName());
                v.put(MediaStore.Downloads.MIME_TYPE, XLSX);
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                OutputStream o = getContentResolver().openOutputStream(u);
                o.write(d);
                o.close();
                toast("Salvo em Downloads: " + name);
            } catch (Exception e) {
                toast("Erro ao salvar: " + e.getMessage());
            }
        }

        @JavascriptInterface
        public void shareFiles(String json) {
            try {
                JSONArray a = new JSONArray(json);
                File dir = new File(getCacheDir(), "shared");
                dir.mkdirs();
                File[] old = dir.listFiles();
                if (old != null) for (File o : old) o.delete();
                final ArrayList<Uri> uris = new ArrayList<Uri>();
                for (int k = 0; k < a.length(); k++) {
                    JSONObject o = a.getJSONObject(k);
                    String base = new File(o.getString("name")).getName();
                    File f = new File(dir, base);
                    int n = 1;
                    while (f.exists()) f = new File(dir, (n++) + "_" + base);
                    FileOutputStream os = new FileOutputStream(f);
                    os.write(Base64.decode(o.getString("b64"), Base64.DEFAULT));
                    os.close();
                    uris.add(FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", f));
                }
                final Intent i = new Intent(uris.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);
                i.setType(XLSX);
                if (uris.size() > 1) i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                else i.putExtra(Intent.EXTRA_STREAM, uris.get(0));
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(new Runnable() { public void run() { startActivity(Intent.createChooser(i, "Compartilhar")); } });
            } catch (Exception e) {
                toast("Erro ao compartilhar: " + e.getMessage());
            }
        }
    }
}
