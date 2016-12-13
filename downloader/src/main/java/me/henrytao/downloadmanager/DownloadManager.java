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

import android.annotation.SuppressLint;
import android.content.Context;

import me.henrytao.downloadmanager.internal.Logger;
import me.henrytao.downloadmanager.internal.Storage;

/**
 * Created by henrytao on 12/12/16.
 */

public final class DownloadManager {

  public static boolean DEBUG = false;

  @SuppressLint("StaticFieldLeak")
  private static volatile DownloadManager sInstance;

  public static DownloadManager create(Context context) {
    if (sInstance == null) {
      synchronized (DownloadManager.class) {
        if (sInstance == null) {
          sInstance = new DownloadManager(context);
        }
      }
    }
    return sInstance;
  }

  public static DownloadManager getInstance() {
    if (sInstance == null) {
      synchronized (DownloadManager.class) {
        if (sInstance == null) {
          throw new IllegalStateException("You need to call create() at least once to create the singleton");
        }
      }
    }
    return sInstance;
  }

  private final Context mContext;

  private final Logger mLogger;

  private final Storage mStorage;

  private DownloadManager(Context context) {
    mContext = context.getApplicationContext();
    mLogger = Logger.newInstance(getClass().getSimpleName(), DEBUG ? Logger.LogLevel.VERBOSE : Logger.LogLevel.NONE);
    mStorage = new Storage(mContext);
  }

  public long enqueue(Request request) {
    if (request.isEnqueued()) {
      return request.getId();
    }
    long id = mStorage.getNextTaskId();
    request.setId(id);
    mStorage.enqueue(request);
    return id;
  }

  public Logger getLogger() {
    return mLogger;
  }

  public Storage getStorage() {
    return mStorage;
  }
}
