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
package org.sonar.batch.scan.filesystem;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchSide;
import org.sonar.api.scan.filesystem.PathResolver;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@BatchSide
public class FileSystemLogger {

  private final DefaultModuleFileSystem fs;

  public FileSystemLogger(DefaultModuleFileSystem fs) {
    this.fs = fs;
  }

  public void log() {
    doLog(LoggerFactory.getLogger(getClass()));
  }

  @VisibleForTesting
  void doLog(Logger logger) {
    logDir(logger, "Base dir: ", fs.baseDir());
    logDir(logger, "Working dir: ", fs.workDir());
    logPaths(logger, "Source paths: ", fs.baseDir(), fs.sources());
    logPaths(logger, "Test paths: ", fs.baseDir(), fs.tests());
    logPaths(logger, "Binary dirs: ", fs.baseDir(), fs.binaryDirs());
    logEncoding(logger, fs.encoding());
  }

  private void logEncoding(Logger logger, Charset charset) {
    if (!fs.isDefaultJvmEncoding()) {
      logger.info("Source encoding: " + charset.displayName() + ", default locale: " + Locale.getDefault());
    } else {
      logger.warn("Source encoding is platform dependent (" + charset.displayName() + "), default locale: " + Locale.getDefault());
    }
  }

  private void logPaths(Logger logger, String label, File baseDir, List<File> paths) {
    if (!paths.isEmpty()) {
      PathResolver resolver = new PathResolver();
      StringBuilder sb = new StringBuilder(label);
      for (Iterator<File> it = paths.iterator(); it.hasNext();) {
        File file = it.next();
        String relativePathToBaseDir = resolver.relativePath(baseDir, file);
        if (relativePathToBaseDir == null) {
          sb.append(file);
        } else if (StringUtils.isBlank(relativePathToBaseDir)) {
          sb.append(".");
        } else {
          sb.append(relativePathToBaseDir);
        }
        if (it.hasNext()) {
          sb.append(", ");
        }
      }
      logger.info(sb.toString());
    }
  }

  private void logDir(Logger logger, String label, File dir) {
    if (dir != null) {
      logger.info(label + dir.getAbsolutePath());
    }
  }
}
