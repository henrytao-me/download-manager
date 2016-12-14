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

import android.text.TextUtils;

/**
 * Created by henrytao on 12/12/16.
 */

public final class Precondition {

  public static <T extends CharSequence> T checkNotEmpty(final T reference) {
    if (TextUtils.isEmpty(checkNotNull(reference))) {
      throw new IllegalArgumentException();
    }
    return reference;
  }

  public static <T extends CharSequence> T checkNotEmpty(final T reference, final T defValue) {
    try {
      return checkNotEmpty(reference);
    } catch (IllegalArgumentException | NullPointerException ignore) {
    }
    return defValue;
  }

  public static <T> T checkNotNull(final T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

  public static <T> T checkNotNull(final T reference, final T defValue) {
    try {
      return checkNotNull(reference);
    } catch (NullPointerException ignore) {
    }
    return defValue;
  }
}
