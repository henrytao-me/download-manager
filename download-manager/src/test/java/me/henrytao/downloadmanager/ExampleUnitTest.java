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

import org.junit.Test;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

  @Test
  public void test() {
    String hello = "abc";
    System.out.println(hello);
    assertThat(hello == "abc", equalTo(true));
  }

  @Test
  public void testThread() {
    ThreadB b = new ThreadB();

    Observable.timer(5000, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation()).subscribe(aLong -> {
      b.interrupt();
    });

    synchronized (b) {
      try {
        System.out.println("Waiting for b to complete");
        b.start();
        b.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println("Total is: " + b.total);
    }
    System.out.println("Done");
  }

  private void log(String value, Object... objects) {
    System.out.println(String.format(Locale.US, value, objects));
  }

  public static class ThreadB extends Thread {

    int total;

    @Override
    public void run() {
      synchronized (this) {
        try {
          for (int i = 0; i < 10; i++) {
            if (interrupted()) {

            }
            total++;
            System.out.println("Output: " + total);
            sleep(1000);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        notify();
      }
    }
  }
}