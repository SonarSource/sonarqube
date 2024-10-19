/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

/**
 * Updates need_issue_sync flag of project_branches so that tasks which are in progress, won't reindex again.
 */
public class UpdateNeedIssueSyncStep implements ComputationStep {

  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;

  public UpdateNeedIssueSyncStep(DbClient dbClient, TreeRootHolder treeRootHolder) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Component project = treeRootHolder.getRoot();
      dbClient.branchDao().updateNeedIssueSync(dbSession, project.getUuid(), false);
      dbSession.commit();
    }
  }

  @Override
  public String getDescription() {
    return "Update need issue sync for branch";
  }

}
