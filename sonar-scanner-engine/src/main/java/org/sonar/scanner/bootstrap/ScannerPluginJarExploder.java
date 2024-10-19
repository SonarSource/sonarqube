/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.scanner.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginJarExploder;

import static org.sonar.core.util.FileUtils.deleteQuietly;

public class ScannerPluginJarExploder extends PluginJarExploder {

  private final PluginFiles pluginFiles;

  public ScannerPluginJarExploder(PluginFiles pluginFiles) {
    this.pluginFiles = pluginFiles;
  }

  @Override
  public ExplodedPlugin explode(PluginInfo info) {
    try {
      File dir = unzipFile(info.getNonNullJarFile());
      return explodeFromUnzippedDir(info, info.getNonNullJarFile(), dir);
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Fail to open plugin [%s]: %s", info.getKey(), info.getNonNullJarFile().getAbsolutePath()), e);
    }
  }

  private File unzipFile(File cachedFile) throws IOException {
    String filename = cachedFile.getName();
    File destDir = new File(cachedFile.getParentFile(), filename + "_unzip");
    File lockFile = new File(cachedFile.getParentFile(), filename + "_unzip.lock");
    if (!destDir.exists()) {
      try (FileOutputStream out = new FileOutputStream(lockFile)) {
        FileLock lock = createLockWithRetries(out.getChannel());
        try {
          // Recheck in case of concurrent processes
          if (!destDir.exists()) {
            File tempDir = pluginFiles.createTempDir();
            ZipUtils.unzip(cachedFile, tempDir, newLibFilter());
            FileUtils.moveDirectory(tempDir, destDir);
          }
        } finally {
          lock.release();
        }
      } finally {
        deleteQuietly(lockFile);
      }
    }
    return destDir;
  }

  private static FileLock createLockWithRetries(FileChannel channel) throws IOException {
    int tryCount = 0;
    while (tryCount++ < 10) {
      try {
        return channel.lock();
      } catch (OverlappingFileLockException ofle) {
        // ignore overlapping file exception
      }
      try {
        Thread.sleep(200L * tryCount);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    throw new IOException("Unable to get lock after " + tryCount + " tries");
  }
}
