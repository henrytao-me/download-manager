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
import android.os.Environment;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.DownloadManager.Request;
import me.henrytao.downloadmanager.sample.App;
import me.henrytao.downloadmanager.sample.ui.base.BaseViewModel;

/**
 * Created by henrytao on 7/1/16.
 */
public class HomeViewModel extends BaseViewModel {

  private final Context mContext;

  private long mDownloadId;

  private DownloadManager mDownloadManager;

  public HomeViewModel() {
    mContext = App.getInstance();
  }

  @Override
  public void onCreateView() {
    super.onCreateView();
    mDownloadManager = DownloadManager.getInstance(mContext);
  }

  public void onDownloadClicked() {
    if (mDownloadId == 0) {
      Request request = new Request("http://download.mysquar.com.s3.amazonaws.com/apk/mychat/mychat.apk")
          .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/")
          .setTitle("test.apk");
      mDownloadId = mDownloadManager.enqueue(request);
    } else {
      mDownloadManager.enqueue(mDownloadId);
    }
    showProgress(mDownloadId);
  }

  private void showProgress(long downloadId) {
    //manageSubscription();
  }
}
