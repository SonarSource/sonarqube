/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import org.apache.commons.io.FileUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginUnzipper;
import org.sonar.core.platform.UnzippedPlugin;
import org.sonar.home.cache.FileCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BatchPluginUnzipper extends PluginUnzipper implements BatchComponent {

  private final FileCache fileCache;

  public BatchPluginUnzipper(FileCache fileCache) {
    this.fileCache = fileCache;
  }

  @Override
  public UnzippedPlugin unzip(PluginInfo info) {
    try {
      File dir = unzipFile(info.getFile());
      return UnzippedPlugin.createFromUnzippedDir(info.getKey(), info.getFile(), dir);
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Fail to open plugin [%s]: %s", info.getKey(), info.getFile().getAbsolutePath()), e);
    }
  }

  private File unzipFile(File cachedFile) throws IOException {
    String filename = cachedFile.getName();
    File destDir = new File(cachedFile.getParentFile(), filename + "_unzip");
    File lockFile = new File(cachedFile.getParentFile(), filename + "_unzip.lock");
    if (!destDir.exists()) {
      FileOutputStream out = new FileOutputStream(lockFile);
      try {
        java.nio.channels.FileLock lock = out.getChannel().lock();
        try {
          // Recheck in case of concurrent processes
          if (!destDir.exists()) {
            File tempDir = fileCache.createTempDir();
            ZipUtils.unzip(cachedFile, tempDir, newLibFilter());
            FileUtils.moveDirectory(tempDir, destDir);
          }
        } finally {
          lock.release();
        }
      } finally {
        out.close();
        FileUtils.deleteQuietly(lockFile);
      }
    }
    return destDir;
  }
}
