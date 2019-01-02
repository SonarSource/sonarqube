/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.organization.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;

public class EnableAnalysisStepTest {

  private static final ReportComponent REPORT_PROJECT = ReportComponent.builder(Component.Type.PROJECT, 1).build();
  private static final String PREVIOUS_ANALYSIS_UUID = "ANALYSIS_1";
  private static final String CURRENT_ANALYSIS_UUID = "ANALYSIS_2";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();

  private EnableAnalysisStep underTest = new EnableAnalysisStep(db.getDbClient(), treeRootHolder, analysisMetadataHolder);

  @Test
  public void switch_islast_flag_and_mark_analysis_as_processed() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organization, REPORT_PROJECT.getUuid());
    db.getDbClient().componentDao().insert(db.getSession(), project);
    insertAnalysis(project, PREVIOUS_ANALYSIS_UUID, SnapshotDto.STATUS_PROCESSED, true);
    insertAnalysis(project, CURRENT_ANALYSIS_UUID, SnapshotDto.STATUS_UNPROCESSED, false);
    db.commit();
    treeRootHolder.setRoot(REPORT_PROJECT);
    analysisMetadataHolder.setUuid(CURRENT_ANALYSIS_UUID);

    underTest.execute(new TestComputationStepContext());

    verifyAnalysis(PREVIOUS_ANALYSIS_UUID, SnapshotDto.STATUS_PROCESSED, false);
    verifyAnalysis(CURRENT_ANALYSIS_UUID, SnapshotDto.STATUS_PROCESSED, true);
  }

  @Test
  public void set_islast_flag_and_mark_as_processed_if_no_previous_analysis() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization(), REPORT_PROJECT.getUuid());
    db.getDbClient().componentDao().insert(db.getSession(), project);
    insertAnalysis(project, CURRENT_ANALYSIS_UUID, SnapshotDto.STATUS_UNPROCESSED, false);
    db.commit();
    treeRootHolder.setRoot(REPORT_PROJECT);
    analysisMetadataHolder.setUuid(CURRENT_ANALYSIS_UUID);

    underTest.execute(new TestComputationStepContext());

    verifyAnalysis(CURRENT_ANALYSIS_UUID, SnapshotDto.STATUS_PROCESSED, true);
  }

  private void verifyAnalysis(String uuid, String expectedStatus, boolean expectedLastFlag) {
    Optional<SnapshotDto> analysis = db.getDbClient().snapshotDao().selectByUuid(db.getSession(), uuid);
    assertThat(analysis.get().getStatus()).isEqualTo(expectedStatus);
    assertThat(analysis.get().getLast()).isEqualTo(expectedLastFlag);
  }

  private void insertAnalysis(ComponentDto project, String uuid, String status, boolean isLastFlag) {
    SnapshotDto snapshot = SnapshotTesting.newAnalysis(project)
      .setLast(isLastFlag)
      .setStatus(status)
      .setUuid(uuid);
    db.getDbClient().snapshotDao().insert(db.getSession(), snapshot);
  }
}
