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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotTesting;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;

public class ValidateProjectStepIT {
  static long PAST_ANALYSIS_TIME = 1_420_088_400_000L; // 2015-01-01
  static long DEFAULT_ANALYSIS_TIME = 1_433_131_200_000L; // 2015-06-01

  static final String PROJECT_KEY = "PROJECT_KEY";
  static final Branch DEFAULT_BRANCH = new DefaultBranchImpl(DEFAULT_MAIN_BRANCH_NAME);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setAnalysisDate(new Date(DEFAULT_ANALYSIS_TIME))
    .setBranch(DEFAULT_BRANCH);

  private final DbClient dbClient = db.getDbClient();

  private final ValidateProjectStep underTest = new ValidateProjectStep(dbClient, treeRootHolder, analysisMetadataHolder);

  @Test
  public void not_fail_if_analysis_date_is_after_last_analysis() {
    ComponentDto project = db.components().insertPrivateProject("ABCD", c -> c.setKey(PROJECT_KEY)).getMainBranchComponent();
    dbClient.snapshotDao().insert(db.getSession(), SnapshotTesting.newAnalysis(project).setCreatedAt(PAST_ANALYSIS_TIME));
    db.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void fail_if_analysis_date_is_before_last_analysis() {
    analysisMetadataHolder.setAnalysisDate(DateUtils.parseDate("2015-01-01"));

    ComponentDto project = db.components().insertPrivateProject("ABCD", c -> c.setKey(PROJECT_KEY)).getMainBranchComponent();
    dbClient.snapshotDao().insert(db.getSession(), SnapshotTesting.newAnalysis(project).setCreatedAt(1433131200000L)); // 2015-06-01
    db.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    var stepContext = new TestComputationStepContext();
    assertThatThrownBy(() -> underTest.execute(stepContext))
      .isInstanceOf(MessageException.class)
      .hasMessageContainingAll("Validation of project failed:",
        "Date of analysis cannot be older than the date of the last known analysis on this project. Value: ",
        "Latest analysis: ");
  }

  @Test
  public void fail_when_project_key_is_invalid() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setKey("inv$lid!")).getMainBranchComponent();
    db.components().insertSnapshot(project, a -> a.setCreatedAt(PAST_ANALYSIS_TIME));
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1)
      .setUuid(project.uuid())
      .setKey(project.getKey())
      .build());

    var stepContext = new TestComputationStepContext();
    assertThatThrownBy(() -> underTest.execute(stepContext))
      .isInstanceOf(MessageException.class)
      .hasMessageContainingAll("Validation of project failed:",
        "The project key ‘inv$lid!’ contains invalid characters.",
        "Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.",
        "You should update the project key with the expected format.");
  }
}
