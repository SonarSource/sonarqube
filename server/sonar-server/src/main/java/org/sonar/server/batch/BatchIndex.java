/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;

/**
 * JAR files to be downloaded by sonar-runner.
 */
@ServerSide
public class BatchIndex implements Startable {

  private final Server server;
  private String index;
  private File batchDir;

  public BatchIndex(Server server) {
    this.server = server;
  }

  @Override
  public void start() {
    StringBuilder sb = new StringBuilder();
    batchDir = new File(server.getRootDir(), "lib/batch");
    if (batchDir.exists()) {
      Collection<File> files = FileUtils.listFiles(batchDir, HiddenFileFilter.VISIBLE, FileFilterUtils.directoryFileFilter());
      for (File file : files) {
        String filename = file.getName();
        if (StringUtils.endsWith(filename, ".jar")) {
          try (FileInputStream fis = new FileInputStream(file)) {
            sb.append(filename).append('|').append(DigestUtils.md5Hex(fis)).append(CharUtils.LF);
          } catch (IOException e) {
            throw new IllegalStateException("Fail to compute hash", e);
          }
        }
      }
    }
    this.index = sb.toString();
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  String getIndex() {
    return index;
  }

  File getFile(String filename) {
    try {
      File input = new File(batchDir, filename);
      if (!input.exists() || !FileUtils.directoryContains(batchDir, input)) {
        throw new IllegalArgumentException("Bad filename: " + filename);
      }
      return input;

    } catch (IOException e) {
      throw new IllegalStateException("Can get file " + filename, e);
    }
  }
}
