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

import com.evernote.android.job.JobRequest;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

public class DownloadManager {

  private static final long DEFAULT_BACKOFF_IN_MILLISECONDS = 2000;

  private static final JobRequest.BackoffPolicy DEFAULT_BACKOFF_POLICY = JobRequest.BackoffPolicy.LINEAR;

  private static final int DEFAULT_BUFFER_SIZE = 2048;

  private static final long DEFAULT_EXECUTION_WINDOW_END_IN_MILLISECONDS = 3000;

  private static final long DEFAULT_EXECUTION_WINDOW_START_IN_MILLISECONDS = 2000;

  private static final boolean DEFAULT_PERSISTED = true;

  public static boolean DEBUG = false;

  private static volatile DownloadManager sInstance;

  public static DownloadManager create(@NonNull Context context, @NonNull Config config) {
    if (sInstance == null) {
      synchronized (DownloadManager.class) {
        if (sInstance == null) {
          sInstance = new DownloadManager(context, config);
        }
      }
    }
    return sInstance;
  }

  public static DownloadManager create(@NonNull Context context) {
    return create(context, new Config.Builder().build());
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

  private final Config mConfig;

  private final Downloader mDownloader;

  private final JobService mJobService;

  private final Logger mLogger;

  private final Storage mStorage;

  private DownloadManager(Context context, Config config) {
    context = context.getApplicationContext();
    mConfig = config;
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

  public Config getConfig() {
    return mConfig;
  }

  public Logger getLogger() {
    return mLogger;
  }

  public Observable<Boolean> isEnqueued(String tag) {
    return Observable.fromCallable(() -> mStorage.find(tag) != null);
  }

  public Observable<Info> observe(String tag) {
    return Observable.fromCallable(() -> mStorage.find(tag))
        .flatMap(task -> {
          if (task == null) {
            return Observable.error(new NullPointerException("Task with tag %s not found"));
          }
          return observe(task.getId());
        });
  }

  public Observable<Info> observe(long id) {
    return mBus.observe(id);
  }

  public void pause(long id) {
    mJobService.stop(id);
    mStorage.update(id, Task.State.IN_ACTIVE);
    mBus.pausing(id);
  }

  public void pause(String tag) {
    List<Task> tasks = mStorage.findAll(tag);
    for (Task task : tasks) {
      pause(task.getId());
    }
  }

  public void resume(long id) {
    mJobService.stop(id);
    mStorage.update(id, Task.State.ACTIVE);
    mJobService.enqueue(id);
    mBus.queueing(id);
  }

  public void resume(String tag) {
    Task task = mStorage.find(tag);
    if (task != null) {
      resume(task.getId());
    }
  }

  public static class Config {

    public final long backoffInMilliseconds;

    public final JobRequest.BackoffPolicy backoffPolicy;

    public final int bufferSize;

    public final long executionWindowEndInMilliseconds;

    public final long executionWindowStartInMilliseconds;

    @NonNull
    public final List<Interceptor> interceptors;

    public final boolean persisted;

    private Config(long executionWindowStartInMilliseconds, long executionWindowEndInMilliseconds, long backoffInMilliseconds,
        JobRequest.BackoffPolicy backoffPolicy, boolean persisted, int bufferSize, @NonNull List<Interceptor> interceptors) {
      this.executionWindowStartInMilliseconds = executionWindowStartInMilliseconds;
      this.executionWindowEndInMilliseconds = executionWindowEndInMilliseconds;
      this.backoffInMilliseconds = backoffInMilliseconds;
      this.backoffPolicy = backoffPolicy;
      this.persisted = persisted;
      this.bufferSize = bufferSize;
      this.interceptors = interceptors;
    }

    public static class Builder {

      private final List<Interceptor> mInterceptors;

      private long mBackoffInMilliseconds;

      private JobRequest.BackoffPolicy mBackoffPolicy;

      private int mBufferSize;

      private long mExecutionWindowEndInMilliseconds;

      private long mExecutionWindowStartInMilliseconds;

      private boolean mPersisted;

      public Builder() {
        mExecutionWindowStartInMilliseconds = DEFAULT_EXECUTION_WINDOW_START_IN_MILLISECONDS;
        mExecutionWindowEndInMilliseconds = DEFAULT_EXECUTION_WINDOW_END_IN_MILLISECONDS;
        mBackoffInMilliseconds = DEFAULT_BACKOFF_IN_MILLISECONDS;
        mBackoffPolicy = DEFAULT_BACKOFF_POLICY;
        mPersisted = DEFAULT_PERSISTED;
        mBufferSize = DEFAULT_BUFFER_SIZE;
        mInterceptors = new ArrayList<>();
      }

      public Builder addInterceptor(Interceptor interceptor) {
        mInterceptors.add(interceptor);
        return this;
      }

      public Config build() {
        return new Config(mExecutionWindowStartInMilliseconds, mExecutionWindowEndInMilliseconds, mBackoffInMilliseconds, mBackoffPolicy,
            mPersisted, mBufferSize, mInterceptors);
      }

      public Builder setBackoffInMilliseconds(long backoffInMilliseconds) {
        mBackoffInMilliseconds = backoffInMilliseconds;
        return this;
      }

      public Builder setBackoffPolicy(JobRequest.BackoffPolicy backoffPolicy) {
        mBackoffPolicy = backoffPolicy;
        return this;
      }

      public Builder setBufferSize(int bufferSize) {
        mBufferSize = bufferSize;
        return this;
      }

      public Builder setExecutionWindowEndInMilliseconds(long executionWindowEndInMilliseconds) {
        mExecutionWindowEndInMilliseconds = executionWindowEndInMilliseconds;
        return this;
      }

      public Builder setExecutionWindowStartInMilliseconds(long executionWindowStartInMilliseconds) {
        mExecutionWindowStartInMilliseconds = executionWindowStartInMilliseconds;
        return this;
      }

      public Builder setPersisted(boolean persisted) {
        mPersisted = persisted;
        return this;
      }
    }
  }
}
