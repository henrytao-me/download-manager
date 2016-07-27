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

import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.henrytao.downloadmanager.config.Constants;
import me.henrytao.downloadmanager.utils.FileUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by henrytao on 7/26/16.
 */
public class Downloader {

  public static Downloader create(String url, String destPath, String destName, String tempPath, String tempName,
      OnStartDownloadListener onStartDownloadListener, OnDownloadingListener onDownloadingListener,
      OnDownloadedListener onDownloadedListener) {
    return new Downloader(url, destPath, destName, tempPath, tempName, onStartDownloadListener, onDownloadingListener,
        onDownloadedListener);
  }

  private final String mDestName;

  private final String mDestPath;

  private final OnDownloadedListener mOnDownloadedListener;

  private final String mTempName;

  private final String mTempPath;

  private final String mUrl;

  private OkHttpClient mClient;

  private boolean mIsClosed;

  private boolean mIsInterrupted;

  private OnDownloadingListener mOnDownloadingListener;

  private OnStartDownloadListener mOnStartDownloadListener;

  protected Downloader(String url, String destPath, String destName, String tempPath, String tempName,
      OnStartDownloadListener onStartDownloadListener, OnDownloadingListener onDownloadingListener,
      OnDownloadedListener onDownloadedListener) {
    mUrl = url;
    mDestPath = destPath;
    mDestName = destName;
    mTempPath = tempPath;
    mTempName = tempName;
    mOnStartDownloadListener = onStartDownloadListener;
    mOnDownloadingListener = onDownloadingListener;
    mOnDownloadedListener = onDownloadedListener;
    mClient = new OkHttpClient.Builder().build();
  }

  public void close() {
    mIsClosed = true;
    mClient = null;
    mOnStartDownloadListener = null;
    mOnDownloadingListener = null;
  }

  public void download() throws IOException {
    File file = getTempFile();
    Pair<Long, Response> executor = execute(file.exists() ? file.length() : 0);
    long bytesRead = executor.first;
    Response response = executor.second;

    // read response
    ResponseBody responseBody = response.body();
    IOException exception = null;
    InputStream input = null;
    OutputStream output = null;
    long contentLength = responseBody.contentLength() + bytesRead;
    try {
      input = responseBody.byteStream();
      output = new FileOutputStream(file, bytesRead != 0);

      byte data[] = new byte[Constants.BUFFER_SIZE];
      int count;

      onStartDownload(bytesRead, contentLength);
      while ((count = input.read(data)) != -1) {
        bytesRead += count;
        output.write(data, 0, count);
        onDownloading(bytesRead, contentLength, bytesRead != contentLength);
        if (mIsInterrupted || mIsClosed) {
          break;
        }
      }
    } catch (IOException ex) {
      exception = ex;
    } finally {
      response.close();
      if (input != null) {
        input.close();
      }
      if (output != null) {
        output.flush();
        output.close();
      }
    }
    if (exception != null) {
      throw exception;
    }
    if (bytesRead == contentLength) {
      onDownloaded(contentLength);
    }
  }

  public void interrupt() {
    mIsInterrupted = true;
  }

  private Pair<Long, Response> execute(long bytesRead) throws IOException {
    Request request = new Request.Builder()
        .url(mUrl)
        .addHeader("Range", "bytes=" + bytesRead + "-")
        .build();
    Response response = mClient.newCall(request).execute();
    if (!response.isSuccessful()) {
      if (response.code() == Constants.Exception.REQUESTED_RANGE_NOT_SATISFIABLE) {
        response.close();
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

  private File getDestFile() {
    return FileUtils.getFile(mDestPath, mDestName);
  }

  private File getTempFile() throws IllegalStateException {
    return FileUtils.getFile(mTempPath, mTempName);
  }

  private void onDownloaded(long contentLength) throws IOException {
    FileUtils.copy(getTempFile(), getDestFile());
    FileUtils.delete(getTempFile());
    if (mOnDownloadedListener != null) {
      mOnDownloadedListener.onDownloaded(contentLength);
    }
  }

  private void onDownloading(long bytesRead, long contentLength, boolean done) {
    if (mOnDownloadingListener != null) {
      mOnDownloadingListener.onDownloading(bytesRead, contentLength, done);
    }
  }

  private void onStartDownload(long bytesRead, long contentLength) {
    if (mOnStartDownloadListener != null) {
      mOnStartDownloadListener.onStartDownload(bytesRead, contentLength);
    }
  }

  public interface OnDownloadedListener {

    void onDownloaded(long contentLength);
  }

  public interface OnDownloadingListener {

    void onDownloading(long bytesRead, long contentLength, boolean done);
  }

  public interface OnStartDownloadListener {

    void onStartDownload(long bytesRead, long contentLength);
  }
}
