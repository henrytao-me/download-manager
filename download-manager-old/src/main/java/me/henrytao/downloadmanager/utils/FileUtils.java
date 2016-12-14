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

package me.henrytao.downloadmanager.utils;

import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

/**
 * Created by henrytao on 7/27/16.
 */
public class FileUtils {

  public static void copy(File input, File output) throws IOException {
    if (input == null || output == null) {
      throw new IllegalArgumentException("Input and Output files can not be null");
    }
    InputStream inputStream = null;
    OutputStream outputStream = null;
    IOException exception = null;
    try {
      inputStream = new FileInputStream(input);
      outputStream = new FileOutputStream(output);
      byte[] buffer = new byte[1024];
      int read;
      while ((read = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
    } catch (IOException ex) {
      exception = ex;
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
      if (outputStream != null) {
        outputStream.flush();
        outputStream.close();
      }
    }
    if (exception != null) {
      throw exception;
    }
  }

  public static boolean delete(File file) {
    return file == null || (file.exists() && file.delete());
  }

  public static File getFile(String path, String name) {
    File file = new File(Uri.parse(path).getPath());
    if (!file.exists() && !file.mkdirs()) {
      throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
    }
    return new File(file, name);
  }

  public static String getMd5(File file) {
    String md5 = "";
    try {
      InputStream input = new FileInputStream(file);
      byte[] buffer = new byte[1024];
      MessageDigest md5Hash = MessageDigest.getInstance("MD5");
      int numRead = 0;
      while (numRead != -1) {
        numRead = input.read(buffer);
        if (numRead > 0) {
          md5Hash.update(buffer, 0, numRead);
        }
      }
      input.close();
      byte[] md5Bytes = md5Hash.digest();
      for (byte md5Byte : md5Bytes) {
        md5 += Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1);
      }
    } catch (Exception ignore) {
      ignore.printStackTrace();
    }
    return md5.length() > 0 ? md5.toLowerCase() : null;
  }

  public static boolean move(File input, File output) {
    if (input == null || output == null) {
      throw new IllegalArgumentException("Input and Output files can not be null");
    }
    return input.renameTo(output);
  }
}
