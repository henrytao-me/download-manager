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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.Request;
import me.henrytao.downloadmanager.sample.App;
import me.henrytao.downloadmanager.sample.ui.base.BaseViewModel;
import me.henrytao.mvvmlifecycle.rx.UnsubscribeLifeCycle;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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
    mDownloadId = new Request.Builder(Uri.parse("http://download.mysquar.com.s3.amazonaws.com/apk/mychat/mychat.apk"))
        .setDestPath(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)))
        .setTempPath(Uri.fromFile(mContext.getCacheDir()))
        .build()
        .enqueue();
    manageSubscription("download-progress", DownloadManager.getInstance().observe(mDownloadId)
        .debounce(500, TimeUnit.MILLISECONDS)
        .map(info -> {
          int percentage = info.getContentLength() > 0 ? (int) ((100 * info.getBytesRead()) / info.getContentLength()) : 0;
          return String.format(Locale.US, "Progress %s | %d%%", info.getId(), percentage);
        })
        .distinctUntilChanged()
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(percentage -> {
          progress.set(percentage);
        }, Throwable::printStackTrace), UnsubscribeLifeCycle.DESTROY_VIEW);
  }

  public void onPauseClicked() {
    DownloadManager.getInstance().pause(mDownloadId);
  }

  public void onResumeClicked() {
    DownloadManager.getInstance().resume(mDownloadId);
  }
}
