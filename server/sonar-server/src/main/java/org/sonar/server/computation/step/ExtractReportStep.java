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
import org.sonar.server.computation.queue.CeTask;
import org.sonar.server.computation.ReportFiles;
import org.sonar.server.computation.batch.MutableBatchReportDirectoryHolder;

/**
 * Extracts the content zip file of the {@link CeTask} to a temp directory and adds a {@link File}
 * representing that temp directory to the {@link MutableBatchReportDirectoryHolder}.
 */
public class ExtractReportStep implements ComputationStep {
  private static final Logger LOG = Loggers.get(ExtractReportStep.class);

  private final ReportFiles reportFiles;
  private final CeTask task;
  private final TempFolder tempFolder;
  private final MutableBatchReportDirectoryHolder reportDirectoryHolder;

  public ExtractReportStep(ReportFiles reportFiles, CeTask task, TempFolder tempFolder,
    MutableBatchReportDirectoryHolder reportDirectoryHolder) {
    this.reportFiles = reportFiles;
    this.task = task;
    this.tempFolder = tempFolder;
    this.reportDirectoryHolder = reportDirectoryHolder;
  }

  @Override
  public void execute() {
    File dir = tempFolder.newDir();
    File zip = reportFiles.fileForUuid(task.getUuid());
    try {
      ZipUtils.unzip(zip, dir);
      reportDirectoryHolder.setDirectory(dir);
      LOG.info("Analysis report extracted | size={} | compressed={}",
        FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(dir)), FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(zip)));
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to unzip %s into %s", zip, dir), e);
    }
  }

  @Override
  public String getDescription() {
    return "Extract report";
  }

}
