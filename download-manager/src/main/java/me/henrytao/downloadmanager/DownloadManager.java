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

package me.henrytao.downloadmanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.util.UUID;

import me.henrytao.downloadmanager.internal.DownloadBus;
import me.henrytao.downloadmanager.internal.DownloadDbHelper;
import me.henrytao.downloadmanager.internal.DownloadInfo;
import me.henrytao.downloadmanager.internal.DownloadService;
import rx.Observable;

/**
 * Created by henrytao on 7/25/16.
 */
public class DownloadManager {

  public static boolean DEBUG = false;

  private static DownloadManager sInstance;

  private static Uri sTempPath;

  public static DownloadManager getInstance(Context context) {
    if (sInstance == null) {
      synchronized (DownloadManager.class) {
        if (sInstance == null) {
          sInstance = new DownloadManager(context);
        }
      }
    }
    return sInstance;
  }

  public static void setTempPath(Uri tempPath) {
    sTempPath = tempPath;
  }

  private static Uri getTempPath(Context context) {
    return sTempPath != null ? sTempPath : Uri.fromFile(context.getCacheDir());
  }

  private final Context mContext;

  private final DownloadBus mDownloadBus;

  private final DownloadDbHelper mDownloadDbHelper;

  protected DownloadManager(Context context) {
    mContext = context.getApplicationContext();
    mDownloadBus = DownloadBus.getInstance();
    mDownloadDbHelper = DownloadDbHelper.create(mContext);
  }

  public long enqueue(Request request) {
    request.validate();
    DownloadInfo downloadInfo = DownloadInfo.create(request, getTempPath(mContext), UUID.randomUUID().toString());
    long id = mDownloadDbHelper.insert(downloadInfo);
    mDownloadBus.enqueue(id);
    enqueue(id);
    return id;
  }

  public void enqueue(long id) {
    Intent intent = new Intent(mContext, DownloadService.class);
    intent.putExtra(DownloadService.EXTRA_DOWNLOAD_ID, id);
    mContext.startService(intent);
  }

  public Observable<Info> observe(long id) {
    if (!mDownloadBus.exist(id)) {
      DownloadInfo downloadInfo = mDownloadDbHelper.find(id);
      if (downloadInfo != null) {
        switch (downloadInfo.getState()) {
          case DOWNLOADED:
            mDownloadBus.downloaded(id, downloadInfo.getContentLength());
            break;
          case DOWNLOADING:
            mDownloadBus.enqueue(id);
            break;
          case PAUSED:
            mDownloadBus.pause(id);
            break;
          default:
            mDownloadBus.invalid(id);
            break;
        }
      }
    }
    return mDownloadBus.observe(id);
  }

  public void pause(long id) {
    mDownloadDbHelper.updateState(id, DownloadInfo.State.PAUSED);
    mDownloadBus.pause(id);
  }

  public void resume(long id) {
    mDownloadDbHelper.updateState(id, DownloadInfo.State.DOWNLOADING);
    mDownloadBus.resume(id);
    enqueue(id);
  }

  public static class Request {

    private final Uri mUri;

    private Uri mDestinationUri;

    private String mTitle;

    public Request(Uri uri) {
      if (uri == null) {
        throw new NullPointerException();
      }
      String scheme = uri.getScheme();
      if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
        throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + uri);
      }
      mUri = uri;
    }

    public Request(String uri) {
      this(Uri.parse(uri));
    }

    public Uri getDestinationUri() {
      return mDestinationUri;
    }

    public Request setDestinationUri(Uri uri) {
      mDestinationUri = uri;
      return this;
    }

    public String getTitle() {
      return mTitle == null || mTitle.length() == 0 ? mUri.getLastPathSegment() : mTitle;
    }

    public Request setTitle(String title) {
      mTitle = title;
      return this;
    }

    public Uri getUri() {
      return mUri;
    }

    public Request setDestinationInExternalPublicDir(String dirType, String subPath) {
      File file = Environment.getExternalStoragePublicDirectory(dirType);
      if (file == null) {
        throw new IllegalStateException("Failed to get external storage public directory");
      } else if (file.exists()) {
        if (!file.isDirectory()) {
          throw new IllegalStateException(file.getAbsolutePath() + " already exists and is not a directory");
        }
      } else {
        if (!file.mkdirs()) {
          throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
        }
      }
      setDestinationFromBase(file, subPath);
      return this;
    }

    public void validate() throws IllegalArgumentException {
      if (getUri() == null) {
        throw new IllegalArgumentException("Uri can not be null");
      }
      if (getDestinationUri() == null) {
        throw new IllegalArgumentException("DestinationUri can not be null");
      }
      if (getTitle() == null || getTitle().length() == 0) {
        throw new IllegalArgumentException("Title can not be null");
      }
    }

    private void setDestinationFromBase(File base, String subPath) {
      if (subPath == null) {
        throw new NullPointerException("subPath cannot be null");
      }
      mDestinationUri = Uri.withAppendedPath(Uri.fromFile(base), subPath);
    }
  }
}
