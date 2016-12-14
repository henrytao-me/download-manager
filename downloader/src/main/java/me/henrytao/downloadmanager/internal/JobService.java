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

import java.util.Set;

import me.henrytao.downloadmanager.DownloadManager;

/**
 * Created by henrytao on 12/13/16.
 */

public final class JobService {

  private static DownloadManager.Config getConfig() {
    return DownloadManager.getInstance().getConfig();
  }

  public JobService(Context context) {
    JobManager.create(context.getApplicationContext()).addJobCreator(new JobService.JobCreator());
  }

  public void enqueue(long taskId) {
    Job.create(taskId).schedule();
  }

  public void stop(long taskId) {
    Set<JobRequest> requests = JobManager.instance().getAllJobRequestsForTag(Job.TAG);
    for (JobRequest request : requests) {
      if (request.getExtras().getLong(Job.EXTRA_TASK_ID, 0) == taskId) {
        JobManager.instance().cancel(request.getJobId());
      }
    }
  }

  private static final class Job extends com.evernote.android.job.Job {

    private static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";

    private static final String TAG = "DOWNLOADER";

    private static JobRequest create(long taskId) {
      PersistableBundleCompat bundle = new PersistableBundleCompat();
      bundle.putLong(EXTRA_TASK_ID, taskId);
      return new JobRequest.Builder(TAG)
          .setExecutionWindow(getConfig().executionWindowStartInMilliseconds, getConfig().executionWindowEndInMilliseconds)
          .setBackoffCriteria(getConfig().backoffInMilliseconds, getConfig().backoffPolicy)
          .setPersisted(getConfig().persisted)
          .setExtras(bundle)
          .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
          .setRequirementsEnforced(true)
          .build();
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
      try {
        DownloadManager.getInstance().download(params.getExtras().getLong(EXTRA_TASK_ID, 0));
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
