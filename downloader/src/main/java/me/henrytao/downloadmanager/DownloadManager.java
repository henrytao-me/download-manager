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

import java.io.IOException;

import me.henrytao.downloadmanager.internal.Bus;
import me.henrytao.downloadmanager.internal.Downloader;
import me.henrytao.downloadmanager.internal.JobService;
import me.henrytao.downloadmanager.internal.Logger;
import me.henrytao.downloadmanager.internal.Storage;
import me.henrytao.downloadmanager.internal.Task;
import rx.Observable;

/**
 * Created by henrytao on 12/12/16.
 */

public final class DownloadManager {

  public static boolean DEBUG = false;

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

  private final Bus mBus;

  private final Downloader mDownloader;

  private final JobService mJobService;

  private final Logger mLogger;

  private final Storage mStorage;

  private DownloadManager(Context context) {
    context = context.getApplicationContext();
    mLogger = Logger.newInstance(getClass().getSimpleName(), DEBUG ? Logger.LogLevel.VERBOSE : Logger.LogLevel.NONE);
    mJobService = new JobService(context);
    mStorage = new Storage(context);
    mBus = new Bus(mStorage);
    mDownloader = new Downloader(mStorage, mBus);
  }

  public void download(long id) throws IOException {
    mDownloader.download(mStorage.find(id));
  }

  public long enqueue(Request request) {
    if (request.isEnqueued()) {
      return request.getId();
    }
    request.setId(mStorage.getNextTaskId());
    mStorage.enqueue(request);
    mJobService.enqueue(request.getId());
    return request.getId();
  }

  public Logger getLogger() {
    return mLogger;
  }

  public Observable<Info> observe(long id) {
    return mBus.observe(id);
  }

  public void pause(long id) {
    mJobService.stop(id);
    mStorage.update(id, Task.State.IN_ACTIVE);
  }

  public void resume(long id) {
    mJobService.stop(id);
    mStorage.update(id, Task.State.ACTIVE);
    mJobService.enqueue(id);
  }
}
