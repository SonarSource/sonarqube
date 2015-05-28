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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class PopulateComponentsUuidAndKeyStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @ClassRule
  public static DbTester dbTester = new DbTester();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  DbSession session;

  PopulateComponentsUuidAndKeyStep sut;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao());

    sut = new PopulateComponentsUuidAndKeyStep(dbClient, reportReader, treeRootHolder);
  }

  @After
  public void tearDown() throws Exception {
    MyBatis.closeQuietly(session);
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }

  @Test
  public void add_components() throws Exception {
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

    treeRootHolder.setRoot(ComponentTreeBuilders.from(reportReader).build());
    sut.execute(mock(ComputationContext.class));

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
  public void use_latest_module_for_files_key() throws Exception {
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

    treeRootHolder.setRoot(ComponentTreeBuilders.from(reportReader).build());
    sut.execute(mock(ComputationContext.class));

    Map<Integer, Component> componentsByRef = getComponentsByRef(treeRootHolder.getRoot());

    assertThat(componentsByRef.get(4).getKey()).isEqualTo("SUB_MODULE_KEY:src/main/java/dir");
    assertThat(componentsByRef.get(5).getKey()).isEqualTo("SUB_MODULE_KEY:src/main/java/dir/Foo.java");
  }

  @Test
  public void use_branch_to_generate_keys() throws Exception {
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

    treeRootHolder.setRoot(ComponentTreeBuilders.from(reportReader).build());
    sut.execute(mock(ComputationContext.class));

    Map<Integer, Component> componentsByRef = getComponentsByRef(treeRootHolder.getRoot());

    assertThat(componentsByRef.get(1).getKey()).isEqualTo("PROJECT_KEY:origin/master");
    assertThat(componentsByRef.get(2).getKey()).isEqualTo("MODULE_KEY:origin/master");
    assertThat(componentsByRef.get(3).getKey()).isEqualTo("MODULE_KEY:origin/master:src/main/java/dir");
    assertThat(componentsByRef.get(4).getKey()).isEqualTo("MODULE_KEY:origin/master:src/main/java/dir/Foo.java");
  }

  private static Map<Integer, Component> getComponentsByRef(Component root) {
    Map<Integer, Component> componentsByRef = new HashMap<>();
    feedComponentByRef(root, componentsByRef);
    return componentsByRef;
  }

  private static void feedComponentByRef(Component component, Map<Integer, Component> map) {
    map.put(component.getRef(), component);
    for (Component child : component.getChildren()) {
      feedComponentByRef(child, map);
    }
  }

}
