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

import me.henrytao.downloadmanager.DownloadManager;

/**
 * Created by henrytao on 7/25/16.
 */
public class DownloadInfo {

  public static final String TABLE_NAME = "download_info";

  public static DownloadInfo create(DownloadManager.Request request, Uri tempPath, String tempTitle) {
    if (request == null) {
      return null;
    }
    return new DownloadInfo(0, request.getUri().toString(), request.getDestinationUri().toString(), request.getTitle(), 0,
        tempPath.toString(), tempTitle, State.DOWNLOADING);
  }

  public static DownloadInfo create(Cursor cursor) {
    if (cursor == null || cursor.isClosed()) {
      return null;
    }
    if (cursor.isBeforeFirst() && !cursor.moveToFirst()) {
      return null;

    }
    return new DownloadInfo(
        cursor.getLong(cursor.getColumnIndex(Fields._ID)),
        cursor.getString(cursor.getColumnIndex(Fields.URL)),
        cursor.getString(cursor.getColumnIndex(Fields.DEST_PATH)),
        cursor.getString(cursor.getColumnIndex(Fields.DEST_TITLE)),
        cursor.getLong(cursor.getColumnIndex(Fields.CONTENT_LENGTH)),
        cursor.getString(cursor.getColumnIndex(Fields.TEMP_PATH)),
        cursor.getString(cursor.getColumnIndex(Fields.TEMP_TITLE)),
        State.fromInt(cursor.getInt(cursor.getColumnIndex(Fields.STATE))));
  }

  private long mContentLength;

  private String mDestPath;

  private String mDestTitle;

  private long mId;

  private State mState;

  private String mTempPath;

  private String mTempTitle;

  private String mUrl;

  protected DownloadInfo(long id, String url, String destPath, String destTitle, long contentLength, String tempPath, String tempTitle,
      State state) {
    mId = id;
    mUrl = url;
    mDestPath = destPath;
    mDestTitle = destTitle;
    mContentLength = contentLength;
    mTempPath = tempPath;
    mTempTitle = tempTitle;
    mState = state;
  }

  public long getContentLength() {
    return mContentLength;
  }

  public String getDestPath() {
    return mDestPath;
  }

  public String getDestTitle() {
    return mDestTitle;
  }

  public long getId() {
    return mId;
  }

  public State getState() {
    return mState;
  }

  public String getTempPath() {
    return mTempPath;
  }

  public String getTempTitle() {
    return mTempTitle;
  }

  public String getUrl() {
    return mUrl;
  }

  public ContentValues toContentValues() {
    ContentValues values = new ContentValues();
    if (mId > 0) {
      values.put(Fields._ID, mId);
    }
    values.put(Fields.URL, mUrl);
    values.put(Fields.DEST_PATH, mDestPath);
    values.put(Fields.DEST_TITLE, mDestTitle);
    values.put(Fields.CONTENT_LENGTH, mContentLength);
    values.put(Fields.TEMP_PATH, mTempPath);
    values.put(Fields.TEMP_TITLE, mTempTitle);
    values.put(Fields.STATE, mState.toInt());
    return values;
  }

  public enum State {
    DOWNLOADED(1),
    DOWNLOADING(2),
    PAUSED(3);

    public static State fromInt(int value) {
      if (value == DOWNLOADED.toInt()) {
        return DOWNLOADED;
      } else if (value == PAUSED.toInt()) {
        return PAUSED;
      }
      return DOWNLOADING;
    }

    private final int mValue;

    State(int value) {
      mValue = value;
    }

    public int toInt() {
      return mValue;
    }
  }

  public interface Fields {

    String CONTENT_LENGTH = "content_length";
    String DEST_PATH = "dest_path";
    String DEST_TITLE = "dest_title";
    String STATE = "state";
    String TEMP_PATH = "temp_path";
    String TEMP_TITLE = "temp_title";
    String URL = "url";
    String _ID = "_id";
  }
}
