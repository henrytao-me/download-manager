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
import java.util.Locale;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.Info;
import me.henrytao.downloadmanager.utils.FileUtils;
import me.henrytao.downloadmanager.utils.Logger;
import rx.Subscription;

/**
 * Created by henrytao on 7/25/16.
 */
public class DownloadService extends IntentService {

  public static final String EXTRA_DOWNLOAD_ID = "EXTRA_DOWNLOAD_ID";

  private final DownloadBus mDownloadBus;

  private final Logger mLogger;

  private Downloader mDownloader;

  private Subscription mSubscription;

  protected DownloadService(String name) {
    super(name);
    setIntentRedelivery(true);
    mDownloadBus = DownloadBus.getInstance();
    mLogger = Logger.newInstance(DownloadManager.DEBUG ? Logger.LogLevel.VERBOSE : Logger.LogLevel.NONE);
    //mLogger.d("onHandleIntent | initialized");
  }

  public DownloadService() {
    this(DownloadService.class.getSimpleName());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    log("onCreate");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    final long id = intent.getLongExtra(EXTRA_DOWNLOAD_ID, 0);
    log("onHandleIntent | %d", id);
    reset();
    DownloadInfo downloadInfo = DownloadDbHelper.create(this).find(id);
    if (downloadInfo != null) {
      File destFile = FileUtils.getFile(downloadInfo.getDestPath(), downloadInfo.getDestTitle());
      long destContentLength = destFile.length();
      if (destFile.exists() && downloadInfo.getContentLength() == destContentLength) {
        FileUtils.delete(FileUtils.getFile(downloadInfo.getTempPath(), downloadInfo.getTempTitle()));
        onDownloaded(id, destContentLength);
      } else {
        mSubscription = mDownloadBus
            .observe(id)
            .filter(info -> info.state == Info.State.PAUSED)
            .subscribe(info -> onPaused(id), throwable -> onError(id, throwable));
        mDownloader = Downloader.create(downloadInfo.getUrl(),
            downloadInfo.getDestPath(), downloadInfo.getDestTitle(),
            downloadInfo.getTempPath(), downloadInfo.getTempTitle(),
            (bytesRead, contentLength) -> onStartDownload(id, bytesRead, contentLength),
            (bytesRead, contentLength, done) -> onDownloading(id, bytesRead, contentLength, done),
            (contentLength) -> onDownloaded(id, contentLength)
        );
        try {
          mDownloader.download();
        } catch (Exception exception) {
          onError(id, exception);
        } finally {
          reset();
        }
      }
    } else {
      mDownloadBus.invalid(id);
    }
  }

  private boolean isPaused() {
    return mDownloader != null && mDownloader.isInterrupted();
  }

  private void log(String value, Object... args) {
    mLogger.d(String.format(Locale.US, "%s | %s", DownloadService.class.getSimpleName(), String.format(Locale.US, value, args)));
  }

  private void onDownloaded(long id, long contentLength) {
    if (isPaused()) {
      return;
    }
    mDownloadBus.downloaded(id, contentLength);
  }

  private void onDownloading(long id, long bytesRead, long contentLength, boolean done) {
    if (isPaused()) {
      return;
    }
    int percentage = (int) ((100 * bytesRead) / contentLength);
    log("onDownloading: %d/100", percentage);
    mDownloadBus.downloading(id, bytesRead, contentLength);
  }

  private void onError(long id, Throwable throwable) {
    mDownloadBus.error(id, throwable);
  }

  private void onPaused(long id) {
    if (mDownloader != null) {
      mDownloader.interrupt();
    }
  }

  private void onStartDownload(long id, long bytesRead, long contentLength) {
    if (isPaused()) {
      return;
    }
    DownloadDbHelper.create(this).updateContentLength(id, contentLength);
    log("onStartDownload: %d of %d", bytesRead, contentLength);
    mDownloadBus.started(id, bytesRead, contentLength);
  }

  private void reset() {
    if (mSubscription != null && !mSubscription.isUnsubscribed()) {
      mSubscription.unsubscribe();
    }
    mSubscription = null;
    if (mDownloader != null) {
      mDownloader.close();
    }
    mDownloader = null;
  }
}
