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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.Info;
import me.henrytao.downloadmanager.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by henrytao on 12/13/16.
 */

public class Downloader {

  private static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;

  private static DownloadManager.Config getConfig() {
    return DownloadManager.getInstance().getConfig();
  }

  private final Bus mBus;

  private final OkHttpClient mClient;

  private final Storage mStorage;

  public Downloader(Storage storage, Bus bus) {
    mStorage = storage;
    mBus = bus;
    mClient = new OkHttpClient.Builder().build();
  }

  public void download(Task task) throws IOException {
    if (task == null || !task.isActive()) {
      // return without exception to finish job
      return;
    }

    mBus.queueing(task.getId());

    ResponseInfo response = null;
    InputStream input = null;
    OutputStream output = null;
    try {
      File interceptedFile = null;
      Info info = Info.create(mStorage.find(task.getId()), 0, Info.Status.QUEUEING);
      for (Interceptor interceptor : getInterceptors()) {
        interceptedFile = interceptor.onQueueing(info);
        if (interceptedFile != null) {
          break;
        }
      }
      if (interceptedFile != null) {
        File file = task.getTempFile();
        FileUtils.delete(file);
        FileUtils.move(interceptedFile, file, true);
        mStorage.update(task.getId(), file.length(), FileUtils.getMd5(file));
        mBus.downloading(task.getId(), file.length());
      } else {
        response = initResponse(task);
        if (response.response != null) {
          long bytesRead = response.bytesRead;
          if (bytesRead == 0) {
            mStorage.update(task.getId(), response.contentLength, response.md5);
          }
          mBus.downloading(task.getId(), bytesRead);

          input = response.response.body().byteStream();
          output = new FileOutputStream(response.file, bytesRead != 0);
          byte data[] = new byte[getConfig().bufferSize];
          int count;
          if (!isCanceled(task.getId())) {
            while ((count = input.read(data)) != -1) {
              bytesRead += count;
              output.write(data, 0, count);
              mBus.downloading(task.getId(), bytesRead);
              if (isCanceled(task.getId())) {
                break;
              }
            }
          }
        }
      }
    } catch (Exception exception) {
      // increase retry count and forward exception
      mStorage.increaseRetryCount(task.getId());
      throw exception;
    } finally {
      if (response != null && response.response != null) {
        response.response.close();
      }
      if (input != null) {
        //noinspection ThrowFromFinallyBlock
        input.close();
      }
      if (output != null) {
        //noinspection ThrowFromFinallyBlock
        output.close();
      }
    }

    if (!isCanceled(task.getId())) {
      mBus.downloaded(task.getId());
      // get latest task info
      task = mStorage.find(task.getId());
      mBus.validating(task.getId());
      if (FileUtils.matchMd5(task.getTempFile(), task.getMd5())) {
        File renamedOutputFile = FileUtils.move(task.getTempFile(), task.getDestFile(), true);
        mStorage.update(task.getId(), Uri.fromFile(renamedOutputFile));
        // get latest task info and call interceptors
        task = mStorage.find(task.getId());
        for (Interceptor interceptor : getInterceptors()) {
          interceptor.onDownloaded(Info.create(task, task.getContentLength(), Info.Status.DOWNLOADED));
        }
        // on download success
        mStorage.update(task.getId(), Task.State.SUCCESS);
        mBus.succeed(task.getId());
      } else {
        mStorage.update(task.getId(), Task.State.ACTIVE);
        FileUtils.delete(task.getTempFile());
        mBus.failed(task.getId());
        throw new IllegalStateException(String.format(Locale.US, "Mismatch md5 for task %d", task.getId()));
      }
    } else {
      mBus.pausing(task.getId());
    }
  }

  private List<Interceptor> getInterceptors() {
    return DownloadManager.getInstance().getConfig().interceptors;
  }

  private ResponseInfo initResponse(Task task) throws IOException {
    File file = task.getTempFile();
    long contentLength = task.getContentLength();
    long bytesRead = task.getBytesRead();
    if (bytesRead == contentLength && contentLength > 0 && FileUtils.matchMd5(file, task.getMd5())) {
      return new ResponseInfo(file, null, task.getMd5(), task.getContentLength(), bytesRead);
    }
    Request request = new Request.Builder()
        .url(task.getUri().toString())
        .addHeader("Range", "bytes=" + bytesRead + "-")
        .build();
    Response response = mClient.newCall(request).execute();
    if (!response.isSuccessful()) {
      if (response.code() == REQUESTED_RANGE_NOT_SATISFIABLE) {
        response.close();
        FileUtils.delete(file);
        return initResponse(task);
      } else {
        throw new IOException("Unexpected code " + response);
      }
    }
    return new ResponseInfo(file, response, response.header("ETag"), bytesRead + response.body().contentLength(), bytesRead);
  }

  private boolean isCanceled(long id) {
    Task task = mStorage.find(id);
    return task == null || !task.isActive();
  }

  private static class ResponseInfo {

    private final long bytesRead;

    private final long contentLength;

    private final File file;

    private final String md5;

    private final Response response;

    ResponseInfo(File file, Response response, String md5, long contentLength, long bytesRead) {
      this.file = file;
      this.response = response;
      this.md5 = md5;
      this.contentLength = contentLength;
      this.bytesRead = bytesRead;
    }
  }
}
