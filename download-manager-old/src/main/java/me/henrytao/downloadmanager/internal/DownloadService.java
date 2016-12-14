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
import me.henrytao.downloadmanager.Info;
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
    mDownloadBus = DownloadBus.getInstance(this);
    mLogger = Logger.newInstance(DownloadService.class.getSimpleName(),
        DownloadManager.DEBUG ? Logger.LogLevel.VERBOSE : Logger.LogLevel.NONE);
  }

  public DownloadService() {
    this(DownloadService.class.getSimpleName());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mLogger.d("onCreate");
    mDownloadBus.initialize();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mLogger.d("onDestroy");
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    super.onTaskRemoved(rootIntent);
    mLogger.d("onTaskRemoved");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    final long id = intent != null ? intent.getLongExtra(EXTRA_DOWNLOAD_ID, 0) : 0;
    mLogger.d("onHandleIntent | %d", id);
    reset();
    DownloadInfo downloadInfo = DownloadDbHelper.getInstance(this).find(id);
    if (downloadInfo == null) {
      mDownloadBus.invalid(id);
    } else if (downloadInfo.getState() == DownloadInfo.State.DOWNLOADING) {
      try {
        mDownloader = Downloader.create(downloadInfo.getUrl(),
            downloadInfo.getDestPath(), downloadInfo.getDestTitle(),
            downloadInfo.getTempPath(), downloadInfo.getTempTitle(),
            (bytesRead, contentLength) -> onStartDownload(id, bytesRead, contentLength),
            (bytesRead, contentLength) -> onDownloading(id, bytesRead, contentLength),
            (contentLength) -> onDownloaded(id, contentLength)
        );
        mSubscription = mDownloadBus
            .observe(id, false)
            .filter(info -> info.state == Info.State.PAUSED)
            .subscribe(info -> interrupt(id), Throwable::printStackTrace);
        mDownloader.download();
      } catch (Exception exception) {
        onError(id, exception);
      } finally {
        reset();
      }
    }
  }

  private void interrupt(long id) {
    if (mDownloader != null) {
      mDownloader.interrupt();
    }
  }

  private boolean isInterrupted() {
    return mDownloader != null && mDownloader.isInterrupted();
  }

  private void onDownloaded(long id, long contentLength) {
    mLogger.d("onDownloaded: %d | %d", id, contentLength);
    mDownloadBus.downloaded(id, contentLength);
  }

  private void onDownloading(long id, long bytesRead, long contentLength) {
    if (isInterrupted()) {
      return;
    }
    int percentage = (int) ((100 * bytesRead) / contentLength);
    mLogger.v("onDownloading: %d/100", percentage);
    mDownloadBus.downloading(id, bytesRead, contentLength);
  }

  private void onError(long id, Throwable throwable) {
    mLogger.d("onError: %d", id);
    mDownloadBus.error(id, throwable);
    reschedule(id);
  }

  private void onStartDownload(long id, long bytesRead, long contentLength) {
    if (isInterrupted()) {
      return;
    }
    DownloadDbHelper.getInstance(this).updateContentLength(id, contentLength);
    mLogger.v("onStartDownload: %d of %d", bytesRead, contentLength);
    mDownloadBus.started(id, bytesRead, contentLength);
  }

  private void reschedule(long id) {
    Intent intent = new Intent(this, DownloadManagerService.class);
    startService(intent);
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