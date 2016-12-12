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

package me.henrytao.downloadmanager.sample;

import com.evernote.android.job.JobManager;
import com.facebook.stetho.Stetho;

import android.app.Application;
import android.content.Context;

import me.henrytao.downloadmanager.DownloadManager;
import me.henrytao.downloadmanager.sample.ui.home.HomeViewModel;

/**
 * Created by henrytao on 6/17/16.
 */
public class App extends Application {

  private static Context sInstance;

  public static Context getInstance() {
    return sInstance;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    sInstance = this;

    Stetho.initializeWithDefaults(this);

    DownloadManager.DEBUG = true;

    JobManager.create(App.getInstance()).addJobCreator(new HomeViewModel.DemoJobCreator());
  }
}
