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

import me.henrytao.downloadmanager.internal.Task;

/**
 * Created by henrytao on 7/27/16.
 */
public final class Info extends Task {

  public static Info create(Task task, long bytesRead, Status status) {
    if (task == null) {
      return null;
    }
    return new Info(task, bytesRead, status);
  }

  private final long bytesRead;

  private final Status status;

  protected Info(Task task, long bytesRead, Status status) {
    super(task.getId(), task.getTag(), task.getUri(), task.getTitle(), task.getDescription(), task.getDestUri(), task.getTempUri(),
        task.getMaxRetry(), task.getRetryCount(), task.getState(), task.getContentLength(), task.getMd5());
    this.bytesRead = bytesRead;
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    Info info = (Info) o;
    if (bytesRead != info.bytesRead) {
      return false;
    }
    return status == info.status;
  }

  @Override
  public long getBytesRead() {
    return getBytesRead(false);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (int) (bytesRead ^ (bytesRead >>> 32));
    result = 31 * result + (status != null ? status.hashCode() : 0);
    return result;
  }

  public long getBytesRead(boolean force) {
    return force || bytesRead == 0 ? super.getBytesRead() : bytesRead;
  }

  public Status getStatus() {
    return status;
  }

  public enum Status {
    NONE(0),
    QUEUEING(1),
    DOWNLOADING(2),
    PAUSING(3),
    DOWNLOADED(4),
    VALIDATING(5),
    FAILED(6),
    SUCCEED(7);

    private static SparseArray<Status> sCaches = new SparseArray<>();

    static {
      for (Status value : Status.values()) {
        sCaches.put(value.toInt(), value);
      }
    }

    public static Status from(int value) {
      return sCaches.get(value, NONE);
    }

    private final int mValue;

    Status(int value) {
      mValue = value;
    }

    @Override
    public String toString() {
      return name();
    }

    public int toInt() {
      return mValue;
    }
  }
}
