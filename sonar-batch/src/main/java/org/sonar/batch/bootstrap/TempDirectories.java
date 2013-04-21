/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TempFileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class TempDirectories {

  public static final String DIR_PREFIX = "sonar-batch";

  // this timeout must be greater than the longest analysis
  public static final int AGE_BEFORE_DELETION = 24 * 60 * 60 * 1000;

  private File rootDir;
  private Map<String, File> directoriesByKey = Maps.newHashMap();

  public TempDirectories() throws IOException {
    this.rootDir = TempFileUtils.createTempDirectory(DIR_PREFIX);
    LoggerFactory.getLogger(getClass()).debug("Temporary directory: " + rootDir.getAbsolutePath());
  }

  public File getRoot() {
    return rootDir;
  }

  /**
   * Get or create a working directory
   */
  public File getDir(String key) {
    if (StringUtils.isBlank(key)) {
      return rootDir;
    }

    File dir = directoriesByKey.get(key);
    if (dir == null) {
      dir = new File(rootDir, key);
      try {
        FileUtils.forceMkdir(dir);
        directoriesByKey.put(key, dir);

      } catch (IOException e) {
        throw new SonarException("Can not create the temp directory: " + dir, e);
      }
    }
    return dir;
  }

  public File getFile(String directoryKey, String filename) {
    File dir = getDir(directoryKey);
    return new File(dir, filename);
  }

  /**
   * This method is executed by picocontainer during shutdown.
   */
  public void stop() {
    directoriesByKey.clear();

    // Deleting temp directory does not work on MS Windows and Sun JVM because URLClassLoader locks JARs and resources.
    // The workaround is that sonar deletes orphans itself.

    // older than AGE_BEFORE_DELETION to be sure that the current dir is deleted on mac and linux.
    rootDir.setLastModified(System.currentTimeMillis() - AGE_BEFORE_DELETION - 60 * 60 * 1000);

    File[] directoriesToDelete = rootDir.getParentFile().listFiles((FileFilter) new AndFileFilter(Arrays.asList(
        DirectoryFileFilter.DIRECTORY, new PrefixFileFilter(DIR_PREFIX), new AgeFileFilter(System.currentTimeMillis() - AGE_BEFORE_DELETION))));
    for (File dir : directoriesToDelete) {
      LoggerFactory.getLogger(getClass()).debug("Delete temporary directory: " + dir);
      FileUtils.deleteQuietly(dir);
    }
  }
}
