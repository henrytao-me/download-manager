/*
 * Copyright 2016 "Henry Tao <hi@henrytao.me>"
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.henrytao.downloadmanager.internal;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.config.Constants;
import me.henrytao.downloadmanager.utils.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by henrytao on 7/25/16.
 */
public class DownloadService extends IntentService {

  public static final String EXTRA_REQUEST = "EXTRA_REQUEST";

  private final OkHttpClient mClient;

  private final Logger mLogger;

  protected DownloadService(String name) {
    super(name);
    setIntentRedelivery(true);
    mLogger = Logger.newInstance(DownloadManager.DEBUG ? Logger.LogLevel.VERBOSE : Logger.LogLevel.NONE);

    mClient = new OkHttpClient.Builder().build();

    mLogger.d("onHandleIntent | initialized");
  }

  public DownloadService() {
    this(DownloadService.class.getSimpleName());
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    DownloadManager.Request request = intent.getParcelableExtra(EXTRA_REQUEST);
    try {
      download(request.getUri().toString(), request.getDestinationUri().toString(), request.getTitle());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void download(String url, String destPath, String destName) throws Exception {
    Request request = new Request.Builder()
        .url(url)
        .build();
    Response response = mClient.newCall(request).execute();
    if (!response.isSuccessful()) {
      throw new IOException("Unexpected code " + response);
    }

    mLogger.d("download | %s", response.headers().toString());

    ResponseBody responseBody = response.body();
    Exception exception = null;
    InputStream input = null;
    OutputStream output = null;
    try {
      File file = new File(Uri.parse(destPath).getPath());
      if (!file.exists() && !file.mkdirs()) {
        throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
      }
      file = new File(file, destName);

      input = responseBody.byteStream();
      output = new FileOutputStream(file);

      byte data[] = new byte[Constants.BUFFER_SIZE];
      long contentLength = responseBody.contentLength();
      long bytesRead = 0;
      int count;
      while ((count = input.read(data)) != -1) {
        bytesRead += count;
        output.write(data, 0, count);
        onDownloading(bytesRead, contentLength, bytesRead != contentLength);
      }

      output.flush();
    } catch (Exception ex) {
      exception = ex;
    } finally {
      if (input != null) {
        input.close();
      }
      if (output != null) {
        output.close();
      }
    }
    if (exception != null) {
      throw exception;
    }
  }

  private void onDownloading(long bytesRead, long contentLength, boolean done) {
    int percentage = (int) ((100 * bytesRead) / contentLength);
    mLogger.d("Progress: %d%% done", percentage);
  }
}
