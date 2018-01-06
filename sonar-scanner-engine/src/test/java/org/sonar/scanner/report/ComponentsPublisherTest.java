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
package org.sonar.scanner.report;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.utils.DateUtils;
import org.sonar.scanner.ProjectAnalysisInfo;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component;
import org.sonar.scanner.protocol.output.ScannerReport.Component.FileStatus;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.DefaultComponentTree;
import org.sonar.scanner.scan.DefaultInputModuleHierarchy;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.batch.fs.internal.TestInputFileBuilder.*;

public class ComponentsPublisherTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultComponentTree tree;
  private InputModuleHierarchy moduleHierarchy;
  private File outputDir;
  private ScannerReportWriter writer;
  private ScannerReportReader reader;
  private BranchConfiguration branchConfiguration;

  @Before
  public void setUp() throws IOException {
    branchConfiguration = mock(BranchConfiguration.class);
    tree = new DefaultComponentTree();
    outputDir = temp.newFolder();
    writer = new ScannerReportWriter(outputDir);
    reader = new ScannerReportReader(outputDir);
  }

  private void writeIssue(int componentId) {
    writer.writeComponentIssues(componentId, Collections.singleton(ScannerReport.Issue.newBuilder().build()));
  }

  @Test
  public void add_components_to_report() throws Exception {
    ProjectAnalysisInfo projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    when(projectAnalysisInfo.analysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setName("Root project")
      .setDescription("Root description")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder());
    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    Path moduleBaseDir = temp.newFolder().toPath();
    ProjectDefinition module1Def = ProjectDefinition.create()
      .setKey("module1")
      .setName("Module1")
      .setDescription("Module description")
      .setBaseDir(moduleBaseDir.toFile())
      .setWorkDir(temp.newFolder());
    rootDef.addSubProject(module1Def);

    DefaultInputModule module1 = new DefaultInputModule(module1Def, 2);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.children(root)).thenReturn(Collections.singleton(module1));
    when(moduleHierarchy.parent(module1)).thenReturn(root);
    tree.index(module1, root);

    DefaultInputDir dir = new DefaultInputDir("module1", "src", 3)
      .setModuleBaseDir(moduleBaseDir);
    tree.index(dir, module1);

    DefaultInputDir dir2 = new DefaultInputDir("module1", "src2", 17)
      .setModuleBaseDir(moduleBaseDir);
    tree.index(dir2, module1);

    DefaultInputFile file = new TestInputFileBuilder("module1", "src/Foo.java", 4).setLines(2).setStatus(InputFile.Status.SAME).build();
    tree.index(file, dir);

    DefaultInputFile file18 = new TestInputFileBuilder("module1", "src2/Foo.java", 18).setLines(2).setStatus(InputFile.Status.SAME).build();
    tree.index(file18, dir2);

    DefaultInputFile file2 = new TestInputFileBuilder("module1", "src/Foo2.java", 5).setPublish(false).setLines(2).build();
    tree.index(file2, dir);

    DefaultInputFile fileWithoutLang = new TestInputFileBuilder("module1", "src/make", 6).setLines(10).setStatus(InputFile.Status.CHANGED).build();
    tree.index(fileWithoutLang, dir);

    DefaultInputFile testFile = new TestInputFileBuilder("module1", "test/FooTest.java", 7).setType(Type.TEST).setStatus(InputFile.Status.ADDED).setLines(4).build();
    tree.index(testFile, dir);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree, branchConfiguration);
    publisher.publish(writer);

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 2)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 3)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 4)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 6)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 7)).isTrue();

    // not marked for publishing
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 5)).isFalse();
    // no such reference
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 8)).isFalse();

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    Component rootProtobuf = reader.readComponent(1);
    assertThat(rootProtobuf.getKey()).isEqualTo("foo");
    assertThat(rootProtobuf.getDescription()).isEqualTo("Root description");
    assertThat(rootProtobuf.getVersion()).isEqualTo("1.0");
    assertThat(rootProtobuf.getLinkCount()).isEqualTo(0);

    Component module1Protobuf = reader.readComponent(2);
    assertThat(module1Protobuf.getKey()).isEqualTo("module1");
    assertThat(module1Protobuf.getDescription()).isEqualTo("Module description");
    assertThat(module1Protobuf.getVersion()).isEqualTo("1.0");

    assertThat(reader.readComponent(4).getStatus()).isEqualTo(FileStatus.SAME);
    assertThat(reader.readComponent(6).getStatus()).isEqualTo(FileStatus.CHANGED);
    assertThat(reader.readComponent(7).getStatus()).isEqualTo(FileStatus.ADDED);
  }

  @Test
  public void should_set_modified_name_with_branch() throws IOException {
    ProjectAnalysisInfo projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    when(projectAnalysisInfo.analysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setDescription("Root description")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder())
      .setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "my_branch");

    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree, branchConfiguration);
    publisher.publish(writer);
    Component rootProtobuf = reader.readComponent(1);
    assertThat(rootProtobuf.getKey()).isEqualTo("foo");
    assertThat(rootProtobuf.getName()).isEqualTo("foo my_branch");
  }

  @Test
  public void should_skip_dir_without_published_files() throws IOException {
    ProjectAnalysisInfo projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    when(projectAnalysisInfo.analysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    Path moduleBaseDir = temp.newFolder().toPath();
    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setName("Root project")
      .setDescription("Root description")
      .setBaseDir(moduleBaseDir.toFile())
      .setWorkDir(temp.newFolder());
    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.children(root)).thenReturn(Collections.emptyList());

    // dir with files
    DefaultInputDir dir = new DefaultInputDir("module1", "src", 2)
      .setModuleBaseDir(moduleBaseDir);
    tree.index(dir, root);

    // dir without files and issues
    DefaultInputDir dir2 = new DefaultInputDir("module1", "src2", 3)
      .setModuleBaseDir(moduleBaseDir);
    tree.index(dir2, root);

    // dir without files but has issues
    DefaultInputDir dir3 = new DefaultInputDir("module1", "src3", 4)
      .setModuleBaseDir(moduleBaseDir);
    tree.index(dir3, root);
    writeIssue(4);

    DefaultInputFile file = new TestInputFileBuilder("module1", "src/Foo.java", 5).setLines(2).setStatus(InputFile.Status.SAME).build();
    tree.index(file, dir);

    DefaultInputFile file2 = new TestInputFileBuilder("module1", "src2/Foo2.java", 6).setPublish(false).setLines(2).build();
    tree.index(file2, dir2);

    DefaultInputFile file3 = new TestInputFileBuilder("module1", "src2/Foo3.java", 7).setPublish(false).setLines(2).build();
    tree.index(file3, dir3);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree, branchConfiguration);
    publisher.publish(writer);

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 2)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 5)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 4)).isTrue();

    // file was not marked for publishing and directory doesn't contain issues, so directory won't be included as well
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 3)).isFalse();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 6)).isFalse();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 7)).isFalse();
  }

  @Test
  public void should_skip_empty_modules_for_short_living_branches() throws IOException {
    ProjectAnalysisInfo projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    when(projectAnalysisInfo.analysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setName("Root project")
      .setDescription("Root description")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder());
    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    ProjectDefinition emptyModuleDef = ProjectDefinition.create()
      .setKey("modEmpty")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setName("Empty module")
      .setDescription("Empty module")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder());
    DefaultInputModule emptyModule = new DefaultInputModule(emptyModuleDef, 2);

    ProjectDefinition notEmptyModuleDef = ProjectDefinition.create()
      .setKey("modNotEmpty")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setName("Module")
      .setDescription("Module")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder());
    DefaultInputModule notEmptyModule = new DefaultInputModule(notEmptyModuleDef, 3);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.isRoot(root)).thenReturn(true);
    when(moduleHierarchy.children(root)).thenReturn(Arrays.asList(emptyModule, notEmptyModule));
    when(moduleHierarchy.children(emptyModule)).thenReturn(Collections.emptyList());
    when(moduleHierarchy.children(notEmptyModule)).thenReturn(Collections.emptyList());
    when(branchConfiguration.branchType()).thenReturn(BranchType.SHORT);

    // dir with files
    DefaultInputDir dir = new DefaultInputDir("modNotEmpty", "src", 4);
    tree.index(dir, notEmptyModule);

    // Only an unchanged file, so module should also be skipped
    DefaultInputFile file = new TestInputFileBuilder("modNotEmpty", "src/Foo.java", 5).setLines(2).setStatus(InputFile.Status.SAME).build();
    tree.index(file, dir);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree, branchConfiguration);
    publisher.publish(writer);

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 2)).isFalse();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 3)).isFalse();
  }

  @Test
  public void skip_unchanged_components_in_short_branches() throws IOException {
    when(branchConfiguration.isShortLivingBranch()).thenReturn(true);
    ProjectAnalysisInfo projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    when(projectAnalysisInfo.analysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    Path moduleBaseDir = temp.newFolder().toPath();
    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setName("Root project")
      .setDescription("Root description")
      .setBaseDir(moduleBaseDir.toFile())
      .setWorkDir(temp.newFolder());
    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.children(root)).thenReturn(Collections.emptyList());

    // dir with changed files
    DefaultInputDir dir = new DefaultInputDir("module1", "src", 2)
      .setModuleBaseDir(moduleBaseDir);
    tree.index(dir, root);

    // dir without changed files or issues
    DefaultInputDir dir2 = new DefaultInputDir("module1", "src2", 3)
      .setModuleBaseDir(moduleBaseDir);
    tree.index(dir2, root);

    // dir without changed files but has issues
    DefaultInputDir dir3 = new DefaultInputDir("module1", "src3", 4)
      .setModuleBaseDir(moduleBaseDir);
    tree.index(dir3, root);
    writeIssue(4);

    DefaultInputFile file = new TestInputFileBuilder("module1", "src/Foo.java", 5)
      .setLines(2)
      .setPublish(true)
      .setStatus(InputFile.Status.ADDED)
      .build();
    tree.index(file, dir);

    DefaultInputFile file2 = new TestInputFileBuilder("module1", "src2/Foo2.java", 6)
      .setPublish(true)
      .setStatus(InputFile.Status.SAME)
      .setLines(2)
      .build();
    tree.index(file2, dir2);

    DefaultInputFile file3 = new TestInputFileBuilder("module1", "src3/Foo3.java", 7)
      .setPublish(true)
      .setStatus(InputFile.Status.SAME)
      .setLines(2)
      .build();
    tree.index(file3, dir3);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree, branchConfiguration);
    publisher.publish(writer);

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 2)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 4)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 5)).isTrue();

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 3)).isFalse();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 6)).isFalse();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 7)).isFalse();
  }

  @Test
  public void add_components_without_version_and_name() throws IOException {
    ProjectAnalysisInfo projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    when(projectAnalysisInfo.analysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setDescription("Root description")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder());
    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    Path moduleBaseDir = temp.newFolder().toPath();
    ProjectDefinition module1Def = ProjectDefinition.create()
      .setKey("module1")
      .setDescription("Module description")
      .setBaseDir(moduleBaseDir.toFile())
      .setWorkDir(temp.newFolder());
    rootDef.addSubProject(module1Def);
    DefaultInputModule module1 = new DefaultInputModule(module1Def, 2);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.children(root)).thenReturn(Collections.singleton(module1));
    tree.index(module1, root);

    DefaultInputDir dir = new DefaultInputDir("module1", "src", 3)
      .setModuleBaseDir(moduleBaseDir);
    tree.index(dir, module1);

    DefaultInputFile file = new TestInputFileBuilder("module1", "src/Foo.java", 4).setLines(2).setStatus(InputFile.Status.SAME).build();
    tree.index(file, dir);

    DefaultInputFile fileWithoutLang = new TestInputFileBuilder("module1", "src/make", 5).setLines(10).setStatus(InputFile.Status.SAME).build();
    tree.index(fileWithoutLang, dir);

    DefaultInputFile testFile = new TestInputFileBuilder("module1", "test/FooTest.java", 6).setType(Type.TEST).setStatus(InputFile.Status.SAME).setLines(4).build();
    tree.index(testFile, dir);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree, branchConfiguration);
    publisher.publish(writer);

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 2)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 3)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 4)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 5)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 6)).isTrue();

    // no such reference
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 7)).isFalse();

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    Component rootProtobuf = reader.readComponent(1);
    assertThat(rootProtobuf.getKey()).isEqualTo("foo");
    assertThat(rootProtobuf.getName()).isEqualTo("");
    assertThat(rootProtobuf.getDescription()).isEqualTo("Root description");
    assertThat(rootProtobuf.getVersion()).isEqualTo("");
    assertThat(rootProtobuf.getLinkCount()).isEqualTo(0);

    Component module1Protobuf = reader.readComponent(2);
    assertThat(module1Protobuf.getKey()).isEqualTo("module1");
    assertThat(module1Protobuf.getName()).isEqualTo("");
    assertThat(module1Protobuf.getDescription()).isEqualTo("Module description");
    assertThat(module1Protobuf.getVersion()).isEqualTo("");
  }

  @Test
  public void add_components_with_links_and_branch() throws Exception {
    ProjectAnalysisInfo projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    when(projectAnalysisInfo.analysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "my_branch")
      .setName("Root project")
      .setProperty(CoreProperties.LINKS_HOME_PAGE, "http://home")
      .setDescription("Root description")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder());
    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    Path moduleBaseDir = temp.newFolder().toPath();
    ProjectDefinition module1Def = ProjectDefinition.create()
      .setKey("module1")
      .setName("Module1")
      .setProperty(CoreProperties.LINKS_CI, "http://ci")
      .setDescription("Module description")
      .setBaseDir(moduleBaseDir.toFile())
      .setWorkDir(temp.newFolder());
    rootDef.addSubProject(module1Def);
    DefaultInputModule module1 = new DefaultInputModule(module1Def, 2);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.children(root)).thenReturn(Collections.singleton(module1));
    when(moduleHierarchy.parent(module1)).thenReturn(root);
    tree.index(module1, root);

    DefaultInputDir dir = new DefaultInputDir("module1", "src", 3)
      .setModuleBaseDir(moduleBaseDir);
    tree.index(dir, module1);

    DefaultInputFile file = new TestInputFileBuilder("module1", "src/Foo.java", 4).setLines(2).setStatus(InputFile.Status.SAME).build();
    tree.index(file, dir);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree, branchConfiguration);
    publisher.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    Component rootProtobuf = reader.readComponent(1);
    assertThat(rootProtobuf.getVersion()).isEqualTo("1.0");
    assertThat(rootProtobuf.getLinkCount()).isEqualTo(1);
    assertThat(rootProtobuf.getLink(0).getType()).isEqualTo(ComponentLinkType.HOME);
    assertThat(rootProtobuf.getLink(0).getHref()).isEqualTo("http://home");

    Component module1Protobuf = reader.readComponent(2);
    assertThat(module1Protobuf.getVersion()).isEqualTo("1.0");
    assertThat(module1Protobuf.getLinkCount()).isEqualTo(1);
    assertThat(module1Protobuf.getLink(0).getType()).isEqualTo(ComponentLinkType.CI);
    assertThat(module1Protobuf.getLink(0).getHref()).isEqualTo("http://ci");
  }

  @Test
  public void add_components_with_correct_project_relative_path() throws Exception {
    Map<DefaultInputModule, DefaultInputModule> parents = new HashMap<>();

    DefaultInputModule root = newDefaultInputModule("foo", temp.newFolder());

    DefaultInputFile file = newDefaultInputFile(root.getBaseDir(), root, "Foo.java");
    tree.index(file, root);

    DefaultInputDir dir1 = newDefaultInputDir(root, "dir1");
    tree.index(dir1, root);

    DefaultInputFile dir1_file = newDefaultInputFile(root.getBaseDir(), root, "dir1/Foo.java");
    tree.index(dir1_file, dir1);

    DefaultInputDir dir1_dir1 = newDefaultInputDir(root, "dir1/dir1");
    tree.index(dir1_dir1, dir1);

    DefaultInputFile dir1_dir1_file = newDefaultInputFile(root.getBaseDir(), root, "dir1/dir1/Foo.java");
    tree.index(dir1_dir1_file, dir1_dir1);

    // module in root

    DefaultInputModule mod1 = newDefaultInputModule(root, "mod1");
    parents.put(mod1, root);
    tree.index(mod1, root);

    DefaultInputFile mod1_file = newDefaultInputFile(root.getBaseDir(), mod1, "Foo.java");
    tree.index(mod1_file, mod1);

    DefaultInputDir mod1_dir2 = newDefaultInputDir(mod1, "dir2");
    tree.index(mod1_dir2, mod1);

    DefaultInputFile mod1_dir2_file = newDefaultInputFile(root.getBaseDir(), mod1, "dir2/Foo.java");
    tree.index(mod1_dir2_file, mod1_dir2);

    // module in module

    DefaultInputModule mod1_mod2 = newDefaultInputModule(mod1, "mod2");
    parents.put(mod1_mod2, mod1);
    tree.index(mod1_mod2, mod1);

    DefaultInputFile mod1_mod2_file = newDefaultInputFile(root.getBaseDir(), mod1_mod2, "Foo.java");
    tree.index(mod1_mod2_file, mod1_mod2);

    DefaultInputDir mod1_mod2_dir = newDefaultInputDir(mod1_mod2, "dir");
    tree.index(mod1_mod2_dir, mod1_mod2);

    DefaultInputFile mod1_mod2_dir_file = newDefaultInputFile(root.getBaseDir(), mod1_mod2, "dir/Foo.java");
    tree.index(mod1_mod2_dir_file, mod1_mod2_dir);

    moduleHierarchy = new DefaultInputModuleHierarchy(parents);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree, branchConfiguration);
    publisher.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);

    // project root
    assertThat(reader.readComponent(root.batchId()).getPath()).isEmpty();
    assertThat(reader.readComponent(root.batchId()).getProjectRelativePath()).isEmpty();

    // file in root
    assertThat(reader.readComponent(file.batchId()).getPath()).isEqualTo("Foo.java");
    assertThat(reader.readComponent(file.batchId()).getProjectRelativePath()).isEqualTo("Foo.java");

    // dir in root
    assertThat(reader.readComponent(dir1.batchId()).getPath()).isEqualTo("dir1");
    assertThat(reader.readComponent(dir1.batchId()).getProjectRelativePath()).isEqualTo("dir1");

    // file in dir in root
    assertThat(reader.readComponent(dir1_file.batchId()).getPath()).isEqualTo("dir1/Foo.java");
    assertThat(reader.readComponent(dir1_file.batchId()).getProjectRelativePath()).isEqualTo("dir1/Foo.java");

    // dir in dir in root
    assertThat(reader.readComponent(dir1_dir1.batchId()).getPath()).isEqualTo("dir1/dir1");
    assertThat(reader.readComponent(dir1_dir1.batchId()).getProjectRelativePath()).isEqualTo("dir1/dir1");

    // module in root
    assertThat(reader.readComponent(mod1.batchId()).getPath()).isEqualTo("mod1");
    assertThat(reader.readComponent(mod1.batchId()).getProjectRelativePath()).isEqualTo("mod1");

    // dir in module in root
    assertThat(reader.readComponent(mod1_dir2.batchId()).getPath()).isEqualTo("dir2");
    assertThat(reader.readComponent(mod1_dir2.batchId()).getProjectRelativePath()).isEqualTo("mod1/dir2");

    // file in dir in module in root
    assertThat(reader.readComponent(mod1_dir2_file.batchId()).getPath()).isEqualTo("dir2/Foo.java");
    assertThat(reader.readComponent(mod1_dir2_file.batchId()).getProjectRelativePath()).isEqualTo("mod1/dir2/Foo.java");

    // module in module
    assertThat(reader.readComponent(mod1_mod2.batchId()).getPath()).isEqualTo("mod2");
    assertThat(reader.readComponent(mod1_mod2.batchId()).getProjectRelativePath()).isEqualTo("mod1/mod2");

    // file in module in module
    assertThat(reader.readComponent(mod1_mod2_file.batchId()).getPath()).isEqualTo("Foo.java");
    assertThat(reader.readComponent(mod1_mod2_file.batchId()).getProjectRelativePath()).isEqualTo("mod1/mod2/Foo.java");

    // dir in module in module
    assertThat(reader.readComponent(mod1_mod2_dir.batchId()).getPath()).isEqualTo("dir");
    assertThat(reader.readComponent(mod1_mod2_dir.batchId()).getProjectRelativePath()).isEqualTo("mod1/mod2/dir");

    // file in dir in module in module
    assertThat(reader.readComponent(mod1_mod2_dir_file.batchId()).getPath()).isEqualTo("dir/Foo.java");
    assertThat(reader.readComponent(mod1_mod2_dir_file.batchId()).getProjectRelativePath()).isEqualTo("mod1/mod2/dir/Foo.java");
  }
}
