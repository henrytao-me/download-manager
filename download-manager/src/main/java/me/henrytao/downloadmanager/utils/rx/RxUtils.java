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

package me.henrytao.downloadmanager.utils.rx;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import me.henrytao.downloadmanager.Info;
import rx.Observable;

/**
 * Created by henrytao on 7/27/16.
 */
public class RxUtils {

  public static Observable.Transformer<List<Info>, List<Info>> distinctUntilChanged() {
    return observable -> observable
        .map(infos -> {
          Stack<Info> stack = new Stack<>();
          for (Info info : infos) {
            if (stack.size() == 0) {
              stack.push(info);
            } else {
              if (info.state == stack.peek().state) {
                stack.pop();
              }
              stack.push(info);
            }
          }
          int n = stack.size();
          Info[] tmp = new Info[n];
          while (!stack.isEmpty()) {
            tmp[--n] = stack.pop();
          }
          return Arrays.asList(tmp);
        });
  }
}
