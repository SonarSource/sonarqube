/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.io.IOException;
import java.io.InputStream;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;

public class PersistPluginCacheStep implements ComputationStep {
  private static final Logger LOGGER = Loggers.get(PersistPluginCacheStep.class);
  private final BatchReportReader reportReader;
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;

  public PersistPluginCacheStep(BatchReportReader reportReader, DbClient dbClient, TreeRootHolder treeRootHolder) {
    this.reportReader = reportReader;
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public String getDescription() {
    return "Persist scanner plugin cache";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    InputStream pluginCacheStream = reportReader.getPluginCache();
    if (pluginCacheStream != null) {
      try (var dataStream = pluginCacheStream;
        var dbSession = dbClient.openSession(false)) {
        String branchUuid = treeRootHolder.getRoot().getUuid();
        dbClient.scannerCacheDao().remove(dbSession, branchUuid);
        dbClient.scannerCacheDao().insert(dbSession, branchUuid, dataStream);
        dbSession.commit();
      } catch (IOException e) {
        LOGGER.error("Error in reading plugin cache", e);
      }
    }

  }
}
