/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentTreeBuilder;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class FillComponentsStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  DbClient dbClient = dbTester.getDbClient();

  FillComponentsStep underTest;

  @Before
  public void setup() {
    dbTester.truncateTables();
    underTest = new FillComponentsStep(dbClient, reportReader, treeRootHolder);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void compute_keys_and_uuids() {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .addChildRef(3)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(4)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    treeRootHolder.setRoot(ComponentTreeBuilder.from(reportReader));
    underTest.execute();

    Map<Integer, Component> componentsByRef = getComponentsByRef(treeRootHolder.getRoot());

    assertThat(componentsByRef.get(1).getKey()).isEqualTo(PROJECT_KEY);
    assertThat(componentsByRef.get(1).getUuid()).isNotNull();

    assertThat(componentsByRef.get(2).getKey()).isEqualTo("MODULE_KEY");
    assertThat(componentsByRef.get(2).getUuid()).isNotNull();

    assertThat(componentsByRef.get(3).getKey()).isEqualTo("MODULE_KEY:src/main/java/dir");
    assertThat(componentsByRef.get(3).getUuid()).isNotNull();

    assertThat(componentsByRef.get(4).getKey()).isEqualTo("MODULE_KEY:src/main/java/dir/Foo.java");
    assertThat(componentsByRef.get(4).getUuid()).isNotNull();
  }

  @Test
  public void return_existing_uuids() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY");
    dbClient.componentDao().insert(dbTester.getSession(), module);
    ComponentDto directory = ComponentTesting.newDirectory(module, "CDEF", "src/main/java/dir").setKey("MODULE_KEY:src/main/java/dir");
    ComponentDto file = ComponentTesting.newFileDto(module, "DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(dbTester.getSession(), directory, file);
    dbTester.getSession().commit();

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .addChildRef(3)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(4)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    treeRootHolder.setRoot(ComponentTreeBuilder.from(reportReader));
    underTest.execute();

    Map<Integer, Component> componentsByRef = getComponentsByRef(treeRootHolder.getRoot());

    assertThat(componentsByRef.get(1).getKey()).isEqualTo(PROJECT_KEY);
    assertThat(componentsByRef.get(1).getUuid()).isEqualTo("ABCD");

    assertThat(componentsByRef.get(2).getKey()).isEqualTo("MODULE_KEY");
    assertThat(componentsByRef.get(2).getUuid()).isEqualTo("BCDE");

    assertThat(componentsByRef.get(3).getKey()).isEqualTo("MODULE_KEY:src/main/java/dir");
    assertThat(componentsByRef.get(3).getUuid()).isEqualTo("CDEF");

    assertThat(componentsByRef.get(4).getKey()).isEqualTo("MODULE_KEY:src/main/java/dir/Foo.java");
    assertThat(componentsByRef.get(4).getUuid()).isEqualTo("DEFG");
  }

  @Test
  public void use_latest_module_for_files_key() {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .setName("Module")
      .addChildRef(3)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.MODULE)
      .setKey("SUB_MODULE_KEY")
      .setName("Sub Module")
      .addChildRef(4)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(5)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    treeRootHolder.setRoot(ComponentTreeBuilder.from(reportReader));
    underTest.execute();

    Map<Integer, Component> componentsByRef = getComponentsByRef(treeRootHolder.getRoot());

    assertThat(componentsByRef.get(4).getKey()).isEqualTo("SUB_MODULE_KEY:src/main/java/dir");
    assertThat(componentsByRef.get(5).getKey()).isEqualTo("SUB_MODULE_KEY:src/main/java/dir/Foo.java");
  }

  @Test
  public void use_branch_to_generate_keys() {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setBranch("origin/master")
      .setProjectKey("")
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .setName("Module")
      .addChildRef(3)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(4)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    treeRootHolder.setRoot(ComponentTreeBuilder.from(reportReader));
    underTest.execute();

    Map<Integer, Component> componentsByRef = getComponentsByRef(treeRootHolder.getRoot());

    assertThat(componentsByRef.get(1).getKey()).isEqualTo("PROJECT_KEY:origin/master");
    assertThat(componentsByRef.get(2).getKey()).isEqualTo("MODULE_KEY:origin/master");
    assertThat(componentsByRef.get(3).getKey()).isEqualTo("MODULE_KEY:origin/master:src/main/java/dir");
    assertThat(componentsByRef.get(4).getKey()).isEqualTo("MODULE_KEY:origin/master:src/main/java/dir/Foo.java");
  }

  @Test
  public void compute_keys_and_uuids_on_project_having_module_and_directory() {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .addChildRef(5)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .addChildRef(3)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(4)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("/")
      .addChildRef(6)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(6)
      .setType(Constants.ComponentType.FILE)
      .setPath("pom.xml")
      .build());

    treeRootHolder.setRoot(ComponentTreeBuilder.from(reportReader));
    underTest.execute();

    Map<Integer, Component> componentsByRef = getComponentsByRef(treeRootHolder.getRoot());

    assertThat(componentsByRef.get(1).getKey()).isEqualTo(PROJECT_KEY);
    assertThat(componentsByRef.get(1).getUuid()).isNotNull();

    assertThat(componentsByRef.get(2).getKey()).isEqualTo("MODULE_KEY");
    assertThat(componentsByRef.get(2).getUuid()).isNotNull();

    assertThat(componentsByRef.get(3).getKey()).isEqualTo("MODULE_KEY:src/main/java/dir");
    assertThat(componentsByRef.get(3).getUuid()).isNotNull();

    assertThat(componentsByRef.get(4).getKey()).isEqualTo("MODULE_KEY:src/main/java/dir/Foo.java");
    assertThat(componentsByRef.get(4).getUuid()).isNotNull();

    assertThat(componentsByRef.get(5).getKey()).isEqualTo(PROJECT_KEY + ":/");
    assertThat(componentsByRef.get(5).getUuid()).isNotNull();

    assertThat(componentsByRef.get(6).getKey()).isEqualTo(PROJECT_KEY + ":pom.xml");
    assertThat(componentsByRef.get(6).getUuid()).isNotNull();
  }

  @Test
  public void compute_keys_and_uuids_on_multi_modules() {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .addChildRef(3)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.MODULE)
      .setKey("SUB_MODULE_KEY")
      .addChildRef(4)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(5)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    treeRootHolder.setRoot(ComponentTreeBuilder.from(reportReader));
    underTest.execute();

    Map<Integer, Component> componentsByRef = getComponentsByRef(treeRootHolder.getRoot());

    assertThat(componentsByRef.get(1).getKey()).isEqualTo(PROJECT_KEY);
    assertThat(componentsByRef.get(1).getUuid()).isNotNull();

    assertThat(componentsByRef.get(2).getKey()).isEqualTo("MODULE_KEY");
    assertThat(componentsByRef.get(2).getUuid()).isNotNull();

    assertThat(componentsByRef.get(3).getKey()).isEqualTo("SUB_MODULE_KEY");
    assertThat(componentsByRef.get(3).getUuid()).isNotNull();

    assertThat(componentsByRef.get(4).getKey()).isEqualTo("SUB_MODULE_KEY:src/main/java/dir");
    assertThat(componentsByRef.get(4).getUuid()).isNotNull();

    assertThat(componentsByRef.get(5).getKey()).isEqualTo("SUB_MODULE_KEY:src/main/java/dir/Foo.java");
    assertThat(componentsByRef.get(5).getUuid()).isNotNull();
  }

  @Test
  public void return_existing_uuids_when_components_were_removed() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto removedModule = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setEnabled(false);
    dbClient.componentDao().insert(dbTester.getSession(), removedModule);
    ComponentDto removedDirectory = ComponentTesting.newDirectory(removedModule, "CDEF", "src/main/java/dir").setKey("MODULE_KEY:src/main/java/dir").setEnabled(false);
    ComponentDto removedFile = ComponentTesting.newFileDto(removedModule, "DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java").setEnabled(false);
    dbClient.componentDao().insert(dbTester.getSession(), removedDirectory, removedFile);
    dbTester.getSession().commit();

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .addChildRef(3)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(4)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    treeRootHolder.setRoot(ComponentTreeBuilder.from(reportReader));
    underTest.execute();

    Map<Integer, Component> componentsByRef = getComponentsByRef(treeRootHolder.getRoot());

    assertThat(componentsByRef.get(1).getKey()).isEqualTo(PROJECT_KEY);
    assertThat(componentsByRef.get(1).getUuid()).isEqualTo("ABCD");

    // No new UUID is generated on removed components

    assertThat(componentsByRef.get(2).getKey()).isEqualTo("MODULE_KEY");
    assertThat(componentsByRef.get(2).getUuid()).isEqualTo("BCDE");

    assertThat(componentsByRef.get(3).getKey()).isEqualTo("MODULE_KEY:src/main/java/dir");
    assertThat(componentsByRef.get(3).getUuid()).isEqualTo("CDEF");

    assertThat(componentsByRef.get(4).getKey()).isEqualTo("MODULE_KEY:src/main/java/dir/Foo.java");
    assertThat(componentsByRef.get(4).getUuid()).isEqualTo("DEFG");
  }

  private static Map<Integer, Component> getComponentsByRef(Component root) {
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

}
