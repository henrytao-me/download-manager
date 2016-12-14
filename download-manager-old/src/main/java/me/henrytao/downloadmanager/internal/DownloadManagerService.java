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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.concurrent.TimeUnit;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.config.Constants;
import me.henrytao.downloadmanager.utils.ConnectivityUtils;
import me.henrytao.downloadmanager.utils.Logger;
import me.henrytao.downloadmanager.utils.ServiceUtils;
import me.henrytao.downloadmanager.utils.rx.SubscriptionUtils;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

/**
 * Created by henrytao on 7/25/16.
 */
public class DownloadManagerService extends Service {

  private Logger mLogger;

  private Subscription mSubscription;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mLogger = Logger.newInstance(DownloadManagerService.class.getSimpleName(),
        DownloadManager.DEBUG ? Logger.LogLevel.VERBOSE : Logger.LogLevel.NONE);
    mLogger.d("onCreate");

    mSubscription = Observable
        .interval(Constants.Retry.INTERVAL, TimeUnit.MILLISECONDS)
        .subscribeOn(Schedulers.computation())
        .subscribe(l -> {
          boolean isConnected = ConnectivityUtils.isConnected(this);
          boolean isRunning = ServiceUtils.isRunning(this, DownloadService.class);
          mLogger.d("isConnected: %b | isDownloadServiceRunning: %b", isConnected, isRunning);
          if (isConnected && !isRunning) {
            DownloadManager.getInstance(this).initialize();
            stopSelf();
          }
        }, Throwable::printStackTrace);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mLogger.d("onDestroy");
    SubscriptionUtils.unsubscribe(mSubscription);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    mLogger.d("onStartCommand");
    return START_STICKY;
  }
}
