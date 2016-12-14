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

import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;

import java.util.Locale;

import me.henrytao.downloadmanager.Info;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by henrytao on 12/13/16.
 */

public class Bus {

  private final Storage mStorage;

  private LongSparseArray<PublishSubject<Info>> mSubject = new LongSparseArray<>();

  public Bus(Storage storage) {
    mStorage = storage;
  }

  public void downloaded(long id) {
    onNext(id, Info.create(mStorage.find(id), 0, Info.Status.DOWNLOADED));
  }

  public void downloading(long id, long bytesRead) {
    onNext(id, Info.create(mStorage.find(id), bytesRead, Info.Status.DOWNLOADING));
  }

  public void failed(long id) {
    onNext(id, Info.create(mStorage.find(id), 0, Info.Status.FAILED));
  }

  public Observable<Info> observe(long id) {
    return Observable.concat(
        Observable.create(subscriber -> {
          Task task = mStorage.find(id);
          if (task == null) {
            SubscriptionUtils.onError(subscriber, new IllegalArgumentException(String.format(Locale.US, "Could not found task %d", id)));
            return;
          }
          switch (task.getState()) {
            case ACTIVE:
              if (task.getBytesRead() == 0) {
                SubscriptionUtils.onNext(subscriber, Info.create(task, 0, Info.Status.QUEUEING));
              } else {
                SubscriptionUtils.onNext(subscriber, Info.create(task, 0, Info.Status.DOWNLOADING));
              }
              break;
            case IN_ACTIVE:
              SubscriptionUtils.onNext(subscriber, Info.create(task, 0, Info.Status.PAUSING));
              break;
            case OUT_OF_RETRY_COUNT:
              SubscriptionUtils.onNext(subscriber, Info.create(task, 0, Info.Status.FAILED));
              break;
            case SUCCESS:
              SubscriptionUtils.onNext(subscriber, Info.create(task, 0, Info.Status.SUCCEED));
              break;
          }
        }),
        get(id)
    );
  }

  public void pausing(long id) {
    onNext(id, Info.create(mStorage.find(id), 0, Info.Status.PAUSING));
  }

  public void queueing(long id) {
    onNext(id, Info.create(mStorage.find(id), 0, Info.Status.QUEUEING));
  }

  public void succeed(long id) {
    onNext(id, Info.create(mStorage.find(id), 0, Info.Status.SUCCEED));
  }

  public void validating(long id) {
    onNext(id, Info.create(mStorage.find(id), 0, Info.Status.VALIDATING));
  }

  @NonNull
  private PublishSubject<Info> get(long id) {
    PublishSubject<Info> subject = mSubject.get(id);
    if (subject == null) {
      subject = PublishSubject.create();
      mSubject.put(id, subject);
    }
    return subject;
  }

  private void onNext(long id, Info info) {
    if (info != null) {
      get(id).onNext(info);
    }
  }
}
