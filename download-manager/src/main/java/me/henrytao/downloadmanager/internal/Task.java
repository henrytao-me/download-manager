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

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.SparseArray;

import java.io.File;
import java.io.IOException;

import me.henrytao.downloadmanager.Request;

/**
 * Created by henrytao on 7/25/16.
 */
public class Task {

  static final String NAME = "task";

  static Task create(Cursor cursor) {
    if (cursor == null || cursor.isClosed() || !cursor.moveToFirst()) {
      return null;
    }
    return new Task(
        cursor.getLong(cursor.getColumnIndex(Fields.ID)),
        cursor.getString(cursor.getColumnIndex(Fields.TAG)),
        Uri.parse(cursor.getString(cursor.getColumnIndex(Fields.URI))),
        cursor.getString(cursor.getColumnIndex(Fields.TITLE)),
        cursor.getString(cursor.getColumnIndex(Fields.DESCRIPTION)),
        Uri.parse(cursor.getString(cursor.getColumnIndex(Fields.DEST_URI))),
        Uri.parse(cursor.getString(cursor.getColumnIndex(Fields.TEMP_URI))),
        cursor.getInt(cursor.getColumnIndex(Fields.MAX_RETRY)),
        cursor.getInt(cursor.getColumnIndex(Fields.RETRY_COUNT)),
        State.from(cursor.getInt(cursor.getColumnIndex(Fields.STATE))),
        cursor.getLong(cursor.getColumnIndex(Fields.CONTENT_LENGTH)),
        cursor.getString(cursor.getColumnIndex(Fields.MD5))
    );
  }

  static Task create(Request request) {
    return new Task(request.getId(), request.getTag(), request.getUri(), request.getTitle(), request.getDescription(), request.getDestUri(),
        request.getTempUri(), request.getRetry(), 0, State.ACTIVE, 0, null);
  }

  private final String mMd5;

  private long mContentLength;

  private String mDescription;

  private Uri mDestUri;

  private long mId;

  private int mMaxRetry;

  private int mRetryCount;

  private State mState;

  private String mTag;

  private Uri mTempUri;

  private String mTitle;

  private Uri mUri;

  protected Task(long id, String tag, Uri uri, String title, String description, Uri destUri, Uri tempUri, int maxRetry, int retryCount,
      State state, long contentLength, String md5) {
    mId = id;
    mTag = tag;
    mUri = uri;
    mTitle = title;
    mDescription = description;
    mDestUri = destUri;
    mTempUri = tempUri;
    mMaxRetry = maxRetry;
    mRetryCount = retryCount;
    mState = state;
    mContentLength = contentLength;
    mMd5 = md5;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Task task = (Task) o;
    if (mContentLength != task.mContentLength) {
      return false;
    }
    if (mId != task.mId) {
      return false;
    }
    if (mMaxRetry != task.mMaxRetry) {
      return false;
    }
    if (mRetryCount != task.mRetryCount) {
      return false;
    }
    if (mMd5 != null ? !mMd5.equals(task.mMd5) : task.mMd5 != null) {
      return false;
    }
    if (mDescription != null ? !mDescription.equals(task.mDescription) : task.mDescription != null) {
      return false;
    }
    if (!mDestUri.equals(task.mDestUri)) {
      return false;
    }
    if (mState != task.mState) {
      return false;
    }
    if (mTag != null ? !mTag.equals(task.mTag) : task.mTag != null) {
      return false;
    }
    if (!mTempUri.equals(task.mTempUri)) {
      return false;
    }
    if (mTitle != null ? !mTitle.equals(task.mTitle) : task.mTitle != null) {
      return false;
    }
    return mUri.equals(task.mUri);
  }

  @Override
  public int hashCode() {
    int result = mMd5 != null ? mMd5.hashCode() : 0;
    result = 31 * result + (int) (mContentLength ^ (mContentLength >>> 32));
    result = 31 * result + (mDescription != null ? mDescription.hashCode() : 0);
    result = 31 * result + mDestUri.hashCode();
    result = 31 * result + (int) (mId ^ (mId >>> 32));
    result = 31 * result + mMaxRetry;
    result = 31 * result + mRetryCount;
    result = 31 * result + (mState != null ? mState.hashCode() : 0);
    result = 31 * result + (mTag != null ? mTag.hashCode() : 0);
    result = 31 * result + mTempUri.hashCode();
    result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
    result = 31 * result + mUri.hashCode();
    return result;
  }

  public long getBytesRead() {
    try {
      File file = getTempFile();
      return file.exists() ? file.length() : 0;
    } catch (Exception ignore) {
    }
    return 0;

  }

  public long getContentLength() {
    return mContentLength;
  }

  public String getDescription() {
    return mDescription;
  }

  public File getDestFile() throws IOException {
    return FileUtils.getFile(getDestUri());
  }

  public Uri getDestUri() {
    return mDestUri;
  }

  public long getId() {
    return mId;
  }

  public int getMaxRetry() {
    return mMaxRetry;
  }

  public String getMd5() {
    return mMd5;
  }

  public int getRetryCount() {
    return mRetryCount;
  }

  public State getState() {
    return mState;
  }

  public String getTag() {
    return mTag;
  }

  public File getTempFile() throws IOException {
    return FileUtils.getFile(getTempUri());
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

  public boolean isActive() {
    return getState() == State.ACTIVE;
  }

  public boolean isForced() {
    return mMaxRetry < 0;
  }

  public ContentValues toContentValues() {
    ContentValues values = new ContentValues();
    values.put(Fields.ID, mId);
    values.put(Fields.TAG, mTag);
    values.put(Fields.URI, mUri.toString());
    values.put(Fields.TITLE, mTitle);
    values.put(Fields.DESCRIPTION, mDescription);
    values.put(Fields.DEST_URI, mDestUri.toString());
    values.put(Fields.TEMP_URI, mTempUri.toString());
    values.put(Fields.MAX_RETRY, mMaxRetry);
    values.put(Fields.RETRY_COUNT, mRetryCount);
    values.put(Fields.STATE, mState.toInt());
    values.put(Fields.CONTENT_LENGTH, mContentLength);
    values.put(Fields.MD5, mMd5);
    return values;
  }

  public enum State {
    ACTIVE(1),
    IN_ACTIVE(2),
    OUT_OF_RETRY_COUNT(3),
    SUCCESS(4);

    private static SparseArray<State> sCaches = new SparseArray<>();

    static {
      for (State value : State.values()) {
        sCaches.put(value.toInt(), value);
      }
    }

    public static State from(int value) {
      return sCaches.get(value, IN_ACTIVE);
    }

    private final int mValue;

    State(int value) {
      mValue = value;
    }

    public int toInt() {
      return mValue;
    }
  }

  interface Fields {

    String CONTENT_LENGTH = "content_length";
    String DESCRIPTION = "description";
    String DEST_URI = "dest_uri";
    String ID = "_id";
    String MAX_RETRY = "max_retry";
    String MD5 = "md5";
    String RETRY_COUNT = "retry_count";
    String STATE = "state";
    String TAG = "tag";
    String TEMP_URI = "temp_uri";
    String TITLE = "title";
    String URI = "uri";
  }
}
