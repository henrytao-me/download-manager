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
import android.net.Uri;
import android.os.Environment;

import java.io.File;

import me.henrytao.downloadmanager.internal.DownloadBus;
import rx.Observable;

/**
 * Created by henrytao on 7/25/16.
 */
public class DownloadManager {

  public static boolean DEBUG = false;

  private static DownloadManager sInstance;

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

  private final Context mContext;

  private final DownloadBus mDownloadBus;

  protected DownloadManager(Context context) {
    mContext = context.getApplicationContext();
    mDownloadBus = DownloadBus.getInstance(mContext);
  }

  public long enqueue(Request request) {
    return mDownloadBus.enqueue(request);
  }

  public long enqueue(Request request, boolean shouldStartNow) {
    return mDownloadBus.enqueue(request, shouldStartNow);
  } 

  public Info.State getState(long id) {
    return mDownloadBus.getState(id);
  }

  public void initialize() {
    mDownloadBus.enqueue(0);
  }

  public Observable<Info> observe(long id) {
    return observe(id, false);
  }

  public Observable<Info> observe(long id, boolean shouldThrowError) {
    return mDownloadBus.observe(id, shouldThrowError);
  }

  public void pause(long id) {
    mDownloadBus.pause(id);
  }

  public void resume(long id) {
    mDownloadBus.resume(id);
  }

  public void setTempPath(Uri tempPath) {
    mDownloadBus.setTempPath(tempPath);
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
