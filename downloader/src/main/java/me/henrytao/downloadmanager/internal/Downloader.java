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

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.Request;

/**
 * Created by henrytao on 12/13/16.
 */

public final class Downloader {

  public Downloader(Context context) {
    JobManager.create(context.getApplicationContext()).addJobCreator(new Downloader.JobCreator());
  }

  public void enqueue(Request request) {
    Job.create(request.getId()).schedule();
  }

  private static final class Job extends com.evernote.android.job.Job {

    private static final String EXTRA_ID = "EXTRA_ID";

    private static final String TAG = "DOWNLOADER";

    private static JobRequest create(long id) {
      PersistableBundleCompat bundle = new PersistableBundleCompat();
      bundle.putLong(EXTRA_ID, id);
      return new JobRequest.Builder(TAG)
          .setExecutionWindow(TimeUnit.SECONDS.toMillis(2), TimeUnit.SECONDS.toMillis(3))
          .setBackoffCriteria(TimeUnit.SECONDS.toMillis(2), JobRequest.BackoffPolicy.LINEAR)
          .setPersisted(true)
          .setExtras(bundle)
          .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
          .setRequirementsEnforced(true)
          .build();
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
      try {
        DownloadManager.getInstance().download(params.getExtras().getLong(EXTRA_ID, 0));
      } catch (Exception e) {
        return Result.RESCHEDULE;
      }
      return Result.SUCCESS;
    }
  }

  private static final class JobCreator implements com.evernote.android.job.JobCreator {

    @Override
    public com.evernote.android.job.Job create(String tag) {
      switch (tag) {
        case Job.TAG:
          return new Job();
      }
      return null;
    }
  }
}
