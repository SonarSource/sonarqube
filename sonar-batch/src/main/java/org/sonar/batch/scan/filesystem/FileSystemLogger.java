/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

public class FileSystemLogger implements BatchComponent {

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
    logDir(logger, "Working dir: ", fs.workingDir());
    logDirs(logger, "Source dirs: ", fs.sourceDirs());
    logDirs(logger, "Test dirs: ", fs.testDirs());
    logDirs(logger, "Binary dirs: ", fs.binaryDirs());
    logEncoding(logger, fs.sourceCharset());
  }

  private void logEncoding(Logger logger, Charset charset) {
    if (!fs.isDefaultJvmEncoding()) {
      logger.info("Source encoding: " + charset.displayName() + ", default locale: " + Locale.getDefault());
    } else {
      logger.warn("Source encoding is platform dependent (" + charset.displayName() + "), default locale: " + Locale.getDefault());
    }
  }

  private void logDirs(Logger logger, String label, List<File> dirs) {
    if (!dirs.isEmpty()) {
      logger.info(label + Joiner.on(", ").join(dirs));
    }
  }

  private void logDir(Logger logger, String label, File dir) {
    if (dir != null) {
      logger.info(label + dir.getAbsolutePath());
    }
  }
}
