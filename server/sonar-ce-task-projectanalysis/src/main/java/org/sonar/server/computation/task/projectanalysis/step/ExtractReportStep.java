/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.ce.queue.CeTask;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskInputDao;
import org.sonar.server.computation.task.projectanalysis.batch.MutableBatchReportDirectoryHolder;
import org.sonar.server.computation.task.step.ComputationStep;

/**
 * Extracts the content zip file of the {@link CeTask} to a temp directory and adds a {@link File}
 * representing that temp directory to the {@link MutableBatchReportDirectoryHolder}.
 */
public class ExtractReportStep implements ComputationStep {

  private final DbClient dbClient;
  private final CeTask task;
  private final TempFolder tempFolder;
  private final MutableBatchReportDirectoryHolder reportDirectoryHolder;

  public ExtractReportStep(DbClient dbClient, CeTask task, TempFolder tempFolder,
    MutableBatchReportDirectoryHolder reportDirectoryHolder) {
    this.dbClient = dbClient;
    this.task = task;
    this.tempFolder = tempFolder;
    this.reportDirectoryHolder = reportDirectoryHolder;
  }

  @Override
  public void execute() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<CeTaskInputDao.DataStream> opt = dbClient.ceTaskInputDao().selectData(dbSession, task.getUuid());
      if (opt.isPresent()) {
        File unzippedDir = tempFolder.newDir();
        try (CeTaskInputDao.DataStream reportStream = opt.get();
             InputStream zipStream = new BufferedInputStream(reportStream.getInputStream())) {
          ZipUtils.unzip(zipStream, unzippedDir);
        } catch (IOException e) {
          throw new IllegalStateException("Fail to extract report " + task.getUuid() + " from database", e);
        }
        reportDirectoryHolder.setDirectory(unzippedDir);
      } else {
        throw MessageException.of("Analysis report " + task.getUuid() + " is missing in database");
      }
    }
  }

  @Override
  public String getDescription() {
    return "Extract report";
  }

}
