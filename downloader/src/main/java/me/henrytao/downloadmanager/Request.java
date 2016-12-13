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

import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.UUID;

import me.henrytao.downloadmanager.internal.Precondition;

/**
 * Created by henrytao on 12/12/16.
 */

public final class Request {

  private static final int DEFAULT_RETRY = 5;

  private final String mDescription;

  private final Uri mDestUri;

  private final int mRetry;

  private final String mTag;

  private final Uri mTempUri;

  private final String mTitle;

  private final Uri mUri;

  private long mId;

  private Request(String tag, Uri uri, String title, String description, Uri destUri, Uri tempUri, int retry) {
    mId = -1;
    mTag = tag;
    mUri = uri;
    mTitle = title;
    mDescription = description;
    mDestUri = destUri;
    mTempUri = tempUri;
    mRetry = retry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Request request = (Request) o;
    if (mRetry != request.mRetry) {
      return false;
    }
    if (mId != request.mId) {
      return false;
    }
    if (mDescription != null ? !mDescription.equals(request.mDescription) : request.mDescription != null) {
      return false;
    }
    if (!mDestUri.equals(request.mDestUri)) {
      return false;
    }
    if (!mTag.equals(request.mTag)) {
      return false;
    }
    if (!mTempUri.equals(request.mTempUri)) {
      return false;
    }
    if (mTitle != null ? !mTitle.equals(request.mTitle) : request.mTitle != null) {
      return false;
    }
    return mUri.equals(request.mUri);
  }

  @Override
  public int hashCode() {
    int result = mDescription != null ? mDescription.hashCode() : 0;
    result = 31 * result + mDestUri.hashCode();
    result = 31 * result + mRetry;
    result = 31 * result + mTag.hashCode();
    result = 31 * result + mTempUri.hashCode();
    result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
    result = 31 * result + mUri.hashCode();
    result = 31 * result + (int) (mId ^ (mId >>> 32));
    return result;
  }

  public int enqueue() {
    return DownloadManager.getInstance().enqueue(this);
  }

  public String getDescription() {
    return mDescription;
  }

  public Uri getDestUri() {
    return mDestUri;
  }

  public long getId() {
    return mId;
  }

  void setId(long id) {
    mId = id;
  }

  public int getRetry() {
    return mRetry;
  }

  public String getTag() {
    return mTag;
  }

  public Uri getTempUri() {
    return mTempUri;
  }

  public String getTitle() {
    return mTitle;
  }

  public Uri getUri() {
    return mUri;
  }

  public boolean isEnqueued() {
    return mId >= 0;
  }

  public static final class Builder {

    private final String mTag;

    private String mDescription;

    private String mDestFilename;

    private Uri mDestPath;

    private Uri mDestUri;

    private int mRetry;

    private String mTempFilename;

    private Uri mTempPath;

    private Uri mTempUri;

    private String mTitle;

    private Uri mUri;

    public Builder(@NonNull String tag) {
      mTag = tag;
      mRetry = DEFAULT_RETRY;
    }

    public Request build() {
      mUri = Precondition.checkNotNull(mUri);
      mDestFilename = Precondition.checkNotEmpty(mDestFilename, mUri.getPath());
      mTempFilename = Precondition.checkNotEmpty(mTempFilename, UUID.randomUUID().toString());
      return new Request(
          Precondition.checkNotEmpty(mTag),
          Precondition.checkNotNull(mUri),
          mTitle,
          mDescription,
          Precondition.checkNotNull(mDestUri, Uri.withAppendedPath(Precondition.checkNotNull(mDestPath), mDestFilename)),
          Precondition.checkNotNull(mTempUri, Uri.withAppendedPath(Precondition.checkNotNull(mTempPath), mTempFilename)),
          mRetry
      );
    }

    public Builder setDescription(String description) {
      mDescription = description;
      return this;
    }

    public Builder setDestFilename(String destFilename) {
      mDestFilename = destFilename;
      return this;
    }

    public Builder setDestPath(Uri destPath) {
      mDestPath = destPath;
      return this;
    }

    public Builder setDestUri(Uri destUri) {
      mDestUri = destUri;
      return this;
    }

    public Builder setRetry(int count) {
      mRetry = count;
      return this;
    }

    public Builder setTempFilename(String tempFilename) {
      mTempFilename = tempFilename;
      return this;
    }

    public Builder setTempPath(Uri tempPath) {
      mTempPath = tempPath;
      return this;
    }

    public Builder setTempUri(Uri tempUri) {
      mTempUri = tempUri;
      return this;
    }

    public Builder setTitle(String title) {
      mTitle = title;
      return this;
    }

    public Builder setUri(Uri uri) {
      mUri = uri;
      return this;
    }
  }
}
