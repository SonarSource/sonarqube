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
package org.sonar.server.computation.batch;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.computation.ReportQueue;

public class ReportExtractor {
  private static final Logger LOG = Loggers.get(ReportExtractor.class);

  private final TempFolder tempFolder;

  public ReportExtractor(TempFolder tempFolder) {
    this.tempFolder = tempFolder;
  }

  public File extractReportInDir(ReportQueue.Item item) {
    File dir = tempFolder.newDir();
    try {
      Profiler profiler = Profiler.createIfDebug(LOG).start();
      ZipUtils.unzip(item.zipFile, dir);
      if (profiler.isDebugEnabled()) {
        String message = String.format("Report extracted | size=%s | project=%s",
            FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(dir)), item.dto.getProjectKey());
        profiler.stopDebug(message);
      }
      return dir;
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to unzip %s into %s", item.zipFile, dir), e);
    }
  }
}
