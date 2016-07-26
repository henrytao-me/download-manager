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
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

import me.henrytao.downloadmanager.data.DownloadInfo;
import me.henrytao.downloadmanager.internal.DownloadService;
import rx.Observable;

/**
 * Created by henrytao on 7/25/16.
 */
public class DownloadManager {

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

  protected DownloadManager(Context context) {
    mContext = context.getApplicationContext();
  }

  public long enqueue(Request request) {
    Intent intent = new Intent(mContext, DownloadService.class);
    intent.putExtra(DownloadService.EXTRA_REQUEST, request);
    mContext.startService(intent);
    return 0;
  }

  public long findId(Request request) {
    return 0;
  }

  public Observable<DownloadInfo> observe(long id) {
    return Observable.create(subscriber -> {

    });
  }

  public static class Request implements Parcelable {

    public static final Creator<Request> CREATOR = new Creator<Request>() {
      @Override
      public Request createFromParcel(Parcel source) {
        return new Request(source);
      }

      @Override
      public Request[] newArray(int size) {
        return new Request[size];
      }
    };

    private final Uri mUri;

    private String mDescription;

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

    protected Request(Parcel in) {
      this.mUri = in.readParcelable(Uri.class.getClassLoader());
      this.mDescription = in.readString();
      this.mDestinationUri = in.readParcelable(Uri.class.getClassLoader());
      this.mTitle = in.readString();
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeParcelable(this.mUri, flags);
      dest.writeString(this.mDescription);
      dest.writeParcelable(this.mDestinationUri, flags);
      dest.writeString(this.mTitle);
    }

    public String getDescription() {
      return mDescription;
    }

    public Request setDescription(String description) {
      mDescription = description;
      return this;
    }

    public Uri getDestinationUri() {
      return mDestinationUri;
    }

    public String getTitle() {
      return mTitle;
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
          throw new IllegalStateException(file.getAbsolutePath() +
              " already exists and is not a directory");
        }
      } else {
        if (!file.mkdirs()) {
          throw new IllegalStateException("Unable to create directory: " +
              file.getAbsolutePath());
        }
      }
      setDestinationFromBase(file, subPath);
      return this;
    }

    private void setDestinationFromBase(File base, String subPath) {
      if (subPath == null) {
        throw new NullPointerException("subPath cannot be null");
      }
      mDestinationUri = Uri.withAppendedPath(Uri.fromFile(base), subPath);
    }
  }
}
