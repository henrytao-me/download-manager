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

package me.henrytao.downloadmanager;

import android.util.SparseArray;

/**
 * Created by henrytao on 7/27/16.
 */
public final class Info {

  public final long bytesRead;

  public final long contentLength;

  public final State state;

  private Throwable mThrowable;

  public Info(State state, long bytesRead, long contentLength) {
    this(state, bytesRead, contentLength, null);
  }

  public Info(State state, long bytesRead, long contentLength, Throwable throwable) {
    this.state = state;
    this.bytesRead = bytesRead;
    this.contentLength = contentLength;
    mThrowable = throwable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Info info = (Info) o;
    if (bytesRead != info.bytesRead) {
      return false;
    }
    if (contentLength != info.contentLength) {
      return false;
    }
    return state == info.state;
  }

  @Override
  public int hashCode() {
    int result = (int) (bytesRead ^ (bytesRead >>> 32));
    result = 31 * result + (int) (contentLength ^ (contentLength >>> 32));
    result = 31 * result + state.hashCode();
    return result;
  }

  public Throwable getThrowable() {
    return mThrowable;
  }

  public enum State {
    NONE(0),
    QUEUEING(1),
    DOWNLOADING(2),
    PAUSING(3),
    DOWNLOADED(4),
    VALIDATING(5),
    FAILED(6),
    SUCCEED(7);

    private static SparseArray<State> sCaches = new SparseArray<>();

    static {
      for (State value : State.values()) {
        sCaches.put(value.toInt(), value);
      }
    }

    public static State from(int value) {
      return sCaches.get(value, NONE);
    }

    private final int mValue;

    State(int value) {
      mValue = value;
    }

    public int toInt() {
      return mValue;
    }
  }
}
