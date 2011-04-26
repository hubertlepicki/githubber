package com.amberbit.githubber;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.app.ProgressDialog;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.webkit.WebView;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.SharedPreferences;

public class FeedActivity extends Activity
{
    ProgressDialog loading_dialog = null;
    String feed_data = null;
    WebView feed_view;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed);
        feed_view = (WebView) findViewById(R.id.feed_view);
        feed_view.getSettings().setJavaScriptEnabled(true);
        //feed_view.loadUrl("http://amberbit.com");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.feed_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.reload_feed:
            loadFeed();
            return true;
        case R.id.open_settings:
            Intent i = new Intent(this, OptionsActivity.class);
            startActivity(i);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void loadFeed() {
      loading_dialog = ProgressDialog.show(FeedActivity.this, "", "Loading. Please wait...", true);

      Thread thread = new Thread(null, getActivityFeed, "Background");
      thread.start();
    }

    private Runnable getActivityFeed = new Runnable() {
      public void run() {
        String data = readURL("https://github.com/hubertlepicki.private.atom");
        if (data != null) {
          feed_data = data;
        }

        runOnUiThread(new Runnable() {
          public void run() {
            loading_dialog.dismiss();
            updateFeedView();
          }
        });
      }
    };

    private void updateFeedView() {
      feed_view.loadData(feed_data, "text/html", "utf-8");
    }

    private String username() { 
      Context context = getApplicationContext();
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      return prefs.getString("GITHUB_USERNAME", "");
    }

    private String password() { 
      Context context = getApplicationContext();
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      return prefs.getString("GITHUB_PASSWORD", "");
    }

    private String readURL(String address) {
      Authenticator.setDefault(new Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(username(), password().toCharArray());
        }
      });

      String data = null;
      InputStream in = null;
      int response = -1;
      System.out.println("Attempting to fetch URL: " + address);
      try {

          URL url = new URL(address);
          URLConnection conn = url.openConnection();
          HttpURLConnection httpConn = (HttpURLConnection) conn;
          httpConn.setAllowUserInteraction(false);
          httpConn.setInstanceFollowRedirects(true);
          httpConn.setRequestMethod("GET");
          httpConn.connect(); 

          response = httpConn.getResponseCode();
          System.out.println("HTTP response code: " + response);
          if (response == HttpURLConnection.HTTP_OK) {
              in = httpConn.getInputStream();
          }
          data = convertStreamToString(in);
          System.out.println(data);
          return data;
      }
      catch (Exception ex) {
        System.out.println("Exception when fetching URL: ");
        System.out.println(ex.toString());
        return null;
      }
    }

    public String convertStreamToString(InputStream is)
            throws IOException {
        /*
         * To convert the InputStream to String we use the
         * Reader.read(char[] buffer) method. We iterate until the
         * Reader return -1 which means there's no more data to
         * read. We use the StringWriter class to produce the string.
         */
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {        
            return "";
        }
    }
}

