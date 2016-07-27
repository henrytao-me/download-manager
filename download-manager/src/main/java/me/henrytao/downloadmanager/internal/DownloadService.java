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

import java.io.File;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.utils.FileUtils;
import me.henrytao.downloadmanager.utils.Logger;

/**
 * Created by henrytao on 7/25/16.
 */
public class DownloadService extends IntentService {

  public static final String EXTRA_DOWNLOAD_ID = "EXTRA_DOWNLOAD_ID";

  private final DownloadBus mDownloadBus;

  private final Logger mLogger;

  private Downloader mDownloader;

  protected DownloadService(String name) {
    super(name);
    setIntentRedelivery(true);
    mDownloadBus = DownloadBus.getInstance();
    mLogger = Logger.newInstance(DownloadManager.DEBUG ? Logger.LogLevel.NONE : Logger.LogLevel.NONE);
    mLogger.d("onHandleIntent | initialized");
  }

  public DownloadService() {
    this(DownloadService.class.getSimpleName());
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    mDownloader = null;
    final long id = intent.getLongExtra(EXTRA_DOWNLOAD_ID, 0);
    DownloadInfo downloadInfo = DownloadDbHelper.create(this).find(id);
    if (downloadInfo != null) {
      File destFile = FileUtils.getFile(downloadInfo.getDestPath(), downloadInfo.getDestTitle());
      long destContentLength = destFile.length();
      if (destFile.exists() && downloadInfo.getContentLength() == destContentLength) {
        onDownloaded(id, destContentLength);
      } else {
        mDownloader = Downloader.create(downloadInfo.getUrl(),
            downloadInfo.getDestPath(), downloadInfo.getDestTitle(),
            downloadInfo.getTempPath(), downloadInfo.getTempTitle(),
            (bytesRead, contentLength) -> onStartDownload(id, bytesRead, contentLength),
            (bytesRead, contentLength, done) -> onDownloading(id, bytesRead, contentLength, done),
            (contentLength) -> onDownloaded(id, contentLength)
        );
        try {
          mDownloader.download();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          mDownloader.close();
        }
      }
    } else {
      mDownloadBus.invalid(id);
    }
  }

  private void onDownloaded(long id, long contentLength) {
    mLogger.d("onDownloaded");
    mDownloadBus.downloaded(id, contentLength);
  }

  private void onDownloading(long id, long bytesRead, long contentLength, boolean done) {
    int percentage = (int) ((100 * bytesRead) / contentLength);
    mLogger.d("onDownloading: %d%%", percentage);
    mDownloadBus.downloading(id, bytesRead, contentLength);
  }

  private void onStartDownload(long id, long bytesRead, long contentLength) {
    DownloadDbHelper.create(this).updateContentLength(id, contentLength);
    mLogger.d("onStartDownload: %d of %d", bytesRead, contentLength);
    mDownloadBus.started(id, bytesRead, contentLength);
  }
}
