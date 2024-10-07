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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;
import org.sonar.scanner.protocol.output.ScannerReport.Component.FileStatus;
import org.sonar.server.project.Project;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.FILE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.PROJECT;

@RunWith(DataProviderRunner.class)
public class BuildComponentTreeStepIT {
  private static final String NO_SCANNER_PROJECT_VERSION = null;
  private static final String NO_SCANNER_BUILD_STRING = null;

  private static final int ROOT_REF = 1;
  private static final int FILE_1_REF = 4;
  private static final int FILE_2_REF = 5;
  private static final int FILE_3_REF = 7;
  private static final int UNCHANGED_FILE_REF = 10;

  private static final String REPORT_PROJECT_KEY = "REPORT_PROJECT_KEY";
  private static final String REPORT_DIR_PATH_1 = "src/main/java/dir1";
  private static final String REPORT_FILE_PATH_1 = "src/main/java/dir1/File1.java";
  private static final String REPORT_FILE_NAME_1 = "File1.java";
  private static final String REPORT_DIR_PATH_2 = "src/main/java/dir2";
  private static final String REPORT_FILE_PATH_2 = "src/main/java/dir2/File2.java";
  private static final String REPORT_FILE_PATH_3 = "src/main/java/dir2/File3.java";
  private static final String REPORT_UNCHANGED_FILE_PATH = "src/main/File3.java";

  private static final long ANALYSIS_DATE = 123456789L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule().setMetadata(createReportMetadata(NO_SCANNER_PROJECT_VERSION, NO_SCANNER_BUILD_STRING));
  @Rule
  public MutableTreeRootHolderRule treeRootHolder = new MutableTreeRootHolderRule();
  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();

  private DbClient dbClient = dbTester.getDbClient();
  private BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder);

  @Test
  public void fails_if_root_component_does_not_exist_in_reportReader() {
    setAnalysisMetadataHolder();

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void verify_tree_is_correctly_built() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, FILE_1_REF, FILE_2_REF, FILE_3_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));
    reportReader.putComponent(componentWithPath(FILE_2_REF, FILE, REPORT_FILE_PATH_2));
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
  public void verify_tree_is_correctly_built_in_prs() {
    setAnalysisMetadataHolder(true);
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, FILE_1_REF, FILE_2_REF, FILE_3_REF, UNCHANGED_FILE_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));
    reportReader.putComponent(componentWithPath(FILE_2_REF, FILE, REPORT_FILE_PATH_2));
    reportReader.putComponent(componentWithPath(FILE_3_REF, FILE, REPORT_FILE_PATH_3));
    reportReader.putComponent(unchangedComponentWithPath(UNCHANGED_FILE_REF, FILE, REPORT_UNCHANGED_FILE_PATH));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    // modified root
    Component mRoot = treeRootHolder.getRoot();
    verifyComponent(mRoot, Component.Type.PROJECT, ROOT_REF, 1);

    Component mDir = mRoot.getChildren().get(0);
    assertThat(mDir.getName()).isEqualTo("src/main/java");
    verifyComponent(mDir, Component.Type.DIRECTORY, null, 2);

    Component mDir1 = mDir.getChildren().get(0);
    assertThat(mDir1.getName()).isEqualTo("src/main/java/dir1");
    verifyComponent(mDir1, Component.Type.DIRECTORY, null, 1);
    verifyComponent(mDir1.getChildren().get(0), Component.Type.FILE, FILE_1_REF, 0);

    Component mDir2 = mDir.getChildren().get(1);
    assertThat(mDir2.getName()).isEqualTo("src/main/java/dir2");
    verifyComponent(mDir2, Component.Type.DIRECTORY, null, 2);
    verifyComponent(mDir2.getChildren().get(0), Component.Type.FILE, FILE_2_REF, 0);
    verifyComponent(mDir2.getChildren().get(1), Component.Type.FILE, FILE_3_REF, 0);

    // root
    Component root = treeRootHolder.getReportTreeRoot();
    verifyComponent(root, Component.Type.PROJECT, ROOT_REF, 1);

    Component dir = root.getChildren().get(0);
    assertThat(dir.getName()).isEqualTo("src/main");
    verifyComponent(dir, Component.Type.DIRECTORY, null, 2);

    Component dir1 = dir.getChildren().get(0);
    assertThat(dir1.getName()).isEqualTo("src/main/java");
    verifyComponent(dir1, Component.Type.DIRECTORY, null, 2);
    verifyComponent(dir1.getChildren().get(0), Component.Type.DIRECTORY, null, 1);
    verifyComponent(dir1.getChildren().get(1), Component.Type.DIRECTORY, null, 2);

    Component dir2 = dir1.getChildren().get(0);
    assertThat(dir2.getName()).isEqualTo("src/main/java/dir1");
    verifyComponent(dir2, Component.Type.DIRECTORY, null, 1);
    verifyComponent(dir2.getChildren().get(0), Component.Type.FILE, FILE_1_REF, 0);

    Component dir3 = dir1.getChildren().get(1);
    assertThat(dir3.getName()).isEqualTo("src/main/java/dir2");
    verifyComponent(dir3, Component.Type.DIRECTORY, null, 2);
    verifyComponent(dir3.getChildren().get(0), Component.Type.FILE, FILE_2_REF, 0);
    verifyComponent(dir3.getChildren().get(1), Component.Type.FILE, FILE_3_REF, 0);

    context.getStatistics().assertValue("components", 7);
  }

  /**
   * SONAR-13262
   */
  @Test
  public void verify_tree_is_correctly_built_in_prs_with_repeated_names() {
    setAnalysisMetadataHolder(true);
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_PROJECT_KEY + "/file.js"));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    // modified root
    Component mRoot = treeRootHolder.getRoot();
    verifyComponent(mRoot, Component.Type.PROJECT, ROOT_REF, 1);

    Component dir = mRoot.getChildren().get(0);
    assertThat(dir.getName()).isEqualTo(REPORT_PROJECT_KEY);
    assertThat(dir.getShortName()).isEqualTo(REPORT_PROJECT_KEY);

    verifyComponent(dir, Component.Type.DIRECTORY, null, 1);
  }

  @Test
  public void compute_keys_and_uuids() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName());
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, REPORT_DIR_PATH_1);
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1);
  }

  @Test
  public void return_existing_uuids() {
    setAnalysisMetadataHolder();
    ComponentDto mainBranch = dbTester.components().insertPrivateProject("ABCD", p -> p.setKey(REPORT_PROJECT_KEY)).getMainBranchComponent();
    ComponentDto directory = newDirectory(mainBranch, "CDEF", REPORT_DIR_PATH_1);
    insertComponent(directory.setKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1));
    insertComponent(newFileDto(mainBranch, directory, "DEFG")
      .setKey(REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1)
      .setPath(REPORT_FILE_PATH_1));

    // new structure, without modules
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName(), mainBranch.uuid());
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, REPORT_DIR_PATH_1, "CDEF");
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1, "DEFG");
  }

  @Test
  public void generate_keys_when_using_new_branch() {
    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn("origin/feature");
    when(branch.isMain()).thenReturn(false);
    when(branch.generateKey(any(), any())).thenReturn("generated");
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(Project.from(newPrivateProjectDto().setKey(REPORT_PROJECT_KEY)))
      .setBranch(branch);
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder);
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, "generated", analysisMetadataHolder.getProject().getName(), null);
    verifyComponentByRef(FILE_1_REF, "generated", REPORT_FILE_NAME_1, null);
  }

  @Test
  public void generate_keys_when_using_existing_branch() {
    ComponentDto projectDto = dbTester.components().insertPublicProject().getMainBranchComponent();
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto componentDto = dbTester.components().insertProjectBranch(projectDto, b -> b.setKey(branchName));
    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn(branchName);
    when(branch.isMain()).thenReturn(false);
    when(branch.generateKey(any(), any())).thenReturn(componentDto.getKey());
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(Project.from(projectDto))
      .setBranch(branch);
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder);
    reportReader.putComponent(component(ROOT_REF, PROJECT, componentDto.getKey()));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, componentDto.getKey(), analysisMetadataHolder.getProject().getName(), componentDto.uuid());
  }

  @Test
  public void generate_keys_when_using_main_branch() {
    setAnalysisMetadataHolder();
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder);
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName(), null);
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, REPORT_DIR_PATH_1);
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1, null);
  }

  @Test
  public void compute_keys_and_uuids_on_project_having_module_and_directory() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, FILE_1_REF, FILE_2_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));
    reportReader.putComponent(componentWithPath(FILE_2_REF, FILE, REPORT_FILE_PATH_2));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName());
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_1, "dir1");
    verifyComponentByRef(FILE_1_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_1, REPORT_FILE_NAME_1);
    verifyComponentByKey(REPORT_PROJECT_KEY + ":" + REPORT_DIR_PATH_2, "dir2");
    verifyComponentByRef(FILE_2_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_PATH_2, "File2.java");
  }

  @Test
  public void compute_keys_and_uuids_on_multi_modules() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_PATH_1));

    underTest.execute(new TestComputationStepContext());

    verifyComponentByRef(ROOT_REF, REPORT_PROJECT_KEY, analysisMetadataHolder.getProject().getName());
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
    ComponentDto project = insertComponent(newPrivateProjectDto("ABCD").setKey(REPORT_PROJECT_KEY));
    insertSnapshot(newAnalysis(project).setLast(false));

    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));
    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.isFirstAnalysis()).isTrue();
  }

  @Test
  public void set_base_project_snapshot_when_last_snapshot_exist() {
    setAnalysisMetadataHolder();
    ComponentDto project = dbTester.components().insertPrivateProject("ABCD", p -> p.setKey(REPORT_PROJECT_KEY)).getMainBranchComponent();
    insertSnapshot(newAnalysis(project).setLast(true));

    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));
    underTest.execute(new TestComputationStepContext());

    assertThat(analysisMetadataHolder.isFirstAnalysis()).isFalse();
  }

  @Test
  public void set_projectVersion_to_not_provided_when_not_set_on_first_analysis() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));

    underTest.execute(new TestComputationStepContext());

    assertThat(treeRootHolder.getReportTreeRoot().getProjectAttributes().getProjectVersion()).isEqualTo("not provided");
  }

  @Test
  @UseDataProvider("oneParameterNullNonNullCombinations")
  public void set_projectVersion_to_previous_analysis_when_not_set(@Nullable String previousAnalysisProjectVersion) {
    setAnalysisMetadataHolder();
    ComponentDto project = dbTester.components().insertPrivateProject("ABCD", p -> p.setKey(REPORT_PROJECT_KEY)).getMainBranchComponent();
    insertSnapshot(newAnalysis(project).setProjectVersion(previousAnalysisProjectVersion).setLast(true));
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));

    underTest.execute(new TestComputationStepContext());

    String projectVersion = treeRootHolder.getReportTreeRoot().getProjectAttributes().getProjectVersion();
    if (previousAnalysisProjectVersion == null) {
      assertThat(projectVersion).isEqualTo("not provided");
    } else {
      assertThat(projectVersion).isEqualTo(previousAnalysisProjectVersion);
    }
  }

  @Test
  public void set_projectVersion_when_it_is_set_on_first_analysis() {
    String scannerProjectVersion = secure().nextAlphabetic(12);
    setAnalysisMetadataHolder();
    reportReader.setMetadata(createReportMetadata(scannerProjectVersion, NO_SCANNER_BUILD_STRING));
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));

    underTest.execute(new TestComputationStepContext());

    assertThat(treeRootHolder.getReportTreeRoot().getProjectAttributes().getProjectVersion())
      .isEqualTo(scannerProjectVersion);
  }

  @Test
  @UseDataProvider("oneParameterNullNonNullCombinations")
  public void set_projectVersion_when_it_is_set_on_later_analysis(@Nullable String previousAnalysisProjectVersion) {
    String scannerProjectVersion = secure().nextAlphabetic(12);
    setAnalysisMetadataHolder();
    reportReader.setMetadata(createReportMetadata(scannerProjectVersion, NO_SCANNER_BUILD_STRING));
    ComponentDto project = insertComponent(newPrivateProjectDto("ABCD").setKey(REPORT_PROJECT_KEY));
    insertSnapshot(newAnalysis(project).setProjectVersion(previousAnalysisProjectVersion).setLast(true));
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));

    underTest.execute(new TestComputationStepContext());

    assertThat(treeRootHolder.getReportTreeRoot().getProjectAttributes().getProjectVersion())
      .isEqualTo(scannerProjectVersion);
  }

  @Test
  @UseDataProvider("oneParameterNullNonNullCombinations")
  public void set_buildString(@Nullable String buildString) {
    String projectVersion = secure().nextAlphabetic(7);
    setAnalysisMetadataHolder();
    reportReader.setMetadata(createReportMetadata(projectVersion, buildString));
    reportReader.putComponent(component(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));

    underTest.execute(new TestComputationStepContext());

    assertThat(treeRootHolder.getReportTreeRoot().getProjectAttributes().getBuildString()).isEqualTo(Optional.ofNullable(buildString));
  }

  @DataProvider
  public static Object[][] oneParameterNullNonNullCombinations() {
    return new Object[][] {
      {null},
      {secure().nextAlphabetic(7)}
    };
  }

  private void verifyComponent(Component component, Component.Type type, @Nullable Integer componentRef, int size) {
    assertThat(component.getType()).isEqualTo(type);
    assertThat(component.getReportAttributes().getRef()).isEqualTo(componentRef);
    assertThat(component.getChildren()).hasSize(size);
  }

  private void verifyComponentByRef(int ref, String key, String shortName) {
    verifyComponentByRef(ref, key, shortName, null);
  }

  private void verifyComponentByKey(String key, String shortName) {
    verifyComponentByKey(key, shortName, null);
  }

  private void verifyComponentByKey(String key, String shortName, @Nullable String uuid) {
    Map<String, Component> componentsByKey = indexAllComponentsInTreeByKey(treeRootHolder.getRoot());
    Component component = componentsByKey.get(key);
    assertThat(component.getKey()).isEqualTo(key);
    assertThat(component.getReportAttributes().getRef()).isNull();
    assertThat(component.getShortName()).isEqualTo(shortName);
    if (uuid != null) {
      assertThat(component.getUuid()).isEqualTo(uuid);
    } else {
      assertThat(component.getUuid()).isNotNull();
    }
  }

  private void verifyComponentByRef(int ref, String key, String shortName, @Nullable String uuid) {
    Map<Integer, Component> componentsByRef = indexAllComponentsInTreeByRef(treeRootHolder.getRoot());
    Component component = componentsByRef.get(ref);
    assertThat(component.getKey()).isEqualTo(key);
    assertThat(component.getShortName()).isEqualTo(shortName);
    if (uuid != null) {
      assertThat(component.getUuid()).isEqualTo(uuid);
    } else {
      assertThat(component.getUuid()).isNotNull();
    }
  }

  private static ScannerReport.Component component(int componentRef, ComponentType componentType, String key, int... children) {
    return component(componentRef, componentType, key, FileStatus.CHANGED, null, children);
  }

  private static ScannerReport.Component unchangedComponentWithPath(int componentRef, ComponentType componentType, String path, int... children) {
    return component(componentRef, componentType, REPORT_PROJECT_KEY + ":" + path, FileStatus.SAME, path, children);
  }

  private static ScannerReport.Component componentWithPath(int componentRef, ComponentType componentType, String path, int... children) {
    return component(componentRef, componentType, REPORT_PROJECT_KEY + ":" + path, FileStatus.CHANGED, path, children);
  }

  private static ScannerReport.Component component(int componentRef, ComponentType componentType, String key, FileStatus status, @Nullable String path, int... children) {
    ScannerReport.Component.Builder builder = ScannerReport.Component.newBuilder()
      .setType(componentType)
      .setRef(componentRef)
      .setName(key)
      .setStatus(status)
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
    return dbTester.components().insertComponent(component);
  }

  private SnapshotDto insertSnapshot(SnapshotDto snapshot) {
    dbClient.snapshotDao().insert(dbTester.getSession(), snapshot);
    dbTester.getSession().commit();
    return snapshot;
  }

  private void setAnalysisMetadataHolder() {
    setAnalysisMetadataHolder(false);
  }

  private void setAnalysisMetadataHolder(boolean isPr) {
    Branch branch = isPr ? new PrBranch(DEFAULT_MAIN_BRANCH_NAME) : new DefaultBranchImpl(DEFAULT_MAIN_BRANCH_NAME);
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setBranch(branch)
      .setProject(Project.from(newPrivateProjectDto().setKey(REPORT_PROJECT_KEY).setName(REPORT_PROJECT_KEY)));
  }

  public static ScannerReport.Metadata createReportMetadata(@Nullable String projectVersion, @Nullable String buildString) {
    ScannerReport.Metadata.Builder builder = ScannerReport.Metadata.newBuilder()
      .setProjectKey(REPORT_PROJECT_KEY)
      .setRootComponentRef(ROOT_REF);
    ofNullable(projectVersion).ifPresent(builder::setProjectVersion);
    ofNullable(buildString).ifPresent(builder::setBuildString);
    return builder.build();
  }

  private static class PrBranch extends DefaultBranchImpl {
    public PrBranch(String branch) {
      super(branch);
    }

    @Override
    public BranchType getType() {
      return BranchType.PULL_REQUEST;
    }
  }
}
