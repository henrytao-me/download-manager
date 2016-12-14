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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by henrytao on 12/13/16.
 */

public class Downloader {

  private static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;

  private static int BUFFER_SIZE = 2048;

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

    ResponseInfo response = null;
    InputStream input = null;
    OutputStream output = null;
    try {
      response = initResponse(task);

      long bytesRead = response.bytesRead;
      if (bytesRead == 0) {
        mStorage.update(task.getId(), response.contentLength, response.md5);
      }
      downloading(task.getId(), bytesRead);

      input = response.response.body().byteStream();
      output = new FileOutputStream(response.file, bytesRead != 0);
      byte data[] = new byte[BUFFER_SIZE];
      int count;
      if (!isCanceled(task.getId())) {
        while ((count = input.read(data)) != -1) {
          bytesRead += count;
          output.write(data, 0, count);
          downloading(task.getId(), bytesRead);
          if (isCanceled(task.getId())) {
            break;
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
      // get latest task info
      task = mStorage.find(task.getId());
      if (FileUtils.matchMd5(task.getTempFile(), task.getMd5())) {
        FileUtils.move(task.getTempFile(), task.getDestFile());
      } else {
        FileUtils.delete(task.getTempFile());
        throw new IllegalStateException(String.format(Locale.US, "Mismatch md5 for task %d", task.getId()));
      }
    }
  }

  private void downloading(long id, long bytesRead) {
    mBus.downloading(id, bytesRead);
  }

  private ResponseInfo initResponse(Task task) throws IOException {
    File file = task.getTempFile();
    long bytesRead = task.getBytesRead();
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
