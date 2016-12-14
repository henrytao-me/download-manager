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

package me.henrytao.downloadmanager.sample.ui.home;

import android.content.Context;
import android.databinding.ObservableField;
import android.net.Uri;
import android.os.Environment;

import me.henrytao.downloadmanager.Request;
import me.henrytao.downloadmanager.sample.App;
import me.henrytao.downloadmanager.sample.ui.base.BaseViewModel;

/**
 * Created by henrytao on 7/1/16.
 */
public class HomeViewModel extends BaseViewModel {

  private final Context mContext;

  public ObservableField<String> progress = new ObservableField<>();

  private long mDownloadId;

  public HomeViewModel() {
    mContext = App.getInstance().getApplicationContext();
  }

  @Override
  public void onCreateView() {
    super.onCreateView();
  }

  public void onDownloadClicked() {
    new Request.Builder(Uri.parse("http://download.mysquar.com.s3.amazonaws.com/apk/mychat/mychat.apk"))
        .setDestPath(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)))
        .setTempPath(Uri.fromFile(mContext.getCacheDir()))
        .build()
        .enqueue();

    //if (mDownloadId == 0) {
    //  Request request = new Request("http://download.mysquar.com.s3.amazonaws.com/apk/mychat/mychat.apk")
    //      .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/")
    //      .setTitle("test.apk");
    //  mDownloadId = mDownloadManager.enqueue(request);
    //  showProgress(mDownloadId);
    //} else {
    //  mDownloadManager.resume(mDownloadId);
    //}
  }

  public void onPauseClicked() {
    //mDownloadManager.pause(mDownloadId);
  }

  public void onTest() {
    //JobRequest jobRequest = new JobRequest.Builder(DemoSyncJob.TAG)
    //    //.setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
    //    .setExecutionWindow(TimeUnit.SECONDS.toMillis(3), TimeUnit.SECONDS.toMillis(5))
    //    .setBackoffCriteria(TimeUnit.SECONDS.toMillis(5), JobRequest.BackoffPolicy.LINEAR)
    //    .setPersisted(true)
    //    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
    //    .setRequirementsEnforced(true)
    //    .build();
    //jobRequest.schedule();
  }

  private void showProgress(long downloadId) {
    //manageSubscription(mDownloadManager.observe(downloadId)
    //    .compose(RxUtils.distinctInfoUntilChanged(300))
    //    .compose(Transformer.applyComputationScheduler())
    //    .subscribe(info -> {
    //      int percentage = info.contentLength > 0 ? (int) ((100 * info.bytesRead) / info.contentLength) : 0;
    //      mLogger.d("Progress %s | %s | %d%%", downloadId, info.state, percentage);
    //    }, Throwable::printStackTrace), UnsubscribeLifeCycle.DESTROY_VIEW);
  }

  //public static class DemoJobCreator implements JobCreator {
  //
  //  @Override
  //  public Job create(String tag) {
  //    switch (tag) {
  //      case DemoSyncJob.TAG:
  //        return new DemoSyncJob();
  //    }
  //    return null;
  //  }
  //}
  //
  //public static class DemoSyncJob extends Job {
  //
  //  public static final String TAG = "DEMO_SYNC_JOB";
  //
  //  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
  //
  //  @NonNull
  //  @Override
  //  protected Result onRunJob(Params params) {
  //    try {
  //      ping().timeout(5, TimeUnit.SECONDS).toBlocking().first();
  //    } catch (Exception e) {
  //      e.printStackTrace();
  //      return Result.RESCHEDULE;
  //    }
  //    return Result.SUCCESS;
  //  }
  //
  //  private Observable<Void> ping() {
  //    return Observable.create(subscriber -> {
  //      DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("downloader").child("ping");
  //      ref.push().setValue(dateFormat.format(new Date()), (databaseError, databaseReference) -> {
  //        if (databaseError != null) {
  //          SubscriptionUtils.onError(subscriber, databaseError.toException());
  //        } else {
  //          SubscriptionUtils.onNextAndComplete(subscriber);
  //        }
  //      });
  //    });
  //  }
  //}
}
