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

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.Plugin;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.BranchLoader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.Project;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class LoadReportAnalysisMetadataHolderStepIT {

  private static final String PROJECT_KEY = "project_key";
  private static final long ANALYSIS_DATE = 123456789L;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();

  private final DbClient dbClient = db.getDbClient();
  private final TestPluginRepository pluginRepository = new TestPluginRepository();
  private ProjectDto project;
  private ComputationStep underTest;

  @Before
  public void setUp() {
    CeTask defaultOrgCeTask = createCeTask(PROJECT_KEY);
    underTest = createStep(defaultOrgCeTask);
    project = db.components().insertPublicProject(p -> p.setKey(PROJECT_KEY)).getProjectDto();
  }

  @Test
  public void set_root_component_ref() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setRootComponentRef(1)
        .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.getRootComponentRef()).isOne();
  }

  @Test
  public void set_analysis_date() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setAnalysisDate(ANALYSIS_DATE)
        .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.getAnalysisDate()).isEqualTo(ANALYSIS_DATE);
  }

  @Test
  public void set_new_code_reference_branch() {
    String newCodeReferenceBranch = "newCodeReferenceBranch";
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setNewCodeReferenceBranch(newCodeReferenceBranch)
        .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.getNewCodeReferenceBranch()).hasValue(newCodeReferenceBranch);
  }

  @Test
  public void set_project_from_dto() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setRootComponentRef(1)
        .build());

    underTest.execute(new TestComputationStepContext());

    Project project = analysisMetadataHolder.getProject();
    assertThat(project.getUuid()).isEqualTo(this.project.getUuid());
    assertThat(project.getKey()).isEqualTo(this.project.getKey());
    assertThat(project.getName()).isEqualTo(this.project.getName());
    assertThat(project.getDescription()).isEqualTo(this.project.getDescription());
  }

  @Test
  public void set_cross_project_duplication_to_true() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setCrossProjectDuplicationActivated(true)
        .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isTrue();
  }

  @Test
  public void set_cross_project_duplication_to_false() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .setCrossProjectDuplicationActivated(false)
        .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isFalse();
  }

  @Test
  public void set_cross_project_duplication_to_false_when_nothing_in_the_report() {
    reportReader.setMetadata(
      newBatchReportBuilder()
        .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.isCrossProjectDuplicationEnabled()).isFalse();
  }

  @Test
  public void execute_fails_with_ISE_if_component_is_null_in_CE_task() {
    CeTask res = mock(CeTask.class);
    when(res.getComponent()).thenReturn(Optional.empty());
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().build());

    ComputationStep underTest = createStep(res);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("component missing on ce task");
  }

  @Test
  public void execute_fails_with_MessageException_if_main_projectKey_is_null_in_CE_task() {
    CeTask res = mock(CeTask.class);
    Optional<CeTask.Component> component = Optional.of(new CeTask.Component("main_prj_uuid", null, null));
    when(res.getComponent()).thenReturn(component);
    when(res.getEntity()).thenReturn(component);
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().build());

    ComputationStep underTest = createStep(res);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(MessageException.class)
      .hasMessage("Compute Engine task entity key is null. Project with UUID main_prj_uuid must have been deleted since report was uploaded. Can not proceed.");
  }

  @Test
  public void execute_fails_with_MessageException_if_projectKey_is_null_in_CE_task() {
    CeTask res = mock(CeTask.class);
    Optional<CeTask.Component> component = Optional.of(new CeTask.Component("prj_uuid", null, null));
    when(res.getComponent()).thenReturn(component);
    when(res.getEntity()).thenReturn(Optional.of(new CeTask.Component("main_prj_uuid", "main_prj_key", null)));
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().build());

    ComputationStep underTest = createStep(res);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(MessageException.class)
      .hasMessage("Compute Engine task component key is null. Project with UUID prj_uuid must have been deleted since report was uploaded. Can not proceed.");
  }

  @Test
  public void execute_fails_with_MessageException_when_projectKey_in_report_is_different_from_componentKey_in_CE_task() {
    ComponentDto otherProject = db.components().insertPublicProject().getMainBranchComponent();
    reportReader.setMetadata(
      ScannerReport.Metadata.newBuilder()
        .setProjectKey(otherProject.getKey())
        .build());

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(MessageException.class)
      .hasMessage("ProjectKey in report (" + otherProject.getKey() + ") is not consistent with projectKey under which the report has been submitted (" + PROJECT_KEY + ")");

  }

  @Test
  public void execute_sets_branch_even_if_MessageException_is_thrown_because_projectKey_in_report_is_different_from_componentKey_in_CE_task() {
    ComponentDto otherProject = db.components().insertPublicProject().getMainBranchComponent();
    reportReader.setMetadata(
      ScannerReport.Metadata.newBuilder()
        .setProjectKey(otherProject.getKey())
        .build());

    try {
      underTest.execute(new TestComputationStepContext());
    } catch (MessageException e) {
      assertThat(analysisMetadataHolder.getBranch()).isNotNull();
    }
  }

  @Test
  public void execute_sets_analysis_date_even_if_MessageException_is_thrown_because_projectKey_is_different_from_componentKey_in_CE_task() {
    ComponentDto otherProject = db.components().insertPublicProject().getMainBranchComponent();
    reportReader.setMetadata(
      ScannerReport.Metadata.newBuilder()
        .setProjectKey(otherProject.getKey())
        .setAnalysisDate(ANALYSIS_DATE)
        .build());

    try {
      underTest.execute(new TestComputationStepContext());
    } catch (MessageException e) {
      assertThat(analysisMetadataHolder.getAnalysisDate()).isEqualTo(ANALYSIS_DATE);
    }
  }

  @Test
  public void execute_does_not_fail_when_report_has_a_quality_profile_that_does_not_exist_anymore() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ScannerReport.Metadata.Builder metadataBuilder = newBatchReportBuilder();
    metadataBuilder
      .setProjectKey(project.getKey());
    metadataBuilder.putQprofilesPerLanguage("js", ScannerReport.Metadata.QProfile.newBuilder().setKey("p1").setName("Sonar way").setLanguage("js").build());
    reportReader.setMetadata(metadataBuilder.build());

    ComputationStep underTest = createStep(createCeTask(project.getKey()));

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void execute_read_plugins_from_report() {
    // the installed plugins
    pluginRepository.add(
      new PluginInfo("java"),
      new PluginInfo("customjava").setBasePlugin("java"),
      new PluginInfo("php"));

    // the plugins sent by scanner
    ScannerReport.Metadata.Builder metadataBuilder = newBatchReportBuilder();
    metadataBuilder.putPluginsByKey("java", ScannerReport.Metadata.Plugin.newBuilder().setKey("java").setUpdatedAt(10L).build());
    metadataBuilder.putPluginsByKey("php", ScannerReport.Metadata.Plugin.newBuilder().setKey("php").setUpdatedAt(20L).build());
    metadataBuilder.putPluginsByKey("customjava", ScannerReport.Metadata.Plugin.newBuilder().setKey("customjava").setUpdatedAt(30L).build());
    metadataBuilder.putPluginsByKey("uninstalled", ScannerReport.Metadata.Plugin.newBuilder().setKey("uninstalled").setUpdatedAt(40L).build());
    reportReader.setMetadata(metadataBuilder.build());

    underTest.execute(new TestComputationStepContext());

    Assertions.assertThat(analysisMetadataHolder.getScannerPluginsByKey().values()).extracting(ScannerPlugin::getKey, ScannerPlugin::getBasePluginKey, ScannerPlugin::getUpdatedAt)
      .containsExactlyInAnyOrder(
        tuple("java", null, 10L),
        tuple("php", null, 20L),
        tuple("customjava", "java", 30L),
        tuple("uninstalled", null, 40L));
  }

  private LoadReportAnalysisMetadataHolderStep createStep(CeTask ceTask) {
    return new LoadReportAnalysisMetadataHolderStep(ceTask, reportReader, analysisMetadataHolder,
      dbClient, new BranchLoader(analysisMetadataHolder, mock(DefaultBranchNameResolver.class)), pluginRepository);
  }

  private static ScannerReport.Metadata.Builder newBatchReportBuilder() {
    return ScannerReport.Metadata.newBuilder()
      .setProjectKey(PROJECT_KEY);
  }

  private CeTask createCeTask(String projectKey) {
    CeTask res = mock(CeTask.class);
    Optional<CeTask.Component> entity = Optional.of(new CeTask.Component(projectKey + "_uuid", projectKey, projectKey + "_name"));
    Optional<CeTask.Component> component = Optional.of(new CeTask.Component(projectKey + "branch_uuid", projectKey, projectKey + "_name"));
    when(res.getComponent()).thenReturn(component);
    when(res.getEntity()).thenReturn(entity);
    return res;
  }

  private static class TestPluginRepository implements PluginRepository {
    private final Map<String, PluginInfo> pluginsMap = new HashMap<>();

    void add(PluginInfo... plugins) {
      stream(plugins).forEach(p -> pluginsMap.put(p.getKey(), p));
    }

    @Override
    public Collection<PluginInfo> getPluginInfos() {
      return pluginsMap.values();
    }

    @Override
    public PluginInfo getPluginInfo(String key) {
      if (!pluginsMap.containsKey(key)) {
        throw new IllegalArgumentException();
      }
      return pluginsMap.get(key);
    }

    @Override
    public Plugin getPluginInstance(String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Plugin> getPluginInstances() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPlugin(String key) {
      return pluginsMap.containsKey(key);
    }
  }
}
