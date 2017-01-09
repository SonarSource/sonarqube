/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.ce.queue.CeTask;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Organization;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoadReportAnalysisMetadataHolderStepTest {

  private static final String PROJECT_KEY = "project_key";
  private static final String BRANCH = "origin/master";
  private static final long ANALYSIS_DATE = 123456789L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private ComputationStep underTest;

  @Before
  public void setUp() throws Exception {
    CeTask defaultOrgCeTask = createCeTask(PROJECT_KEY, dbTester.getDefaultOrganization().getUuid());
    underTest = createStep(defaultOrgCeTask);
  }

  @Test
  public void set_root_component_ref() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setRootComponentRef(1)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getRootComponentRef()).isEqualTo(1);
  }

  @Test
  public void set_analysis_date() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setAnalysisDate(ANALYSIS_DATE)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getAnalysisDate()).isEqualTo(ANALYSIS_DATE);
  }

  @Test
  public void set_branch() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setBranch(BRANCH)
        .build());

    CeTask ceTask = createCeTask(PROJECT_KEY + ":" + BRANCH, dbTester.getDefaultOrganization().getUuid());
    ComputationStep underTest = createStep(ceTask);

    underTest.execute();

    assertThat(analysisMetadataHolder.getBranch()).isEqualTo(BRANCH);
  }

  @Test
  public void set_null_branch_when_nothing_in_the_report() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getBranch()).isNull();
  }

  @Test
  public void set_cross_project_duplication_to_true() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setCrossProjectDuplicationActivated(true)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(true);
  }

  @Test
  public void set_cross_project_duplication_to_false() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setCrossProjectDuplicationActivated(false)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(false);
  }

  @Test
  public void set_cross_project_duplication_to_false_when_nothing_in_the_report() throws Exception {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(false);
  }

  @Test
  public void execute_fails_with_MessageException_if_projectKey_is_null_in_CE_task() {
    CeTask res = mock(CeTask.class);
    when(res.getComponentUuid()).thenReturn("prj_uuid");
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().build());

    ComputationStep underTest = createStep(res);

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Compute Engine task component key is null. Project with UUID prj_uuid must have been deleted since report was uploaded. Can not proceed.");

    underTest.execute();
  }

  @Test
  public void execute_fails_with_MessageException_when_projectKey_in_report_is_different_from_componentKey_in_CE_task() {
    reportReader.setMetadata(
      ScannerReport.Metadata.newBuilder()
        .setProjectKey("some other key")
        .build());

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("ProjectKey in report (some other key) is not consistent with projectKey under which the report as been submitted (" + PROJECT_KEY + ")");

    underTest.execute();
  }

  @Test
  public void execute_sets_analysis_date_even_if_MessageException_is_thrown_because_projectKey_is_different_from_componentKey_in_CE_task() {
    reportReader.setMetadata(
      ScannerReport.Metadata.newBuilder()
        .setProjectKey("some other key")
        .setAnalysisDate(ANALYSIS_DATE)
        .build());

    try {
      underTest.execute();
    } catch (MessageException e) {
      assertThat(analysisMetadataHolder.getAnalysisDate()).isEqualTo(ANALYSIS_DATE);
    }
  }

  @Test
  public void execute_fails_with_MessageException_when_report_has_no_organizationKey_but_does_not_belong_to_the_default_organization() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .build());
    OrganizationDto nonDefaultOrganizationDto = dbTester.organizations().insert();

    ComputationStep underTest = createStep(createCeTask(PROJECT_KEY, nonDefaultOrganizationDto.getUuid()));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Report does not specify an OrganizationKey but it has been submitted to another organization (" +
      nonDefaultOrganizationDto.getKey() + ") than the default one (" + dbTester.getDefaultOrganization().getKey() + ")");

    underTest.execute();

  }

  @Test
  public void execute_set_organization_from_ce_task_when_organizationKey_is_not_set_in_report() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .build());

    underTest.execute();

    Organization organization = analysisMetadataHolder.getOrganization();
    OrganizationDto defaultOrganization = dbTester.getDefaultOrganization();
    assertThat(organization.getUuid()).isEqualTo(defaultOrganization.getUuid());
    assertThat(organization.getKey()).isEqualTo(defaultOrganization.getKey());
    assertThat(organization.getName()).isEqualTo(defaultOrganization.getName());
  }

  @Test
  public void execute_set_organization_from_ce_task_when_organizationKey_is_set_in_report() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setOrganizationKey(dbTester.getDefaultOrganization().getKey())
        .build());

    underTest.execute();

    Organization organization = analysisMetadataHolder.getOrganization();
    OrganizationDto defaultOrganization = dbTester.getDefaultOrganization();
    assertThat(organization.getUuid()).isEqualTo(defaultOrganization.getUuid());
    assertThat(organization.getKey()).isEqualTo(defaultOrganization.getKey());
    assertThat(organization.getName()).isEqualTo(defaultOrganization.getName());
  }

  @Test
  public void execute_set_non_default_organization_from_ce_task() {
    OrganizationDto nonDefaultOrganizationDto = dbTester.organizations().insert();
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setOrganizationKey(nonDefaultOrganizationDto.getKey())
        .build());

    ComputationStep underTest = createStep(createCeTask(PROJECT_KEY, nonDefaultOrganizationDto.getUuid()));

    underTest.execute();

    Organization organization = analysisMetadataHolder.getOrganization();
    assertThat(organization.getUuid()).isEqualTo(nonDefaultOrganizationDto.getUuid());
    assertThat(organization.getKey()).isEqualTo(nonDefaultOrganizationDto.getKey());
    assertThat(organization.getName()).isEqualTo(nonDefaultOrganizationDto.getName());
  }

  private LoadReportAnalysisMetadataHolderStep createStep(CeTask ceTask) {
    return new LoadReportAnalysisMetadataHolderStep(ceTask, reportReader, analysisMetadataHolder, defaultOrganizationProvider, dbClient);
  }

  private static ScannerReport.Metadata.Builder newBatchReportBuilder() {
    return ScannerReport.Metadata.newBuilder()
      .setProjectKey(PROJECT_KEY);
  }

  private CeTask createCeTask(String projectKey, String organizationUuid) {
    CeTask res = mock(CeTask.class);
    when(res.getOrganizationUuid()).thenReturn(organizationUuid);
    when(res.getComponentKey()).thenReturn(projectKey);
    return res;
  }

}
