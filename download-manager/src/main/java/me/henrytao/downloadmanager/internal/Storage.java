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

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;

import java.util.concurrent.atomic.AtomicLong;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.Request;

/**
 * Created by henrytao on 12/12/16.
 */

public class Storage {

  private static final int CACHE_SIZE = 30;

  private static final String DATABASE_NAME = "download-manager.db";

  private static final int DATABASE_VERSION = 1;

  private static final String PREFERENCE_NAME = "download_manager";

  private static final String PREF_TASK_COUNTER = "PREF_TASK_COUNTER";

  private static Logger log() {
    return DownloadManager.getInstance().getLogger();
  }

  private final DbHelper mDbHelper;

  private final SharedPreferences mPreferences;

  private final TaskCache mTaskCache;

  private final AtomicLong mTaskCounter;

  public Storage(Context context) {
    context = context.getApplicationContext();
    mDbHelper = new DbHelper(context);
    mPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    mTaskCounter = new AtomicLong(mPreferences.getLong(PREF_TASK_COUNTER, 0));
    mTaskCache = new TaskCache(mDbHelper);
  }

  public synchronized void enqueue(Request request) {
    Task task = Task.create(request);
    if (mDbHelper.insert(task)) {
      addToCache(task);
    }
  }

  public synchronized Task find(long id) {
    return mTaskCache.get(id);
  }

  @SuppressLint("CommitPrefEdits")
  public synchronized long getNextTaskId() {
    long id = mTaskCounter.incrementAndGet();
    if (id < 0) {
      /*
       * An overflow occurred. It'll happen rarely, but just in case reset the ID and start from scratch.
       * Existing jobs will be treated as orphaned and will be overwritten.
       */
      id = 1;
      mTaskCounter.set(id);
    }
    mPreferences.edit().putLong(PREF_TASK_COUNTER, id).commit();
    return id;
  }

  public synchronized void increaseRetryCount(long id) {
    if (mDbHelper.increaseRetryCount(id)) {
      updateCache(id);
    }
  }

  public void update(long id, long contentLength, String md5) {
    if (mDbHelper.update(id, contentLength, md5)) {
      updateCache(id);
    }
  }

  public void update(long id, Task.State state) {
    if (mDbHelper.update(id, state)) {
      updateCache(id);
    }
  }

  private void addToCache(@NonNull Task task) {
    mTaskCache.put(task.getId(), task);
  }

  private void updateCache(long id) {
    mTaskCache.remove(id);
    addToCache(mDbHelper.find(id));
  }

  private static class DbHelper extends SQLiteOpenHelper {

    private static final String C_COMMA = " , ";

    private static final String C_INTEGER = " INTEGER ";

    private static final String C_TEXT = " TEXT ";

    private SQLiteDatabase mDb;

    DbHelper(Context context) {
      super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE IF NOT EXISTS " + Task.NAME + " ( "
          + Task.Fields.ID + C_INTEGER + " PRIMARY KEY " + C_COMMA
          + Task.Fields.TAG + C_TEXT + C_COMMA
          + Task.Fields.URI + C_TEXT + C_COMMA
          + Task.Fields.TITLE + C_TEXT + C_COMMA
          + Task.Fields.DESCRIPTION + C_TEXT + C_COMMA
          + Task.Fields.DEST_URI + C_TEXT + C_COMMA
          + Task.Fields.TEMP_URI + C_TEXT + C_COMMA
          + Task.Fields.MAX_RETRY + C_INTEGER + C_COMMA
          + Task.Fields.RETRY_COUNT + C_INTEGER + C_COMMA
          + Task.Fields.STATE + C_INTEGER + C_COMMA
          + Task.Fields.CONTENT_LENGTH + C_INTEGER + C_COMMA
          + Task.Fields.MD5 + C_TEXT
          + " )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
      // do nothing
    }

    Task find(long id) {
      Task task = null;
      Cursor cursor = null;
      try {
        cursor = db().query(Task.NAME, null, Task.Fields.ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        task = Task.create(cursor);
      } catch (Exception e) {
        log().e(e, "Could not find id %d", id);
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
      return task;
    }

    boolean increaseRetryCount(long id) {
      try {
        Task task = find(id);
        ContentValues values = new ContentValues();
        if (!task.isForced() && task.getRetryCount() >= task.getMaxRetry()) {
          if (task.getState() != Task.State.OUT_OF_RETRY_COUNT) {
            values.put(Task.Fields.STATE, Task.State.OUT_OF_RETRY_COUNT.toInt());
          }
        } else {
          values.put(Task.Fields.RETRY_COUNT, task.getRetryCount() + 1);
        }
        if (values.size() == 0) {
          return false;
        }
        db().update(Task.NAME, values, Task.Fields.ID + " = ?", new String[]{String.valueOf(id)});
      } catch (Exception e) {
        log().e(e, "Could not increase retry count of id %d", id);
        return false;
      }
      return true;
    }

    boolean insert(Task task) {
      try {
        remove(task.getId());
        db().insert(Task.NAME, null, task.toContentValues());
      } catch (Exception e) {
        log().e(e, "Could not insert task %s", task);
        return false;
      }
      return true;
    }

    boolean remove(long id) {
      try {
        db().delete(Task.NAME, Task.Fields.ID + " = ?", new String[]{String.valueOf(id)});
      } catch (Exception e) {
        log().e(e, "Could not delete task %d", id);
        return false;
      }
      return true;
    }

    boolean update(long id, Task.State state) {
      try {
        ContentValues values = new ContentValues();
        values.put(Task.Fields.STATE, state.toInt());
        db().update(Task.NAME, values, Task.Fields.ID + " = ?", new String[]{String.valueOf(id)});
      } catch (Exception e) {
        log().e(e, "Could not update state of id %d", id);
        return false;
      }
      return true;
    }

    boolean update(long id, long contentLength, String md5) {
      try {
        ContentValues values = new ContentValues();
        values.put(Task.Fields.CONTENT_LENGTH, contentLength);
        values.put(Task.Fields.MD5, md5);
        db().update(Task.NAME, values, Task.Fields.ID + " = ?", new String[]{String.valueOf(id)});
      } catch (Exception e) {
        log().e(e, "Could not update contentLength and md5 of id %d", id);
        return false;
      }
      return true;
    }

    private SQLiteDatabase db() {
      if (mDb == null) {
        synchronized (DbHelper.class) {
          if (mDb == null) {
            mDb = getWritableDatabase();
          }
        }
      }
      return mDb;
    }
  }

  private static class TaskCache extends LruCache<Long, Task> {

    private final DbHelper mDbHelper;

    TaskCache(DbHelper dbHelper) {
      super(CACHE_SIZE);
      mDbHelper = dbHelper;
    }

    @Override
    protected Task create(Long key) {
      return mDbHelper.find(key);
    }
  }
}
