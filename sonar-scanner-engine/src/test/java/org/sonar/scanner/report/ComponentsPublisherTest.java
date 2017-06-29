/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
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
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.DefaultComponentTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentsPublisherTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultComponentTree tree;
  private InputModuleHierarchy moduleHierarchy;
  private File outputDir;
  private ScannerReportWriter writer;

  @Before
  public void setUp() throws IOException {
    tree = new DefaultComponentTree();
    outputDir = temp.newFolder();
    writer = new ScannerReportWriter(outputDir);
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
      .setDescription("Root description");
    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    ProjectDefinition module1Def = ProjectDefinition.create()
      .setKey("module1")
      .setName("Module1")
      .setDescription("Module description");
    rootDef.addSubProject(module1Def);

    DefaultInputModule module1 = new DefaultInputModule(module1Def, 2);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.children(root)).thenReturn(Collections.singleton(module1));
    tree.index(module1, root);

    DefaultInputDir dir = new DefaultInputDir("module1", "src", 3);
    tree.index(dir, module1);

    DefaultInputFile file = new TestInputFileBuilder("module1", "src/Foo.java", 4).setLines(2).build();
    tree.index(file, dir);

    DefaultInputFile file2 = new TestInputFileBuilder("module1", "src/Foo2.java", 5).setPublish(false).setLines(2).build();
    tree.index(file2, dir);

    DefaultInputFile fileWithoutLang = new TestInputFileBuilder("module1", "src/make", 6).setLines(10).build();
    tree.index(fileWithoutLang, dir);

    DefaultInputFile testFile = new TestInputFileBuilder("module1", "test/FooTest.java", 7).setType(Type.TEST).setLines(4).build();
    tree.index(testFile, dir);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree);
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
  }

  @Test
  public void should_skip_dir_without_published_files() {
    ProjectAnalysisInfo projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    when(projectAnalysisInfo.analysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setName("Root project")
      .setDescription("Root description");
    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.children(root)).thenReturn(Collections.emptyList());

    // dir with files
    DefaultInputDir dir = new DefaultInputDir("module1", "src", 2);
    tree.index(dir, root);

    // dir without files and issues
    DefaultInputDir dir2 = new DefaultInputDir("module1", "src2", 3);
    tree.index(dir2, root);

    // dir without files but has issues
    DefaultInputDir dir3 = new DefaultInputDir("module1", "src3", 4);
    tree.index(dir3, root);
    writeIssue(4);

    DefaultInputFile file = new TestInputFileBuilder("module1", "src/Foo.java", 5).setLines(2).build();
    tree.index(file, dir);

    DefaultInputFile file2 = new TestInputFileBuilder("module1", "src2/Foo2.java", 6).setPublish(false).setLines(2).build();
    tree.index(file2, dir2);

    DefaultInputFile file3 = new TestInputFileBuilder("module1", "src2/Foo3.java", 7).setPublish(false).setLines(2).build();
    tree.index(file3, dir3);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree);
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
  public void add_components_without_version_and_name() throws IOException {
    ProjectAnalysisInfo projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    when(projectAnalysisInfo.analysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setDescription("Root description");
    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    ProjectDefinition module1Def = ProjectDefinition.create()
      .setKey("module1")
      .setDescription("Module description");
    rootDef.addSubProject(module1Def);
    DefaultInputModule module1 = new DefaultInputModule(module1Def, 2);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.children(root)).thenReturn(Collections.singleton(module1));
    tree.index(module1, root);

    DefaultInputDir dir = new DefaultInputDir("module1", "src", 3);
    tree.index(dir, module1);

    DefaultInputFile file = new TestInputFileBuilder("module1", "src/Foo.java", 4).setLines(2).build();
    tree.index(file, dir);

    DefaultInputFile fileWithoutLang = new TestInputFileBuilder("module1", "src/make", 5).setLines(10).build();
    tree.index(fileWithoutLang, dir);

    DefaultInputFile testFile = new TestInputFileBuilder("module1", "test/FooTest.java", 6).setType(Type.TEST).setLines(4).build();
    tree.index(testFile, dir);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree);
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
      .setDescription("Root description");
    DefaultInputModule root = new DefaultInputModule(rootDef, 1);

    ProjectDefinition module1Def = ProjectDefinition.create()
      .setKey("module1")
      .setName("Module1")
      .setProperty(CoreProperties.LINKS_CI, "http://ci")
      .setDescription("Module description");
    rootDef.addSubProject(module1Def);
    DefaultInputModule module1 = new DefaultInputModule(module1Def, 2);

    moduleHierarchy = mock(InputModuleHierarchy.class);
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.children(root)).thenReturn(Collections.singleton(module1));
    tree.index(module1, root);

    DefaultInputDir dir = new DefaultInputDir("module1", "src", 3);
    tree.index(dir, module1);

    DefaultInputFile file = new TestInputFileBuilder("module1", "src/Foo.java", 4).setLines(2).build();
    tree.index(file, dir);

    ComponentsPublisher publisher = new ComponentsPublisher(moduleHierarchy, tree);
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
}
