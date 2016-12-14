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

import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Created by henrytao on 7/27/16.
 */

class FileUtils {

  public static boolean delete(File file) {
    return file == null || (file.exists() && file.delete());
  }

  public static File getFile(Uri uri) throws IOException {
    File file = new File(uri.toString());
    if (!file.exists() && !file.mkdirs()) {
      throw new IOException("Could not create directory");
    }
    return file;
  }

  public static String getMd5(File file) {
    StringBuilder builder = new StringBuilder();
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
        builder.append(Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1));
      }
    } catch (Exception ignore) {
      ignore.printStackTrace();
    }
    return builder.length() > 0 ? builder.toString().toLowerCase() : null;
  }

  public static boolean matchMd5(File file, String md5) {
    if (file == null) {
      return false;
    }
    String fileMd5 = getMd5(file);
    return fileMd5 != null && md5 != null && fileMd5.toLowerCase().replaceAll("\"", "").equals(md5.toLowerCase().replaceAll("\"", ""));
  }

  public static boolean move(File input, File output) throws IOException {
    if (input == null || output == null) {
      throw new IllegalArgumentException("Input and Output files can not be null");
    }
    if (!output.mkdirs()) {
      throw new IOException("Could not create output directory");
    }
    return input.renameTo(output);
  }
}
