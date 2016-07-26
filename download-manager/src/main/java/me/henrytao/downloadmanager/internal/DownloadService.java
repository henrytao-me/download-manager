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

import android.app.IntentService;
import android.content.Intent;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.data.DownloadInfo;
import me.henrytao.downloadmanager.utils.Logger;

/**
 * Created by henrytao on 7/25/16.
 */
public class DownloadService extends IntentService {

  public static final String EXTRA_DOWNLOAD_ID = "EXTRA_DOWNLOAD_ID";

  private final Logger mLogger;

  protected DownloadService(String name) {
    super(name);
    setIntentRedelivery(true);
    mLogger = Logger.newInstance(DownloadManager.DEBUG ? Logger.LogLevel.VERBOSE : Logger.LogLevel.NONE);
    mLogger.d("onHandleIntent | initialized");
  }

  public DownloadService() {
    this(DownloadService.class.getSimpleName());
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    long id = intent.getLongExtra(EXTRA_DOWNLOAD_ID, 0);
    DownloadInfo downloadInfo = DownloadDbHelper.create(this).find(id);
    if (downloadInfo != null) {
      try {
        Downloader.create(downloadInfo.getUrl(), downloadInfo.getDestPath(), downloadInfo.getDestTitle(), this::onDownloading).download();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void onDownloading(long bytesRead, long contentLength, boolean done) {
    int percentage = (int) ((100 * bytesRead) / contentLength);
    mLogger.d("Progress: %d%% done", percentage);
  }
}
