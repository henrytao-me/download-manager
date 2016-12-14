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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.utils.Logger;

/**
 * Created by henrytao on 8/4/16.
 */

public class AutoStartBroadcastReceiver extends BroadcastReceiver {

  private final Logger mLogger;

  public AutoStartBroadcastReceiver() {
    mLogger = Logger.newInstance(AutoStartBroadcastReceiver.class.getSimpleName(),
        DownloadManager.DEBUG ? Logger.LogLevel.VERBOSE : Logger.LogLevel.NONE);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    mLogger.d("onReceive");
    DownloadManager.getInstance(context).initialize();
  }
}
