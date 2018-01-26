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
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;
import org.sonar.scanner.protocol.output.ScannerReport.Component.FileStatus;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.computation.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Project;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.server.computation.task.projectanalysis.component.MutableTreeRootHolderRule;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.DIRECTORY;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.FILE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.MODULE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.PROJECT;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.UNRECOGNIZED;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.UNSET;

@RunWith(DataProviderRunner.class)
public class BuildComponentTreeStepTest {

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 2;
  private static final int DIR_REF_1 = 3;
  private static final int FILE_1_REF = 4;
  private static final int FILE_2_REF = 5;
  private static final int DIR_REF_2 = 6;
  private static final int FILE_3_REF = 7;

  private static final String REPORT_PROJECT_KEY = "REPORT_PROJECT_KEY";
  private static final String REPORT_MODULE_KEY = "MODULE_KEY";
  private static final String REPORT_DIR_KEY_1 = "src/main/java/dir1";
  private static final String REPORT_FILE_KEY_1 = "src/main/java/dir1/File1.java";
  private static final String REPORT_DIR_KEY_2 = "src/main/java/dir2";
  private static final String REPORT_FILE_KEY_2 = "src/main/java/dir2/File2.java";

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

  private DbClient dbClient = dbTester.getDbClient();
  private BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder);

  @Test(expected = NullPointerException.class)
  public void fails_if_root_component_does_not_exist_in_reportReader() {
    setAnalysisMetadataHolder();

    underTest.execute();
  }

  @DataProvider
  public static Object[][] allComponentTypes() {
    Object[][] res = new Object[ComponentType.values().length - 2][1];
    int i = 0;
    for (ComponentType componentType : from(asList(ComponentType.values())).filter(not(in(asList(UNSET, UNRECOGNIZED))))) {
      res[i][0] = componentType;
      i++;
    }
    return res;
  }

  @Test
  @UseDataProvider("allComponentTypes")
  public void verify_ref_and_type(ComponentType componentType) {
    setAnalysisMetadataHolder();
    int componentRef = 1;
    reportReader.putComponent(component(componentRef, componentType));

    underTest.execute();

    Component root = treeRootHolder.getRoot();
    assertThat(root).isNotNull();
    assertThat(root.getType()).isEqualTo(Component.Type.valueOf(componentType.name()));
    assertThat(root.getReportAttributes().getRef()).isEqualTo(ROOT_REF);
    assertThat(root.getChildren()).isEmpty();
  }

  @Test
  public void verify_tree_is_correctly_built() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(component(ROOT_REF, PROJECT, MODULE_REF));
    reportReader.putComponent(component(MODULE_REF, MODULE, DIR_REF_1, DIR_REF_2));
    reportReader.putComponent(component(DIR_REF_1, DIRECTORY, FILE_1_REF, FILE_2_REF));
    reportReader.putComponent(component(FILE_1_REF, FILE));
    reportReader.putComponent(component(FILE_2_REF, FILE));
    reportReader.putComponent(component(DIR_REF_2, DIRECTORY, FILE_3_REF));
    reportReader.putComponent(component(FILE_3_REF, FILE));

    underTest.execute();

    Component root = treeRootHolder.getRoot();
    assertThat(root).isNotNull();
    verifyComponent(root, Component.Type.PROJECT, ROOT_REF, 1);
    Component module = root.getChildren().iterator().next();
    verifyComponent(module, Component.Type.MODULE, MODULE_REF, 2);
    Component dir1 = module.getChildren().get(0);
    verifyComponent(dir1, Component.Type.DIRECTORY, DIR_REF_1, 2);
    verifyComponent(dir1.getChildren().get(0), Component.Type.FILE, FILE_1_REF, 0);
    verifyComponent(dir1.getChildren().get(1), Component.Type.FILE, FILE_2_REF, 0);
    Component dir2 = module.getChildren().get(1);
    verifyComponent(dir2, Component.Type.DIRECTORY, DIR_REF_2, 1);
    verifyComponent(dir2.getChildren().iterator().next(), Component.Type.FILE, FILE_3_REF, 0);
  }

  @Test
  public void compute_keys_and_uuids() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(componentWithKey(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_KEY_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_KEY_1));

    underTest.execute();

    verifyComponent(ROOT_REF, REPORT_PROJECT_KEY);
    verifyComponent(MODULE_REF, REPORT_MODULE_KEY);
    verifyComponent(DIR_REF_1, REPORT_MODULE_KEY + ":" + REPORT_DIR_KEY_1);
    verifyComponent(FILE_1_REF, REPORT_MODULE_KEY + ":" + REPORT_FILE_KEY_1);
  }

  @Test
  public void return_existing_uuids() {
    setAnalysisMetadataHolder();
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = insertComponent(newPrivateProjectDto(organizationDto, "ABCD").setDbKey(REPORT_PROJECT_KEY));
    ComponentDto module = insertComponent(newModuleDto("BCDE", project).setDbKey(REPORT_MODULE_KEY));
    ComponentDto directory = newDirectory(module, "CDEF", REPORT_DIR_KEY_1);
    insertComponent(directory.setDbKey(REPORT_MODULE_KEY + ":" + REPORT_DIR_KEY_1));
    insertComponent(newFileDto(module, directory, "DEFG").setDbKey(REPORT_MODULE_KEY + ":" + REPORT_FILE_KEY_1));

    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(componentWithKey(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_KEY_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_KEY_1));

    underTest.execute();

    verifyComponent(ROOT_REF, REPORT_PROJECT_KEY, "ABCD");
    verifyComponent(MODULE_REF, REPORT_MODULE_KEY, "BCDE");
    verifyComponent(DIR_REF_1, REPORT_MODULE_KEY + ":" + REPORT_DIR_KEY_1, "CDEF");
    verifyComponent(FILE_1_REF, REPORT_MODULE_KEY + ":" + REPORT_FILE_KEY_1, "DEFG");
  }

  @Test
  public void generate_keys_when_using_branch() {
    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn("origin/feature");
    when(branch.isMain()).thenReturn(false);
    when(branch.isLegacyFeature()).thenReturn(false);
    when(branch.generateKey(any(), any())).thenReturn("generated");
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(new Project("U1", REPORT_PROJECT_KEY, REPORT_PROJECT_KEY))
      .setBranch(branch);
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder);
    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(componentWithKey(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_KEY_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_KEY_1));

    underTest.execute();

    verifyComponent(ROOT_REF, "generated", REPORT_PROJECT_KEY, null);
    verifyComponent(MODULE_REF, "generated", REPORT_MODULE_KEY, null);
    verifyComponent(DIR_REF_1, "generated", REPORT_MODULE_KEY + ":" + REPORT_DIR_KEY_1, null);
    verifyComponent(FILE_1_REF, "generated", REPORT_MODULE_KEY + ":" + REPORT_FILE_KEY_1, null);
  }

  @Test
  public void generate_keys_when_using_main_branch() {
    Branch branch = new DefaultBranchImpl();
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(new Project("U1", REPORT_PROJECT_KEY, REPORT_PROJECT_KEY))
      .setBranch(branch);
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder);
    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(componentWithKey(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_KEY_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_KEY_1));

    underTest.execute();

    verifyComponent(ROOT_REF, REPORT_PROJECT_KEY, REPORT_PROJECT_KEY, null);
    verifyComponent(MODULE_REF, REPORT_MODULE_KEY, REPORT_MODULE_KEY, null);
    verifyComponent(DIR_REF_1, REPORT_MODULE_KEY + ":" + REPORT_DIR_KEY_1, REPORT_MODULE_KEY + ":" + REPORT_DIR_KEY_1, null);
    verifyComponent(FILE_1_REF, REPORT_MODULE_KEY + ":" + REPORT_FILE_KEY_1, REPORT_MODULE_KEY + ":" + REPORT_FILE_KEY_1, null);
  }

  @Test
  public void generate_keys_when_using_legacy_branch() {
    analysisMetadataHolder.setRootComponentRef(ROOT_REF)
      .setAnalysisDate(ANALYSIS_DATE)
      .setProject(new Project("U1", REPORT_PROJECT_KEY, REPORT_PROJECT_KEY))
      .setBranch(new DefaultBranchImpl("origin/feature"));
    BuildComponentTreeStep underTest = new BuildComponentTreeStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder);
    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(componentWithKey(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_KEY_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_KEY_1));

    underTest.execute();

    verifyComponent(ROOT_REF, REPORT_PROJECT_KEY + ":origin/feature", null);
    verifyComponent(MODULE_REF, REPORT_MODULE_KEY + ":origin/feature", null);
    verifyComponent(DIR_REF_1, REPORT_MODULE_KEY + ":origin/feature:" + REPORT_DIR_KEY_1, null);
    verifyComponent(FILE_1_REF, REPORT_MODULE_KEY + ":origin/feature:" + REPORT_FILE_KEY_1, null);
  }

  @Test
  public void compute_keys_and_uuids_on_project_having_module_and_directory() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF, DIR_REF_2));
    reportReader.putComponent(componentWithKey(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_KEY_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_KEY_1));
    reportReader.putComponent(componentWithPath(DIR_REF_2, DIRECTORY, REPORT_DIR_KEY_2, FILE_2_REF));
    reportReader.putComponent(componentWithPath(FILE_2_REF, FILE, REPORT_FILE_KEY_2));

    underTest.execute();

    verifyComponent(ROOT_REF, REPORT_PROJECT_KEY);
    verifyComponent(MODULE_REF, REPORT_MODULE_KEY);
    verifyComponent(DIR_REF_1, REPORT_MODULE_KEY + ":" + REPORT_DIR_KEY_1);
    verifyComponent(FILE_1_REF, REPORT_MODULE_KEY + ":" + REPORT_FILE_KEY_1);
    verifyComponent(DIR_REF_2, REPORT_PROJECT_KEY + ":" + REPORT_DIR_KEY_2);
    verifyComponent(FILE_2_REF, REPORT_PROJECT_KEY + ":" + REPORT_FILE_KEY_2);
  }

  @Test
  public void compute_keys_and_uuids_on_multi_modules() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(componentWithKey(MODULE_REF, MODULE, REPORT_MODULE_KEY, 100));
    reportReader.putComponent(componentWithKey(100, MODULE, "SUB_MODULE_KEY", DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_KEY_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_KEY_1));

    underTest.execute();

    verifyComponent(ROOT_REF, REPORT_PROJECT_KEY);
    verifyComponent(MODULE_REF, REPORT_MODULE_KEY);
    verifyComponent(100, "SUB_MODULE_KEY");
    verifyComponent(DIR_REF_1, "SUB_MODULE_KEY" + ":" + REPORT_DIR_KEY_1);
    verifyComponent(FILE_1_REF, "SUB_MODULE_KEY" + ":" + REPORT_FILE_KEY_1);
  }

  @Test
  public void return_existing_uuids_when_components_were_removed() {
    setAnalysisMetadataHolder();
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = insertComponent(newPrivateProjectDto(organizationDto, "ABCD").setDbKey(REPORT_PROJECT_KEY));
    ComponentDto removedModule = insertComponent(newModuleDto("BCDE", project).setDbKey(REPORT_MODULE_KEY).setEnabled(false));
    ComponentDto removedDirectory = insertComponent(newDirectory(removedModule, "CDEF", REPORT_DIR_KEY_1).setDbKey(REPORT_MODULE_KEY + ":" + REPORT_DIR_KEY_1).setEnabled(false));
    insertComponent(newFileDto(removedModule, removedDirectory, "DEFG").setDbKey(REPORT_MODULE_KEY + ":" + REPORT_FILE_KEY_1).setEnabled(false));

    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY, MODULE_REF));
    reportReader.putComponent(componentWithKey(MODULE_REF, MODULE, REPORT_MODULE_KEY, DIR_REF_1));
    reportReader.putComponent(componentWithPath(DIR_REF_1, DIRECTORY, REPORT_DIR_KEY_1, FILE_1_REF));
    reportReader.putComponent(componentWithPath(FILE_1_REF, FILE, REPORT_FILE_KEY_1));

    underTest.execute();

    verifyComponent(ROOT_REF, REPORT_PROJECT_KEY, "ABCD");

    // No new UUID is generated on removed components
    verifyComponent(MODULE_REF, REPORT_MODULE_KEY, "BCDE");
    verifyComponent(DIR_REF_1, REPORT_MODULE_KEY + ":" + REPORT_DIR_KEY_1, "CDEF");
    verifyComponent(FILE_1_REF, REPORT_MODULE_KEY + ":" + REPORT_FILE_KEY_1, "DEFG");
  }

  @Test
  public void set_no_base_project_snapshot_when_no_snapshot() {
    setAnalysisMetadataHolder();
    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));
    underTest.execute();

    assertThat(analysisMetadataHolder.isFirstAnalysis()).isTrue();
  }

  @Test
  public void set_no_base_project_snapshot_when_no_last_snapshot() {
    setAnalysisMetadataHolder();
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = insertComponent(newPrivateProjectDto(organizationDto, "ABCD").setDbKey(REPORT_PROJECT_KEY));
    insertSnapshot(newAnalysis(project).setLast(false));

    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));
    underTest.execute();

    assertThat(analysisMetadataHolder.isFirstAnalysis()).isTrue();
  }

  @Test
  public void set_base_project_snapshot_when_last_snapshot_exist() {
    setAnalysisMetadataHolder();
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = insertComponent(newPrivateProjectDto(organizationDto, "ABCD").setDbKey(REPORT_PROJECT_KEY));
    insertSnapshot(newAnalysis(project).setLast(true));

    reportReader.putComponent(componentWithKey(ROOT_REF, PROJECT, REPORT_PROJECT_KEY));
    underTest.execute();

    assertThat(analysisMetadataHolder.isFirstAnalysis()).isFalse();
  }

  private void verifyComponent(Component component, Component.Type type, int componentRef, int size) {
    assertThat(component.getType()).isEqualTo(type);
    assertThat(component.getReportAttributes().getRef()).isEqualTo(componentRef);
    assertThat(component.getChildren()).hasSize(size);
  }

  private void verifyComponent(int ref, String key) {
    verifyComponent(ref, key, key, null);
  }

  private void verifyComponent(int ref, String key, @Nullable String uuid) {
    verifyComponent(ref, key, key, uuid);
  }

  private void verifyComponent(int ref, String key, String publicKey, @Nullable String uuid) {
    Map<Integer, Component> componentsByRef = indexAllComponentsInTreeByRef(treeRootHolder.getRoot());
    Component component = componentsByRef.get(ref);
    assertThat(component.getKey()).isEqualTo(key);
    assertThat(component.getPublicKey()).isEqualTo(publicKey);
    if (uuid != null) {
      assertThat(component.getUuid()).isEqualTo(uuid);
    } else {
      assertThat(component.getUuid()).isNotNull();
    }
  }

  private static ScannerReport.Component component(int componentRef, ComponentType componentType, int... children) {
    return component(componentRef, componentType, null, null, children);
  }

  private static ScannerReport.Component componentWithKey(int componentRef, ComponentType componentType, String key, int... children) {
    return component(componentRef, componentType, key, null, children);
  }

  private static ScannerReport.Component componentWithPath(int componentRef, ComponentType componentType, String path, int... children) {
    return component(componentRef, componentType, null, path, children);
  }

  private static ScannerReport.Component component(int componentRef, ComponentType componentType, @Nullable String key, @Nullable String path, int... children) {
    ScannerReport.Component.Builder builder = ScannerReport.Component.newBuilder()
      .setType(componentType)
      .setRef(componentRef)
      .setStatus(FileStatus.UNAVAILABLE)
      .setLines(1);
    if (key != null) {
      builder.setKey(key);
    }
    if (path != null) {
      builder.setPath(path);
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

  private static void feedComponentByRef(Component component, Map<Integer, Component> map) {
    map.put(component.getReportAttributes().getRef(), component);
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
      .setProject(new Project("U1", REPORT_PROJECT_KEY, REPORT_PROJECT_KEY));
  }

}
