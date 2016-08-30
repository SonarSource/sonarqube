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
package org.sonar.scanner.report;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.DateUtils;
import org.sonar.scanner.FakeJava;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport.Component;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;
import org.sonar.scanner.report.ComponentsPublisher;
import org.sonar.scanner.scan.ImmutableProjectReactor;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentsPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  BatchComponentCache resourceCache = new BatchComponentCache();

  @Test
  public void add_components_to_report() throws Exception {

    ProjectDefinition rootDef = ProjectDefinition.create().setKey("foo");
    rootDef.properties().put(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0");
    Project root = new Project("foo").setName("Root project").setDescription("Root description")
      .setAnalysisDate(DateUtils.parseDate(("2012-12-12")));
    root.setId(1).setUuid("PROJECT_UUID");
    resourceCache.add(root, null).setInputComponent(new DefaultInputModule("foo"));

    Project module1 = new Project("module1").setName("Module1").setDescription("Module description");
    module1.setParent(root);
    module1.setId(2).setUuid("MODULE_UUID");
    resourceCache.add(module1, root).setInputComponent(new DefaultInputModule("module1"));
    rootDef.addSubProject(ProjectDefinition.create().setKey("module1"));

    Directory dir = Directory.create("src");
    dir.setEffectiveKey("module1:src");
    dir.setId(3).setUuid("DIR_UUID");
    resourceCache.add(dir, module1).setInputComponent(new DefaultInputDir("foo", "src"));

    org.sonar.api.resources.File file = org.sonar.api.resources.File.create("src/Foo.java", FakeJava.INSTANCE, false);
    file.setEffectiveKey("module1:src/Foo.java");
    file.setId(4).setUuid("FILE_UUID");
    resourceCache.add(file, dir).setInputComponent(new DefaultInputFile("module1", "src/Foo.java").setLines(2));

    org.sonar.api.resources.File fileWithoutLang = org.sonar.api.resources.File.create("src/make", null, false);
    fileWithoutLang.setEffectiveKey("module1:src/make");
    fileWithoutLang.setId(5).setUuid("FILE_WITHOUT_LANG_UUID");
    resourceCache.add(fileWithoutLang, dir).setInputComponent(new DefaultInputFile("module1", "src/make").setLines(10));

    org.sonar.api.resources.File testFile = org.sonar.api.resources.File.create("test/FooTest.java", FakeJava.INSTANCE, true);
    testFile.setEffectiveKey("module1:test/FooTest.java");
    testFile.setId(6).setUuid("TEST_FILE_UUID");
    resourceCache.add(testFile, dir).setInputComponent(new DefaultInputFile("module1", "test/FooTest.java").setLines(4));

    ImmutableProjectReactor reactor = new ImmutableProjectReactor(rootDef);

    ComponentsPublisher publisher = new ComponentsPublisher(reactor, resourceCache);

    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);
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
    assertThat(rootProtobuf.getDescription()).isEqualTo("Root description");
    assertThat(rootProtobuf.getVersion()).isEqualTo("1.0");
    assertThat(rootProtobuf.getLinkCount()).isEqualTo(0);

    Component module1Protobuf = reader.readComponent(2);
    assertThat(module1Protobuf.getKey()).isEqualTo("module1");
    assertThat(module1Protobuf.getDescription()).isEqualTo("Module description");
    assertThat(module1Protobuf.getVersion()).isEqualTo("1.0");
  }
  
  @Test
  public void add_components_without_version_and_name() throws IOException {
    ProjectDefinition rootDef = ProjectDefinition.create().setKey("foo");
    Project root = new Project("foo").setDescription("Root description")
      .setAnalysisDate(DateUtils.parseDate(("2012-12-12")));
    root.setId(1).setUuid("PROJECT_UUID");
    resourceCache.add(root, null).setInputComponent(new DefaultInputModule("foo"));

    Project module1 = new Project("module1").setDescription("Module description");
    module1.setParent(root);
    module1.setId(2).setUuid("MODULE_UUID");
    resourceCache.add(module1, root).setInputComponent(new DefaultInputModule("module1"));
    rootDef.addSubProject(ProjectDefinition.create().setKey("module1"));

    Directory dir = Directory.create("src");
    dir.setEffectiveKey("module1:src");
    dir.setId(3).setUuid("DIR_UUID");
    resourceCache.add(dir, module1).setInputComponent(new DefaultInputDir("foo", "src"));

    org.sonar.api.resources.File file = org.sonar.api.resources.File.create("src/Foo.java", FakeJava.INSTANCE, false);
    file.setEffectiveKey("module1:src/Foo.java");
    file.setId(4).setUuid("FILE_UUID");
    resourceCache.add(file, dir).setInputComponent(new DefaultInputFile("module1", "src/Foo.java").setLines(2));

    org.sonar.api.resources.File fileWithoutLang = org.sonar.api.resources.File.create("src/make", null, false);
    fileWithoutLang.setEffectiveKey("module1:src/make");
    fileWithoutLang.setId(5).setUuid("FILE_WITHOUT_LANG_UUID");
    resourceCache.add(fileWithoutLang, dir).setInputComponent(new DefaultInputFile("module1", "src/make").setLines(10));

    org.sonar.api.resources.File testFile = org.sonar.api.resources.File.create("test/FooTest.java", FakeJava.INSTANCE, true);
    testFile.setEffectiveKey("module1:test/FooTest.java");
    testFile.setId(6).setUuid("TEST_FILE_UUID");
    resourceCache.add(testFile, dir).setInputComponent(new DefaultInputFile("module1", "test/FooTest.java").setLines(4));

    ImmutableProjectReactor reactor = new ImmutableProjectReactor(rootDef);

    ComponentsPublisher publisher = new ComponentsPublisher(reactor, resourceCache);

    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);
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
    // inputs
    ProjectDefinition rootDef = ProjectDefinition.create().setKey("foo");
    rootDef.properties().put(CoreProperties.PROJECT_VERSION_PROPERTY, "1.0");
    Project root = new Project("foo:my_branch").setName("Root project")
      .setAnalysisDate(DateUtils.parseDate(("2012-12-12")));
    root.setId(1).setUuid("PROJECT_UUID");
    resourceCache.add(root, null).setInputComponent(new DefaultInputModule("foo"));
    rootDef.properties().put(CoreProperties.LINKS_HOME_PAGE, "http://home");
    rootDef.properties().put(CoreProperties.PROJECT_BRANCH_PROPERTY, "my_branch");

    Project module1 = new Project("module1:my_branch").setName("Module1");
    module1.setParent(root);
    module1.setId(2).setUuid("MODULE_UUID");
    resourceCache.add(module1, root).setInputComponent(new DefaultInputModule("module1"));
    ProjectDefinition moduleDef = ProjectDefinition.create().setKey("module1");
    moduleDef.properties().put(CoreProperties.LINKS_CI, "http://ci");
    rootDef.addSubProject(moduleDef);

    Directory dir = Directory.create("src");
    dir.setEffectiveKey("module1:my_branch:my_branch:src");
    dir.setId(3).setUuid("DIR_UUID");
    resourceCache.add(dir, module1).setInputComponent(new DefaultInputDir("foo", "src"));

    org.sonar.api.resources.File file = org.sonar.api.resources.File.create("src/Foo.java", FakeJava.INSTANCE, false);
    file.setEffectiveKey("module1:my_branch:my_branch:src/Foo.java");
    file.setId(4).setUuid("FILE_UUID");
    resourceCache.add(file, dir).setInputComponent(new DefaultInputFile("module1", "src/Foo.java").setLines(2));

    ImmutableProjectReactor reactor = new ImmutableProjectReactor(rootDef);

    ComponentsPublisher publisher = new ComponentsPublisher(reactor, resourceCache);

    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);
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
