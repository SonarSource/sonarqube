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
package org.sonar.scanner.report;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.utils.DateUtils;
import org.sonar.scanner.ProjectInfo;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport.Component;
import org.sonar.scanner.protocol.output.ScannerReport.Component.FileStatus;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentsPublisherTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File outputDir;
  private ScannerReportWriter writer;
  private BranchConfiguration branchConfiguration;

  @Before
  public void setUp() throws IOException {
    branchConfiguration = mock(BranchConfiguration.class);
    outputDir = temp.newFolder();
    writer = new ScannerReportWriter(outputDir);
  }

  @Test
  public void add_components_to_report() throws Exception {
    ProjectInfo projectInfo = mock(ProjectInfo.class);
    when(projectInfo.getAnalysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setName("Root project")
      .setDescription("Root description")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder());
    DefaultInputProject project = new DefaultInputProject(rootDef, 1);

    InputComponentStore store = new InputComponentStore(branchConfiguration);

    Path moduleBaseDir = temp.newFolder().toPath();
    ProjectDefinition module1Def = ProjectDefinition.create()
      .setKey("module1")
      .setName("Module1")
      .setDescription("Module description")
      .setBaseDir(moduleBaseDir.toFile())
      .setWorkDir(temp.newFolder());
    rootDef.addSubProject(module1Def);

    DefaultInputFile file = new TestInputFileBuilder("foo", "module1/src/Foo.java", 4).setLines(2).setStatus(InputFile.Status.SAME).build();
    store.put("module1", file);

    DefaultInputFile file18 = new TestInputFileBuilder("foo", "module1/src2/Foo.java", 18).setLines(2).setStatus(InputFile.Status.SAME).build();
    store.put("module1", file18);

    DefaultInputFile file2 = new TestInputFileBuilder("foo", "module1/src/Foo2.java", 5).setPublish(false).setLines(2).build();
    store.put("module1", file2);

    DefaultInputFile fileWithoutLang = new TestInputFileBuilder("foo", "module1/src/make", 6).setLines(10).setStatus(InputFile.Status.CHANGED).build();
    store.put("module1", fileWithoutLang);

    DefaultInputFile testFile = new TestInputFileBuilder("foo", "module1/test/FooTest.java", 7).setType(Type.TEST).setStatus(InputFile.Status.ADDED).setLines(4).build();
    store.put("module1", testFile);

    ComponentsPublisher publisher = new ComponentsPublisher(project, store);
    publisher.publish(writer);

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
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
    assertThat(rootProtobuf.getLinkCount()).isEqualTo(0);

    assertThat(reader.readComponent(4).getStatus()).isEqualTo(FileStatus.SAME);
    assertThat(reader.readComponent(6).getStatus()).isEqualTo(FileStatus.CHANGED);
    assertThat(reader.readComponent(7).getStatus()).isEqualTo(FileStatus.ADDED);
  }

  @Test
  public void publish_unchanged_components_even_in_short_branches() throws IOException {
    when(branchConfiguration.isShortOrPullRequest()).thenReturn(true);
    ProjectInfo projectInfo = mock(ProjectInfo.class);
    when(projectInfo.getAnalysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    Path baseDir = temp.newFolder().toPath();
    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setName("Root project")
      .setDescription("Root description")
      .setBaseDir(baseDir.toFile())
      .setWorkDir(temp.newFolder());
    DefaultInputProject project = new DefaultInputProject(rootDef, 1);

    InputComponentStore store = new InputComponentStore(branchConfiguration);

    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.java", 5)
      .setLines(2)
      .setPublish(true)
      .setStatus(InputFile.Status.ADDED)
      .build();
    store.put("foo", file);

    DefaultInputFile file2 = new TestInputFileBuilder("foo", "src2/Foo2.java", 6)
      .setPublish(true)
      .setStatus(InputFile.Status.SAME)
      .setLines(2)
      .build();
    store.put("foo", file2);

    ComponentsPublisher publisher = new ComponentsPublisher(project, store);
    publisher.publish(writer);

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 5)).isTrue();

    // do not skip, needed for computing overall coverage
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 6)).isTrue();
  }

  @Test
  public void publish_project_without_version_and_name() throws IOException {
    ProjectInfo projectInfo = mock(ProjectInfo.class);
    when(projectInfo.getAnalysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setDescription("Root description")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder());
    DefaultInputProject project = new DefaultInputProject(rootDef, 1);

    InputComponentStore store = new InputComponentStore(branchConfiguration);
    ComponentsPublisher publisher = new ComponentsPublisher(project, store);
    publisher.publish(writer);

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    Component rootProtobuf = reader.readComponent(1);
    assertThat(rootProtobuf.getKey()).isEqualTo("foo");
    assertThat(rootProtobuf.getName()).isEqualTo("");
    assertThat(rootProtobuf.getDescription()).isEqualTo("Root description");
    assertThat(rootProtobuf.getLinkCount()).isEqualTo(0);
  }

  @Test
  public void publish_project_with_links() throws Exception {
    ProjectInfo projectInfo = mock(ProjectInfo.class);
    when(projectInfo.getAnalysisDate()).thenReturn(DateUtils.parseDate("2012-12-12"));

    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey("foo")
      .setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0")
      .setName("Root project")
      .setProperty(CoreProperties.LINKS_HOME_PAGE, "http://home")
      .setProperty(CoreProperties.LINKS_CI, "http://ci")
      .setDescription("Root description")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder());
    DefaultInputProject project = new DefaultInputProject(rootDef, 1);

    InputComponentStore store = new InputComponentStore(branchConfiguration);
    ComponentsPublisher publisher = new ComponentsPublisher(project, store);
    publisher.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    Component rootProtobuf = reader.readComponent(1);
    assertThat(rootProtobuf.getLinkCount()).isEqualTo(2);
    assertThat(rootProtobuf.getLink(0).getType()).isEqualTo(ComponentLinkType.HOME);
    assertThat(rootProtobuf.getLink(0).getHref()).isEqualTo("http://home");
    assertThat(rootProtobuf.getLink(1).getType()).isEqualTo(ComponentLinkType.CI);
    assertThat(rootProtobuf.getLink(1).getHref()).isEqualTo("http://ci");
  }
}
