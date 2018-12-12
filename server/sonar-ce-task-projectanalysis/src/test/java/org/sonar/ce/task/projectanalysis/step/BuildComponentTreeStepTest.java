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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.MutableTreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ReportModulesPath;
import org.sonar.ce.task.projectanalysis.issue.IssueRelocationToRoot;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;
import org.sonar.scanner.protocol.output.ScannerReport.Component.FileStatus;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.DIRECTORY;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.FILE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.MODULE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.PROJECT;

@RunWith(DataProviderRunner.class)
public class BuildComponentTreeStepTest {

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 2;
  private static final int DIR_REF_1 = 3;
  private static final int FILE_1_REF = 4;
  private static final int FILE_2_REF = 5;
  private static final int DIR_REF_2 = 6;
  private static final int FILE_3_REF = 7;
  private static final int LEAFLESS_MODULE_REF = 8;
  private static final int LEAFLESS_DIR_REF = 9;
  private static final int UNCHANGED_FILE_REF = 10;

  private static final String REPORT_PROJECT_KEY = "REPORT_PROJECT_KEY";
  private static final String REPORT_MODULE_KEY = "MODULE_KEY";
  private static final String REPORT_DIR_PATH_1 = "src/main/java/dir1";
  private static final String REPORT_FILE_PATH_1 = "src/main/java/dir1/File1.java";
  private static final String REPORT_FILE_NAME_1 = "File1.java";
  private static final String REPORT_DIR_PATH_2 = "src/main/java/dir2";
  private static final String REPORT_FILE_PATH_2 = "src/main/java/dir2/File2.java";
  private static final String REPORT_FILE_PATH_3 = "src/main/java/dir2/File3.java";
  private static final String REPORT_LEAFLESS_MODULE_KEY = "LEAFLESS_MODULE_KEY";
  private static final String REPORT_LEAFLESS_DIR_PATH = "src/main/java/leafless";
  private static final String REPORT_UNCHANGED_FILE_PATH = "src/main/java/leafless/File3.java";

  private static final long ANALYSIS_DATE = 123456789L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule()
    .setMetadata(ScannerReport.Metadata.newBuilder()
      .setProjectKey(REPORT_PROJECT_KEY)
      .setRootComponentRef(ROOT_REF)
      .build());
  @Rule
  public MutableTreeRootHolderRule treeRootHolder = new MutableTreeRootHolderRule();
  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();
  private ReportModulesPath reportModulesPath = new ReportModulesPath(reportReader);
  private IssueRelocationToRoot issueRelocationToRoot = mock(IssueRelocationToRoot.class);

  private DbClient dbClient = dbTester.getDbClient();
  private BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder,
    issueRelocationToRoot, reportModulesPath);

  @Test(expected = NullPointerException.class)
  public void fails_if_root_component_does_not_exist_in_reportReader() {
    setAnalysisMetadataHolder();

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void verify_tree_is_correctly_built() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(component(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1, DIR_REF_2));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_PATH_1, FILE_1_REF, FILE_2_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));
    reportReader.putComponent(componentWithPath(FILE_2_REF, FILE, REPORT_FILE_PATH_2));
    reportReader.putComponent(componentWithPath(DIR_REF_2, DIRECTORY, REPORT_DIR_PATH_2, FILE_3_REF));
    reportReader.putComponent(componentWithPath(FILE_3_REF, FILE, REPORT_FILE_PATH_3));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    Component root = treeRootHolder.getRoot();
    assertThat(root).isNotNull();
    verifyComponent(root, Component.Type.PROJECT, ROOT_REF, 1);

    Component dir = root.getChildren().iterator().next();
    verifyComponent(dir, Component.Type.DIRECTORY, null, 2);

    Component dir1 = dir.getChildren().get(0);
    verifyComponent(dir1, Component.Type.DIRECTORY, null, 1);
    verifyComponent(dir1.getChildren().get(0), Component.Type.FILE, FILE_1_REF, 0);

    Component dir2 = dir.getChildren().get(1);
    verifyComponent(dir2, Component.Type.DIRECTORY, null, 2);
    verifyComponent(dir2.getChildren().get(0), Component.Type.FILE, FILE_2_REF, 0);
    verifyComponent(dir2.getChildren().get(1), Component.Type.FILE, FILE_3_REF, 0);

    context.getStatistics().assertValue("components", 7);
  }

  @Test
  public void compute_keys_and_uuids() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(component(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_PATH_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName());
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, REPORT_DIR_PATH_1);
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1);
  }

  @Test
  public void return_existing_uuids() {
    setAnalysisMetadataHolder();
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = insertComponent(newPrivateProjectDto(organizationDto, "ABCD").setDbKey(REPORT_PROJECT_KEY));
    ComponentDto directory = newDirectory(project, "CDEF", REPORT_DIR_PATH_1);
    insertComponent(directory.setDbKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1));
    insertComponent(newFileDto(project, directory, "DEFG")
      .setDbKey(REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1)
      .setPath(REPORT_FILE_PATH_1));

    // new structure, without modules
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_PATH_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName(), "ABCD");
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, REPORT_DIR_PATH_1, "CDEF");
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1, "DEFG");
  }

  @Test
  public void return_existing_uuids_with_modules() {
    setAnalysisMetadataHolder();
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = insertComponent(newPrivateProjectDto(organizationDto, "ABCD").setDbKey(REPORT_PROJECT_KEY));
    ComponentDto module = insertComponent(newModuleDto("BCDE", project).setDbKey(REPORT_MODULE_KEY));
    ComponentDto directory = newDirectory(module, "CDEF", REPORT_DIR_PATH_1);
    insertComponent(directory.setDbKey(REPORT_MODULE_KEY + ":" + REPORT_DIR_PATH_1));
    insertComponent(newFileDto(module, directory, "DEFG")
      .setDbKey(REPORT_MODULE_KEY + ":" + REPORT_FILE_PATH_1)
      .setPath(REPORT_FILE_PATH_1));

    // new structure, without modules
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, "module/" + REPORT_DIR_PATH_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, "module/" + REPORT_FILE_PATH_1));
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().putModulesProjectRelativePathByKey(REPORT_MODULE_KEY,
      "module").build());
    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName(), "ABCD");
    verifyComponentByKey(REPORT_PROJECT_KEY + ":module/" + REPORT_DIR_PATH_1, REPORT_PROJECT_KEY + ":module/" + REPORT_DIR_PATH_1, "module/" + REPORT_DIR_PATH_1, "CDEF");
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":module/" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1, "DEFG");
  }

  @Test
  public void generate_keys_when_using_new_branch() {
    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn("origin/feature");
    when(branch.isMain()).thenReturn(false);
    when(branch.isLegacyFeature()).thenReturn(false);
    when(branch.generateKey(any(), any())).thenReturn("generated");
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(Project.from(newPrivateProjectDto(newOrganizationDto()).setDbKey(REPORT_PROJECT_KEY)))
      .setBranch(branch);
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder,
      issueRelocationToRoot, reportModulesPath);
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(component(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_PATH_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, "generated", REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName(), null);

    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, "generated", REPORT_DIR_PATH_1);
    verifyComponentByRef(FILE_1_REF, "generated", REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1, null);
  }

  @Test
  @UseDataProvider("shortLivingBranchAndPullRequest")
  public void prune_modules_and_directories_without_leaf_descendants_on_non_long_branch(BranchType branchType) {
    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn("origin/feature");
    when(branch.isMain()).thenReturn(false);
    when(branch.getType()).thenReturn(branchType);
    when(branch.isLegacyFeature()).thenReturn(false);
    when(branch.generateKey(any(), any())).thenReturn("generated");
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(Project.from(newPrivateProjectDto(newOrganizationDto()).setDbKey(REPORT_PROJECT_KEY)))
      .setBranch(branch);
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder,
      issueRelocationToRoot, reportModulesPath);
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF, LEAFLESS_MODULE_REF));
    reportReader.putComponent(component(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_PATH_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    reportReader.putComponent(component(LEAFLESS_MODULE_REF, MODULE, REPORT_LEAFLESS_MODULE_KEY, LEAFLESS_DIR_REF));
    reportReader.putComponent(componentWithPath(LEAFLESS_DIR_REF, DIRECTORY, REPORT_LEAFLESS_DIR_PATH, UNCHANGED_FILE_REF));
    ScannerReport.Component unchangedFile = ScannerReport.Component.newBuilder()
      .setType(FILE)
      .setRef(UNCHANGED_FILE_REF)
      .setProjectRelativePath(REPORT_UNCHANGED_FILE_PATH)
      .setStatus(FileStatus.SAME)
      .setLines(1)
      .build();
    reportReader.putComponent(unchangedFile);

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, "generated", REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName(), null);
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, "generated", "dir1");
    verifyComponentByRef(FILE_1_REF, "generated", REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1,null);

    verifyComponentMissingByRef(LEAFLESS_MODULE_REF);
    verifyComponentMissingByRef(LEAFLESS_DIR_REF);
    verifyComponentMissingByRef(UNCHANGED_FILE_REF);
  }

  @Test
  public void do_not_prune_modules_and_directories_without_leaf_descendants_on_long_branch() {
    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn("origin/feature");
    when(branch.isMain()).thenReturn(false);
    when(branch.getType()).thenReturn(BranchType.LONG);
    when(branch.isLegacyFeature()).thenReturn(false);
    when(branch.generateKey(any(), any())).thenReturn("generated");
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(Project.from(newPrivateProjectDto(newOrganizationDto()).setDbKey(REPORT_PROJECT_KEY)))
      .setBranch(branch);
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder,
      issueRelocationToRoot, reportModulesPath);
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF, LEAFLESS_MODULE_REF));
    reportReader.putComponent(component(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_PATH_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    reportReader.putComponent(component(LEAFLESS_MODULE_REF, MODULE, REPORT_LEAFLESS_MODULE_KEY, LEAFLESS_DIR_REF));
    reportReader.putComponent(componentWithPath(LEAFLESS_DIR_REF, DIRECTORY, REPORT_LEAFLESS_DIR_PATH, UNCHANGED_FILE_REF));
    ScannerReport.Component unchangedFile = ScannerReport.Component.newBuilder()
      .setType(FILE)
      .setRef(UNCHANGED_FILE_REF)
      .setProjectRelativePath(REPORT_UNCHANGED_FILE_PATH)
      .setStatus(FileStatus.SAME)
      .setLines(1)
      .build();
    reportReader.putComponent(unchangedFile);

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, "generated", REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName(), null);
    verifyComponentMissingByRef(MODULE_REF);
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, "generated", "dir1");
    verifyComponentByRef(FILE_1_REF, "generated", REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1, null);

    verifyComponentMissingByRef(LEAFLESS_MODULE_REF);
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_LEAFLESS_DIR_PATH, "generated", "leafless");
    verifyComponentByRef(UNCHANGED_FILE_REF, "generated", REPORT_PROJECT_KEY + ":" + REPORT_UNCHANGED_FILE_PATH, "File3.java", null);
  }

  @DataProvider
  public static Object[][] shortLivingBranchAndPullRequest() {
    return new Object[][] {{BranchType.SHORT}, {BranchType.PULL_REQUEST}};
  }

  @Test
  public void generate_keys_when_using_existing_branch() {
    ComponentDto projectDto = dbTester.components().insertMainBranch();
    ComponentDto branchDto = dbTester.components().insertProjectBranch(projectDto);
    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn(branchDto.getBranch());
    when(branch.isMain()).thenReturn(false);
    when(branch.isLegacyFeature()).thenReturn(false);
    when(branch.generateKey(any(), any())).thenReturn(branchDto.getDbKey());
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(Project.from(projectDto))
      .setBranch(branch);
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder,
      issueRelocationToRoot, reportModulesPath);
    reportReader.putComponent(component(ROOT_REF, PROJECT, branchDto.getKey()));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, branchDto.getDbKey(), branchDto.getKey(), analysisMetadataHolder.getProject().getName(), branchDto.uuid());
  }

  @Test
  public void generate_keys_when_using_main_branch() {
    Branch branch = new DefaultBranchImpl();
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(Project.from(newPrivateProjectDto(newOrganizationDto()).setDbKey(REPORT_PROJECT_KEY)))
      .setBranch(branch);
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder,
      issueRelocationToRoot, reportModulesPath);
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(component(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_PATH_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName(), null);
    verifyComponentMissingByRef(MODULE_REF);
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, REPORT_DIR_PATH_1);
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1, null);
  }

  @Test
  public void generate_keys_when_using_legacy_branch() {
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(Project.from(newPrivateProjectDto(newOrganizationDto()).setDbKey(REPORT_PROJECT_KEY)))
      .setBranch(new DefaultBranchImpl("origin/feature"));
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder,
      issueRelocationToRoot, reportModulesPath);
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(component(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_PATH_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY + ":origin/feature", analysisMetadataHolder.getProject().getName(), null);
    verifyComponentMissingByRef(MODULE_REF);
    verifyComponentByKey(REPORT_PROJECT_KEY + ":origin/feature:" + REPORT_DIR_PATH_1, REPORT_DIR_PATH_1);
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":origin/feature:" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1, null);
  }

  @Test
  public void compute_keys_and_uuids_on_project_having_module_and_directory() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF, DIR_REF_2));
    reportReader.putComponent(component(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_PATH_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));
    reportReader.putComponent(componentWithPath(DIR_REF_2, DIRECTORY, REPORT_DIR_PATH_2, FILE_2_REF));
    reportReader.putComponent(componentWithPath(FILE_2_REF, FILE, REPORT_FILE_PATH_2));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName());
    verifyComponentMissingByRef(MODULE_REF);
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, "dir1");
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1);
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_2, "dir2");
    verifyComponentByRef(FILE_2_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_2, "File2.java");
  }

  @Test
  public void compute_keys_and_uuids_on_multi_modules() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(component(MODULE_REF, MODULE, REPORT_MODULE_KEY, 100));
    reportReader.putComponent(component(100, MODULE, "SUB_MODULE_KEY", DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_PATH_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName());
    verifyComponentMissingByRef(MODULE_REF);
    verifyComponentMissingByKey(REPORT_MODULE_KEY);
    verifyComponentMissingByRef(100);
    verifyComponentMissingByKey("SUB_MODULE_KEY");
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, REPORT_DIR_PATH_1);
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1);
  }

  @Test
  public void set_no_base_project_snapshot_when_no_snapshot() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));
    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.isFirstAnalysis()).isTrue();
  }

  @Test
  public void set_no_base_project_snapshot_when_no_last_snapshot() {
    setAnalysisMetadataHolder();
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = insertComponent(newPrivateProjectDto(organizationDto, "ABCD").setDbKey(REPORT_PROJECT_KEY));
    insertSnapshot(newAnalysis(project).setLast(false));

    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));
    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.isFirstAnalysis()).isTrue();
  }

  @Test
  public void set_base_project_snapshot_when_last_snapshot_exist() {
    setAnalysisMetadataHolder();
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = insertComponent(newPrivateProjectDto(organizationDto, "ABCD").setDbKey(REPORT_PROJECT_KEY));
    insertSnapshot(newAnalysis(project).setLast(true));

    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));
    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.isFirstAnalysis()).isFalse();
  }

  private void verifyComponent(Component component, Component.Type type, @Nullable Integer componentRef, int size) {
    assertThat(component.getType()).isEqualTo(type);
    assertThat(component.getReportAttributes().getRef()).isEqualTo(componentRef);
    assertThat(component.getChildren()).hasSize(size);
  }

  private void verifyComponentByRef(int ref, String key, String shortName) {
    verifyComponentByRef(ref, key, key, shortName, null);
  }

  private void verifyComponentByRef(int ref, String key, String shortName, @Nullable String uuid) {
    verifyComponentByRef(ref, key, key, shortName, uuid);
  }

  private void verifyComponentByKey(String publicKey, String shortName) {
    verifyComponentByKey(publicKey, publicKey, shortName, null);
  }

  private void verifyComponentByKey(String publicKey, String key, String shortName) {
    verifyComponentByKey(publicKey, key, shortName, null);
  }

  private void verifyComponentByKey(String publicKey, String key, String shortName, @Nullable String uuid) {
    Map<String, Component> componentsByKey = indexAllComponentsInTreeByKey(treeRootHolder.getRoot());
    Component component = componentsByKey.get(publicKey);
    assertThat(component.getDbKey()).isEqualTo(key);
    assertThat(component.getReportAttributes().getRef()).isNull();
    assertThat(component.getKey()).isEqualTo(publicKey);
    assertThat(component.getShortName()).isEqualTo(shortName);
    if (uuid != null) {
      assertThat(component.getUuid()).isEqualTo(uuid);
    } else {
      assertThat(component.getUuid()).isNotNull();
    }
  }

  private void verifyComponentByRef(int ref, String key, String publicKey, String shortName, @Nullable String uuid) {
    Map<Integer, Component> componentsByRef = indexAllComponentsInTreeByRef(treeRootHolder.getRoot());
    Component component = componentsByRef.get(ref);
    assertThat(component.getDbKey()).isEqualTo(key);
    assertThat(component.getKey()).isEqualTo(publicKey);
    assertThat(component.getShortName()).isEqualTo(shortName);
    if (uuid != null) {
      assertThat(component.getUuid()).isEqualTo(uuid);
    } else {
      assertThat(component.getUuid()).isNotNull();
    }
  }

  private void verifyComponentMissingByRef(int ref) {
    Map<Integer, Component> componentsByRef = indexAllComponentsInTreeByRef(treeRootHolder.getRoot());
    assertThat(componentsByRef.get(ref)).isNull();
  }

  private void verifyComponentMissingByKey(String key) {
    Map<String, Component> componentsByKey = indexAllComponentsInTreeByKey(treeRootHolder.getRoot());
    assertThat(componentsByKey.get(key)).isNull();
  }

  private static ScannerReport.Component component(int componentRef, ComponentType componentType, String key, int... children) {
    return component(componentRef, componentType, key, null, children);
  }

  private static ScannerReport.Component componentWithPath(int componentRef, ComponentType componentType, String path, int... children) {
    return component(componentRef, componentType, REPORT_PROJECT_KEY + ":" + path, path, children);
  }

  private static ScannerReport.Component component(int componentRef, ComponentType componentType, String key, @Nullable String path, int... children) {
    ScannerReport.Component.Builder builder = ScannerReport.Component.newBuilder()
      .setType(componentType)
      .setRef(componentRef)
      .setStatus(FileStatus.UNAVAILABLE)
      .setLines(1)
      .setKey(key);
    if (path != null) {
      builder.setProjectRelativePath(path);
    }
    for (int child : children) {
      builder.addChildRef(child);
    }
    return builder.build();
  }

  private static Map<Integer, Component> indexAllComponentsInTreeByRef(Component root) {
    Map<Integer, Component> componentsByRef = new HashMap<>();
    feedComponentByRef(root, componentsByRef);
    return componentsByRef;
  }

  private static Map<String, Component> indexAllComponentsInTreeByKey(Component root) {
    Map<String, Component> componentsByKey = new HashMap<>();
    feedComponentByKey(root, componentsByKey);
    return componentsByKey;
  }

  private static void feedComponentByKey(Component component, Map<String, Component> map) {
    map.put(component.getKey(), component);
    for (Component child : component.getChildren()) {
      feedComponentByKey(child, map);
    }
  }

  private static void feedComponentByRef(Component component, Map<Integer, Component> map) {
    if (component.getReportAttributes().getRef() != null) {
      map.put(component.getReportAttributes().getRef(), component);
    }
    for (Component child : component.getChildren()) {
      feedComponentByRef(child, map);
    }
  }

  private ComponentDto insertComponent(ComponentDto component) {
    dbClient.componentDao().insert(dbTester.getSession(), component);
    dbTester.getSession().commit();
    return component;
  }

  private SnapshotDto insertSnapshot(SnapshotDto snapshot) {
    dbClient.snapshotDao().insert(dbTester.getSession(), snapshot);
    dbTester.getSession().commit();
    return snapshot;
  }

  private void setAnalysisMetadataHolder() {
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setBranch(new DefaultBranchImpl(null))
      .setProject(Project.from(newPrivateProjectDto(newOrganizationDto()).setDbKey(REPORT_PROJECT_KEY)));
  }

}
