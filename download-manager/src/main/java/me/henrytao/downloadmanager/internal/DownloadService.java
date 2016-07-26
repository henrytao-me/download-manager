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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.utils.Logger;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Created by henrytao on 7/25/16.
 */
public class DownloadService extends IntentService {

  public static final String EXTRA_REQUEST = "EXTRA_REQUEST";

  private final OkHttpClient mClient;

  private final Logger mLogger;

  private final ProgressListener mProgressListener;

  protected DownloadService(String name) {
    super(name);
    setIntentRedelivery(true);
    mLogger = Logger.newInstance(Logger.LogLevel.VERBOSE);
    mLogger.d("onHandleIntent | initialized");

    mProgressListener = (bytesRead, contentLength, done) -> {
      mLogger.d("Progress: %d%% done\n", (100 * bytesRead) / contentLength);
    };

    mClient = new OkHttpClient.Builder()
        .addNetworkInterceptor(chain -> {
          Response response = chain.proceed(chain.request());
          return response.newBuilder()
              .body(new ProgressResponseBody(response.body(), mProgressListener))
              .build();
        })
        .build();
  }

  public DownloadService() {
    this(DownloadService.class.getSimpleName());
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    DownloadManager.Request request = intent.getParcelableExtra(EXTRA_REQUEST);
    mLogger.d("onHandleIntent | downloading");

    try {
      download(request.getUri().toString(), request.getDestinationUri().getPath(), request.getTitle());
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    mLogger.d("onHandleIntent | downloaded");
  }

  private void download(String url, String destPath, String destName) throws IOException {
    Request request = new Request.Builder()
        .url(url)
        .build();
    Response response = mClient.newCall(request).execute();
    if (!response.isSuccessful()) {
      throw new IOException("Unexpected code " + response);
    }

    Headers responseHeaders = response.headers();
    mLogger.d("download | %s", responseHeaders.toString());
    mLogger.d("download | %s", response.body().toString());

    if (response.isSuccessful()) {
      InputStream input = null;
      OutputStream output = null;

      input = response.body().byteStream();
      long length = response.body().contentLength();

      File file = new File(destPath, destName);

      output = new FileOutputStream(file);
      byte data[] = new byte[1024];

      //mLogger.d("progress | %s", "0%");
      long total = 0;
      int count;
      while ((count = input.read(data)) != -1) {
        total += count;

        //mLogger.d("progress | %s", String.valueOf(total * 100 / length) + "%");

        output.write(data, 0, count);
      }
      output.flush();
      output.close();
      input.close();
    }
  }

  private interface ProgressListener {

    void update(long bytesRead, long contentLength, boolean done);
  }

  private static class ProgressResponseBody extends ResponseBody {

    private final ProgressListener progressListener;

    private final ResponseBody responseBody;

    private BufferedSource bufferedSource;

    public ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
      this.responseBody = responseBody;
      this.progressListener = progressListener;
    }

    @Override
    public long contentLength() {
      return responseBody.contentLength();
    }

    @Override
    public MediaType contentType() {
      return responseBody.contentType();
    }

    @Override
    public BufferedSource source() {
      if (bufferedSource == null) {
        bufferedSource = Okio.buffer(source(responseBody.source()));
      }
      return bufferedSource;
    }

    private Source source(Source source) {
      return new ForwardingSource(source) {
        long totalBytesRead = 0L;

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
          long bytesRead = super.read(sink, byteCount);
          // read() returns the number of bytes read, or -1 if this source is exhausted.
          totalBytesRead += bytesRead != -1 ? bytesRead : 0;
          progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
          return bytesRead;
        }
      };
    }
  }
}
