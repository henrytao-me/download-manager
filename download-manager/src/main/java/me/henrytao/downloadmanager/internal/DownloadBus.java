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

import java.util.HashMap;
import java.util.Map;

import me.henrytao.downloadmanager.Info;
import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Created by henrytao on 7/27/16.
 */
public class DownloadBus {

  private static DownloadBus sDefault;

  public static DownloadBus getInstance() {
    if (sDefault == null) {
      synchronized (DownloadBus.class) {
        if (sDefault == null) {
          sDefault = new DownloadBus();
        }
      }
    }
    return sDefault;
  }

  private final Map<Long, BehaviorSubject<Info>> maps = new HashMap<>();

  public void downloaded(long id, long contentLength) {
    get(id).onNext(new Info(Info.State.DOWNLOADED, contentLength, contentLength));
  }

  public void downloading(long id, long bytesRead, long contentLength) {
    get(id).onNext(new Info(Info.State.DOWNLOADING, bytesRead, contentLength));
  }

  public void enqueue(long id) {
    get(id).onNext(new Info(Info.State.QUEUEING, 0, 0));
  }

  public void error(long id, Throwable throwable) {
    get(id).onNext(new Info(Info.State.ERROR, throwable));
  }

  public boolean exist(long id) {
    return maps.containsKey(id);
  }

  public void invalid(long id) {
    get(id).onNext(new Info(Info.State.INVALID, 0, 0));
  }

  public Observable<Info> observe(long id) {
    return Observable.just((Info) null)
        .mergeWith(get(id))
        .filter(info -> info != null)
        .flatMap(info -> {
          if (info.state == Info.State.ERROR) {
            return Observable.error(info.getThrowable());
          }
          return Observable.just(info);
        });
  }

  public void pause(long id) {
    get(id).onNext(new Info(Info.State.PAUSED, 0, 0));
  }

  public void resume(long id) {
    get(id).onNext(new Info(Info.State.RESUMED, 0, 0));
  }

  public void started(long id, long bytesRead, long contentLength) {
    get(id).onNext(new Info(Info.State.STARTED, bytesRead, contentLength));
  }

  private BehaviorSubject<Info> get(long id) {
    if (!maps.containsKey(id)) {
      maps.put(id, BehaviorSubject.create());
    }
    return maps.get(id);
  }
}
