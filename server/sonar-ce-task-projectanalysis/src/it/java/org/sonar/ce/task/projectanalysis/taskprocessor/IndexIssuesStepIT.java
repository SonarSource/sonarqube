/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.taskprocessor;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.CeTask;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.db.component.BranchType.BRANCH;

public class IndexIssuesStepIT {
  private final String ENTITY_UUID = "entity_uuid";
  private final String BRANCH_UUID = "branch_uuid";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = dbTester.getDbClient();
  private final CeTask.Component entity = new CeTask.Component(ENTITY_UUID, "component key", "component name");
  private final CeTask.Component component = new CeTask.Component(BRANCH_UUID, "component key", "component name");
  private final CeTask ceTask = new CeTask.Builder()
    .setType("type")
    .setUuid("uuid")
    .setComponent(component)
    .setEntity(entity)
    .build();

  @Rule
  public EsTester es = EsTester.create();

  private final IssueIndexer issueIndexer = mock(IssueIndexer.class);
  private final IndexIssuesStep underTest = new IndexIssuesStep(ceTask, dbClient, issueIndexer);

  @Test
  public void execute() {
    BranchDto branchDto = new BranchDto()
      .setBranchType(BRANCH)
      .setKey("branchName")
      .setUuid(BRANCH_UUID)
      .setProjectUuid("project_uuid")
      .setIsMain(false)
      .setNeedIssueSync(true);
    dbClient.branchDao().insert(dbTester.getSession(), branchDto);
    dbTester.commit();

    underTest.execute(() -> null);

    verify(issueIndexer, times(1)).indexOnAnalysis(BRANCH_UUID);
    Optional<BranchDto> branch = dbClient.branchDao().selectByUuid(dbTester.getSession(), BRANCH_UUID);
    assertThat(branch.get().isNeedIssueSync()).isFalse();
  }

  @Test
  public void execute_on_already_indexed_branch() {
    BranchDto branchDto = new BranchDto()
      .setBranchType(BRANCH)
      .setKey("branchName")
      .setUuid(BRANCH_UUID)
      .setProjectUuid("project_uuid")
      .setIsMain(false)
      .setNeedIssueSync(false);
    dbClient.branchDao().insert(dbTester.getSession(), branchDto);
    dbTester.commit();

    underTest.execute(() -> null);

    verify(issueIndexer, times(0)).indexOnAnalysis(BRANCH_UUID);
  }

  @Test
  public void fail_if_missing_component_in_task() {
    CeTask ceTask = new CeTask.Builder()
      .setType("type")
      .setUuid("uuid")
      .setComponent(null)
      .setEntity(null)
      .build();
    IndexIssuesStep underTest = new IndexIssuesStep(ceTask, dbClient, issueIndexer);

    assertThatThrownBy(() -> underTest.execute(() -> null))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("component not found in task");
  }

}
