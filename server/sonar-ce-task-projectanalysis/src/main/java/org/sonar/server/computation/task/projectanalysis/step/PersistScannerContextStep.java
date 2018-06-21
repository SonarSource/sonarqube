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

import org.sonar.ce.queue.CeTask;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.step.ComputationStep;

import static java.util.Collections.singleton;

public class PersistScannerContextStep implements ComputationStep {
  private final BatchReportReader reportReader;
  private final DbClient dbClient;
  private final CeTask ceTask;

  public PersistScannerContextStep(BatchReportReader reportReader, DbClient dbClient, CeTask ceTask) {
    this.reportReader = reportReader;
    this.dbClient = dbClient;
    this.ceTask = ceTask;
  }

  @Override
  public String getDescription() {
    return "Persist scanner context";
  }

  @Override
  public void execute() {
    try (CloseableIterator<String> logsIterator = reportReader.readScannerLogs()) {
      if (logsIterator.hasNext()) {
        try (DbSession dbSession = dbClient.openSession(false)) {
          // in case the task was restarted, the context might have been already persisted
          // for total reliability, we rather delete the existing row as we don't want to assume the content
          // consistent with the report
          dbClient.ceScannerContextDao().deleteByUuids(dbSession, singleton(ceTask.getUuid()));
          dbSession.commit();
          dbClient.ceScannerContextDao().insert(dbSession, ceTask.getUuid(), logsIterator);
          dbSession.commit();
        }
      }
    }
  }
}
