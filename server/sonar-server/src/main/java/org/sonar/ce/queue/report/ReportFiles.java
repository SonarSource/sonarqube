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
package org.sonar.ce.queue.report;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.process.ProcessProperties;

import static java.lang.String.format;

@ComputeEngineSide
public class ReportFiles {

  private static final String ZIP_EXTENSION = "zip";

  private final Settings settings;

  public ReportFiles(Settings settings) {
    this.settings = settings;
  }

  public void save(String taskUuid, InputStream reportInput) {
    File file = fileForUuid(taskUuid);
    try {
      FileUtils.copyInputStreamToFile(reportInput, file);
    } catch (Exception e) {
      org.sonar.core.util.FileUtils.deleteQuietly(file);
      IOUtils.closeQuietly(reportInput);
      throw new IllegalStateException(format("Fail to copy report to file: %s", file.getAbsolutePath()), e);
    }
  }

  public void deleteIfExists(String taskUuid) {
    org.sonar.core.util.FileUtils.deleteQuietly(fileForUuid(taskUuid));
  }

  public void deleteAll() {
    File dir = reportDir();
    if (dir.exists()) {
      try {
        org.sonar.core.util.FileUtils.cleanDirectory(dir);
      } catch (Exception e) {
        throw new IllegalStateException(format("Fail to clean directory: %s", dir.getAbsolutePath()), e);
      }
    }
  }

  private File reportDir() {
    return new File(settings.getString(ProcessProperties.PATH_DATA), "ce/reports");
  }

  /**
   * The analysis report to be processed. Can't be null
   * but may no exist on file system.
   */
  public File fileForUuid(String taskUuid) {
    return new File(reportDir(), format("%s.%s", taskUuid, ZIP_EXTENSION));
  }

  public List<String> listUuids() {
    List<String> uuids = new ArrayList<>();
    File dir = reportDir();
    if (dir.exists()) {
      Collection<File> files = FileUtils.listFiles(dir, new String[]{ZIP_EXTENSION}, false);
      for (File file : files) {
        uuids.add(FilenameUtils.getBaseName(file.getName()));
      }
    }
    return uuids;
  }
}
