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

import android.net.Uri;
import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.henrytao.downloadmanager.config.Constants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by henrytao on 7/26/16.
 */
public class Downloader {

  public static Downloader create(String url, String destPath, String destName, ProgressListener progressListener) {
    return new Downloader(url, destPath, destName, progressListener);
  }

  private final OkHttpClient mClient;

  private final String mDestName;

  private final String mDestPath;

  private final ProgressListener mProgressListener;

  private final String mUrl;

  protected Downloader(String url, String destPath, String destName, ProgressListener progressListener) {
    mUrl = url;
    mDestPath = destPath;
    mDestName = destName;
    mProgressListener = progressListener;
    mClient = new OkHttpClient.Builder().build();
  }

  public void download() throws IllegalStateException, IOException {
    File file = getDestFile();
    Pair<Long, Response> executor = execute(file.exists() ? file.length() : 0);
    long bytesRead = executor.first;
    Response response = executor.second;

    // read response
    ResponseBody responseBody = response.body();
    IOException exception = null;
    InputStream input = null;
    OutputStream output = null;
    try {
      input = responseBody.byteStream();
      output = new FileOutputStream(file, bytesRead != 0);

      long contentLength = responseBody.contentLength() + bytesRead;
      byte data[] = new byte[Constants.BUFFER_SIZE];
      int count;
      while ((count = input.read(data)) != -1) {
        bytesRead += count;
        output.write(data, 0, count);
        onDownloading(bytesRead, contentLength, bytesRead != contentLength);
      }

      output.flush();
    } catch (IOException ex) {
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

  private Pair<Long, Response> execute(long bytesRead) throws IOException {
    Request request = new Request.Builder()
        .url(mUrl)
        .addHeader("Range", "bytes=" + bytesRead + "-")
        .build();
    Response response = mClient.newCall(request).execute();
    if (!response.isSuccessful()) {
      if (response.code() == Constants.Exception.REQUESTED_RANGE_NOT_SATISFIABLE) {
        // reset downloader if it's out of range
        bytesRead = 0;
        request = new Request.Builder()
            .url(mUrl)
            .build();
        response = mClient.newCall(request).execute();
      } else {
        throw new IOException("Unexpected code " + response);
      }
    }
    return new Pair<>(bytesRead, response);
  }

  private File getDestFile() throws IllegalStateException {
    File file = new File(Uri.parse(mDestPath).getPath());
    if (!file.exists() && !file.mkdirs()) {
      throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
    }
    return new File(file, mDestName);
  }

  private void onDownloading(long bytesRead, long contentLength, boolean done) {
    if (mProgressListener != null) {
      mProgressListener.onDownloading(bytesRead, contentLength, done);
    }
  }

  public interface ProgressListener {

    void onDownloading(long bytesRead, long contentLength, boolean done);
  }
}
