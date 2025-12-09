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

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.TestBranch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.ce.task.projectanalysis.period.PeriodOrigin;
import org.sonar.ce.task.step.ComputationStep.Context;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;

class PersistReferenceBranchPeriodStepIT {

  private static final String BRANCH_NAME = "feature";

  @RegisterExtension
  private final PeriodHolderRule periodHolder = new PeriodHolderRule();

  @RegisterExtension
  private final AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @RegisterExtension
  private final TreeRootHolderRule treeRootHolderRule = new TreeRootHolderRule();

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final PersistReferenceBranchPeriodStep persistReferenceBranchPeriodStep = new PersistReferenceBranchPeriodStep(
    periodHolder, analysisMetadataHolder, db.getDbClient(), treeRootHolderRule);

  private ProjectData projectData;
  private String branchUuid;

  @BeforeEach
  void setUp() {
    projectData = db.components().insertPrivateProject();
    BranchDto branchDto = db.components().insertProjectBranch(projectData.getProjectDto(), branch -> branch.setKey(BRANCH_NAME));
    branchUuid = branchDto.getUuid();

    analysisMetadataHolder.setProject(new Project(projectData.projectUuid(), projectData.projectKey(), projectData.projectKey(), null, List.of()));
    analysisMetadataHolder.setBranch(new TestBranch(BRANCH_NAME));
    periodHolder.setPeriod(new Period(REFERENCE_BRANCH.name(), "main", null));
    periodHolder.setPeriodOrigin(PeriodOrigin.SCANNER);

    ReportComponent reportComponent = ReportComponent
      .builder(Component.Type.PROJECT, 1)
      .setUuid(branchUuid)
      .setKey(branchDto.getKey())
      .build();
    treeRootHolderRule.setRoot(reportComponent);
  }

  @Test
  void execute_shouldPersistReferenceBranchPeriod() {

    persistReferenceBranchPeriodStep.execute(mock(Context.class));

    NewCodePeriodDto newCodePeriodDto = db.getDbClient().newCodePeriodDao().selectByBranch(db.getSession(), projectData.projectUuid(), branchUuid)
      .orElseGet(() -> fail("No new code period found for branch"));
    assertThat(newCodePeriodDto.getBranchUuid()).isEqualTo(branchUuid);
    assertThat(newCodePeriodDto.getType()).isEqualTo(REFERENCE_BRANCH);
    assertThat(newCodePeriodDto.getValue()).isEqualTo("main");
  }

  @Test
  void execute_shouldUpdateReferenceBranchPeriod() {
    db.newCodePeriods().insert(projectData.projectUuid(), branchUuid, REFERENCE_BRANCH, "old_value");

    persistReferenceBranchPeriodStep.execute(mock(Context.class));

    NewCodePeriodDto newCodePeriodDto = db.getDbClient().newCodePeriodDao().selectByBranch(db.getSession(), projectData.projectUuid(), branchUuid)
      .orElseGet(() -> fail("No new code period found for branch"));
    assertThat(newCodePeriodDto.getBranchUuid()).isEqualTo(branchUuid);
    assertThat(newCodePeriodDto.getType()).isEqualTo(REFERENCE_BRANCH);
    assertThat(newCodePeriodDto.getValue()).isEqualTo("main");
  }

}
