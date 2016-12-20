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
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

/**
 * Created by henrytao on 7/27/16.
 */

class FileUtils {

  public static boolean delete(File file) {
    return file == null || (file.exists() && file.delete());
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static File getFile(Uri uri) {
    File file = new File(uri.getPath());
    file.getParentFile().mkdirs();
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

  @SuppressWarnings({"ResultOfMethodCallIgnored"})
  public static File move(File input, File output, boolean autoRename) throws IOException {
    if (input == null || !input.exists() || output == null) {
      throw new IllegalArgumentException("Input and Output files can not be null");
    }
    output.getParentFile().mkdirs();
    if (output.exists() && autoRename) {
      output = autoRenameIfExists(output);
    }
    FileChannel inputChannel = null;
    FileChannel outputChannel = null;
    try {
      inputChannel = new FileInputStream(input).getChannel();
      outputChannel = new FileOutputStream(output).getChannel();
      inputChannel.transferTo(0, inputChannel.size(), outputChannel);
      inputChannel.close();
      input.delete();
    } finally {
      if (inputChannel != null) {
        //noinspection ThrowFromFinallyBlock
        inputChannel.close();
      }
      if (outputChannel != null) {
        //noinspection ThrowFromFinallyBlock
        outputChannel.close();
      }
    }
    return output;
  }

  private static File autoRenameIfExists(File file) {
    if (!file.exists()) {
      return file;
    }
    File parent = file.getParentFile();
    String name = getFilenameWithoutExtension(file);
    String extension = getFileExtension(file);
    int i = 1;
    while (true) {
      file = new File(parent, name + "_" + i + "." + extension);
      if (!file.exists()) {
        break;
      }
      i += 1;
    }
    return file;
  }

  @NonNull
  private static String getFileExtension(File file) {
    try {
      String name = file.getName();
      return name.substring(name.lastIndexOf(".") + 1);
    } catch (Exception ignore) {
    }
    return "";
  }

  @NonNull
  private static String getFilenameWithoutExtension(File file) {
    try {
      String name = file.getName();
      return name.substring(0, name.lastIndexOf("."));
    } catch (Exception ignore) {
    }
    return "";
  }
}
