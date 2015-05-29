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
package org.sonar.server.computation.step;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.computation.batch.MutableBatchReportDirectoryHolder;

/**
 * Extracts the content zip file of the {@link ReportQueue.Item} to a temp directory and adds a {@link File}
 * representing that temp directory to the {@link MutableBatchReportDirectoryHolder}.
 */
public class ReportExtractionStep implements ComputationStep {
  private static final Logger LOG = Loggers.get(ReportExtractionStep.class);

  private final ReportQueue.Item item;
  private final TempFolder tempFolder;
  private final MutableBatchReportDirectoryHolder reportDirectoryHolder;

  public ReportExtractionStep(ReportQueue.Item item, TempFolder tempFolder, MutableBatchReportDirectoryHolder reportDirectoryHolder) {
    this.item = item;
    this.tempFolder = tempFolder;
    this.reportDirectoryHolder = reportDirectoryHolder;
  }

  @Override
  public void execute() {
    File dir = tempFolder.newDir();
    try {
      Profiler profiler = Profiler.createIfDebug(LOG).start();
      ZipUtils.unzip(item.zipFile, dir);
      if (profiler.isDebugEnabled()) {
        String message = String.format("Report extracted | size=%s | project=%s",
            FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(dir)), item.dto.getProjectKey());
        profiler.stopDebug(message);
      }
      reportDirectoryHolder.setDirectory(dir);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to unzip %s into %s", item.zipFile, dir), e);
    }
  }

  @Override
  public String getDescription() {
    return "Extracting batch report to temp directory";
  }

}
