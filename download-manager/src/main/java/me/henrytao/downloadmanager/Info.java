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

/**
 * Created by henrytao on 7/27/16.
 */
public class Info {

  public final long bytesRead;

  public final long contentLength;

  public final State state;

  public Info(State state, long bytesRead, long contentLength) {
    this.state = state;
    this.bytesRead = bytesRead;
    this.contentLength = contentLength;
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

  public enum State {
    QUEUEING,
    STARTED,
    DOWNLOADING,
    PAUSED,
    RESUMED,
    DOWNLOADED,
    INVALID
  }
}
