/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateNeedIssueSyncStepIT {
  private static final Component PROJECT = ReportComponent.DUMB_PROJECT;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(PROJECT);

  private DbClient dbClient = db.getDbClient();

  UpdateNeedIssueSyncStep underTest = new UpdateNeedIssueSyncStep(dbClient, treeRootHolder);

  @Test
  public void analysis_step_updates_need_issue_sync_flag() {
    ComponentDto project = db.components()
      .insertPrivateProject(c -> c.setUuid(PROJECT.getUuid()).setKey(PROJECT.getKey())).getMainBranchComponent();
    dbClient.branchDao().updateNeedIssueSync(db.getSession(), PROJECT.getUuid(), true);
    db.getSession().commit();

    assertThat(dbClient.branchDao().selectByUuid(db.getSession(), project.uuid()))
      .isNotEmpty()
      .map(BranchDto::isNeedIssueSync)
      .hasValue(true);

    underTest.execute(new TestComputationStepContext());

    assertThat(dbClient.branchDao().selectByUuid(db.getSession(), project.uuid()))
      .isNotEmpty()
      .map(BranchDto::isNeedIssueSync)
      .hasValue(false);
  }

}
