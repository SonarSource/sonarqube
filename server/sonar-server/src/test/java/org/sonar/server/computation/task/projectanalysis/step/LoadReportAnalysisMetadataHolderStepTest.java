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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.ce.queue.CeTask;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Organization;
import org.sonar.server.computation.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.component.BranchLoader;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.organization.TestDefaultOrganizationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class LoadReportAnalysisMetadataHolderStepTest {

  private static final String PROJECT_KEY = "project_key";
  private static final long ANALYSIS_DATE = 123456789L;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private PluginRepository pluginRepository = mock(PluginRepository.class);
  private ComputationStep underTest;
  private OrganizationFlags organizationFlags = mock(OrganizationFlags.class);

  @Before
  public void setUp() {
    CeTask defaultOrgCeTask = createCeTask(PROJECT_KEY, db.getDefaultOrganization().getUuid());
    underTest = createStep(defaultOrgCeTask);
    db.components().insertPublicProject(db.getDefaultOrganization(), p -> p.setDbKey(PROJECT_KEY));
  }

  @Test
  public void set_root_component_ref() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setRootComponentRef(1)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getRootComponentRef()).isEqualTo(1);
  }

  @Test
  public void set_analysis_date() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setAnalysisDate(ANALYSIS_DATE)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getAnalysisDate()).isEqualTo(ANALYSIS_DATE);
  }

  @Test
  public void set_cross_project_duplication_to_true() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setCrossProjectDuplicationActivated(true)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(true);
  }

  @Test
  public void set_cross_project_duplication_to_false() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setCrossProjectDuplicationActivated(false)
        .build());

    underTest.execute();

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isEqualTo(false);
  }

  @Test
  public void set_cross_project_duplication_to_false_when_nothing_in_the_report() {
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
    when(res.getOrganizationUuid()).thenReturn(defaultOrganizationProvider.get().getUuid());
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
    OrganizationDto nonDefaultOrganizationDto = db.organizations().insert();

    ComputationStep underTest = createStep(createCeTask(PROJECT_KEY, nonDefaultOrganizationDto.getUuid()));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Report does not specify an OrganizationKey but it has been submitted to another organization (" +
      nonDefaultOrganizationDto.getKey() + ") than the default one (" + db.getDefaultOrganization().getKey() + ")");

    underTest.execute();
  }

  @Test
  public void execute_set_organization_from_ce_task_when_organizationKey_is_not_set_in_report() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .build());

    underTest.execute();

    Organization organization = analysisMetadataHolder.getOrganization();
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    assertThat(organization.getUuid()).isEqualTo(defaultOrganization.getUuid());
    assertThat(organization.getKey()).isEqualTo(defaultOrganization.getKey());
    assertThat(organization.getName()).isEqualTo(defaultOrganization.getName());
  }

  @Test
  @UseDataProvider("organizationEnabledFlags")
  public void execute_set_organization_from_ce_task_when_organizationKey_is_set_in_report(boolean organizationEnabled) {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setOrganizationKey(db.getDefaultOrganization().getKey())
        .build());
    when(organizationFlags.isEnabled(any())).thenReturn(organizationEnabled);

    underTest.execute();

    Organization organization = analysisMetadataHolder.getOrganization();
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    assertThat(organization.getUuid()).isEqualTo(defaultOrganization.getUuid());
    assertThat(organization.getKey()).isEqualTo(defaultOrganization.getKey());
    assertThat(organization.getName()).isEqualTo(defaultOrganization.getName());
    assertThat(analysisMetadataHolder.isOrganizationsEnabled()).isEqualTo(organizationEnabled);
  }

  @Test
  @UseDataProvider("organizationEnabledFlags")
  public void execute_set_non_default_organization_from_ce_task(boolean organizationEnabled) {
    OrganizationDto nonDefaultOrganizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(nonDefaultOrganizationDto);
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setOrganizationKey(nonDefaultOrganizationDto.getKey())
        .setProjectKey(project.getDbKey())
        .build());
    when(organizationFlags.isEnabled(any())).thenReturn(organizationEnabled);

    ComputationStep underTest = createStep(createCeTask(project.getDbKey(), nonDefaultOrganizationDto.getUuid()));

    underTest.execute();

    Organization organization = analysisMetadataHolder.getOrganization();
    assertThat(organization.getUuid()).isEqualTo(nonDefaultOrganizationDto.getUuid());
    assertThat(organization.getKey()).isEqualTo(nonDefaultOrganizationDto.getKey());
    assertThat(organization.getName()).isEqualTo(nonDefaultOrganizationDto.getName());
    assertThat(analysisMetadataHolder.isOrganizationsEnabled()).isEqualTo(organizationEnabled);
  }

  @DataProvider
  public static Object[][] organizationEnabledFlags() {
    return new Object[][] {
      {true},
      {false}
    };
  }

  @Test
  public void execute_ensures_that_report_has_quality_profiles_matching_the_project_organization() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    ScannerReport.Metadata.Builder metadataBuilder = newBatchReportBuilder();
    metadataBuilder
      .setOrganizationKey(organization.getKey())
      .setProjectKey(project.getDbKey());
    metadataBuilder.getMutableQprofilesPerLanguage().put("js", ScannerReport.Metadata.QProfile.newBuilder().setKey("p1").setName("Sonar way").setLanguage("js").build());
    reportReader.setMetadata(metadataBuilder.build());

    db.qualityProfiles().insert(organization, p -> p.setLanguage("js").setKee("p1"));

    ComputationStep underTest = createStep(createCeTask(project.getDbKey(), organization.getUuid()));

    // no errors
    underTest.execute();
  }

  @Test
  public void execute_fails_with_MessageException_when_report_has_quality_profiles_on_other_organizations() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto projectInOrg1 = db.components().insertPublicProject(organization1);
    ScannerReport.Metadata.Builder metadataBuilder = newBatchReportBuilder();
    metadataBuilder
      .setOrganizationKey(organization1.getKey())
      .setProjectKey(projectInOrg1.getDbKey());
    metadataBuilder.getMutableQprofilesPerLanguage().put("js", ScannerReport.Metadata.QProfile.newBuilder().setKey("jsInOrg1").setName("Sonar way").setLanguage("js").build());
    metadataBuilder.getMutableQprofilesPerLanguage().put("php", ScannerReport.Metadata.QProfile.newBuilder().setKey("phpInOrg2").setName("PHP way").setLanguage("php").build());
    reportReader.setMetadata(metadataBuilder.build());

    db.qualityProfiles().insert(organization1, p -> p.setLanguage("js").setKee("jsInOrg1"));
    db.qualityProfiles().insert(organization2, p -> p.setLanguage("php").setKee("phpInOrg2"));

    ComputationStep underTest = createStep(createCeTask(projectInOrg1.getDbKey(), organization1.getUuid()));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Quality profiles with following keys don't exist in organization [" + organization1.getKey() + "]: phpInOrg2");

    underTest.execute();
  }

  @Test
  public void execute_does_not_fail_when_report_has_a_quality_profile_that_does_not_exist_anymore() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    ScannerReport.Metadata.Builder metadataBuilder = newBatchReportBuilder();
    metadataBuilder
      .setOrganizationKey(organization.getKey())
      .setProjectKey(project.getDbKey());
    metadataBuilder.getMutableQprofilesPerLanguage().put("js", ScannerReport.Metadata.QProfile.newBuilder().setKey("p1").setName("Sonar way").setLanguage("js").build());
    reportReader.setMetadata(metadataBuilder.build());

    ComputationStep underTest = createStep(createCeTask(project.getDbKey(), organization.getUuid()));

    underTest.execute();
  }

  @Test
  public void execute_read_plugins_from_report() {
    ScannerReport.Metadata.Builder metadataBuilder = newBatchReportBuilder();
    metadataBuilder.getMutablePluginsByKey().put("java", ScannerReport.Metadata.Plugin.newBuilder().setKey("java").setUpdatedAt(12345L).build());
    metadataBuilder.getMutablePluginsByKey().put("php", ScannerReport.Metadata.Plugin.newBuilder().setKey("php").setUpdatedAt(678910L).build());
    metadataBuilder.getMutablePluginsByKey().put("customjava", ScannerReport.Metadata.Plugin.newBuilder().setKey("customjava").setUpdatedAt(111111L).build());
    when(pluginRepository.getPluginInfo("customjava")).thenReturn(new PluginInfo("customjava").setBasePlugin("java"));

    reportReader.setMetadata(metadataBuilder.build());

    underTest.execute();

    assertThat(analysisMetadataHolder.getScannerPluginsByKey()).containsOnlyKeys("java", "php", "customjava");
    assertThat(analysisMetadataHolder.getScannerPluginsByKey().values()).extracting(ScannerPlugin::getKey, ScannerPlugin::getBasePluginKey, ScannerPlugin::getUpdatedAt)
      .containsOnly(
        tuple("java", null, 12345L),
        tuple("customjava", "java", 111111L),
        tuple("php", null, 678910L));
  }

  private LoadReportAnalysisMetadataHolderStep createStep(CeTask ceTask) {
    return new LoadReportAnalysisMetadataHolderStep(ceTask, reportReader, analysisMetadataHolder,
      defaultOrganizationProvider, dbClient, new BranchLoader(analysisMetadataHolder), pluginRepository, organizationFlags);
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
