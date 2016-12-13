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

import me.henrytao.downloadmanager.Request;

/**
 * Created by henrytao on 7/25/16.
 */
public final class Task {

  static final String NAME = "task";

  public static Task create(Cursor cursor) {
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
        cursor.getLong(cursor.getColumnIndex(Fields.CONTENT_LENGTH))
    );
  }

  static Task create(Request request) {
    return new Task(request.getId(), request.getTag(), request.getUri(), request.getTitle(), request.getDescription(), request.getDestUri(),
        request.getTempUri(), request.getRetry(), 0, State.ACTIVE, 0);
  }

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

  private Task(long id, String tag, Uri uri, String title, String description, Uri destUri, Uri tempUri, int maxRetry, int retryCount,
      State state, long contentLength) {
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
  }

  public long getContentLength() {
    return mContentLength;
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

  public int getMaxRetry() {
    return mMaxRetry;
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

  public Uri getTempUri() {
    return mTempUri;
  }

  public String getTitle() {
    return mTitle;
  }

  public Uri getUri() {
    return mUri;
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
    return values;
  }

  public enum State {
    ACTIVE(1),
    IN_ACTIVE(2),
    ERROR(3);

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
    String RETRY_COUNT = "retry_count";
    String STATE = "state";
    String TAG = "tag";
    String TEMP_URI = "temp_uri";
    String TITLE = "title";
    String URI = "uri";
  }
}
