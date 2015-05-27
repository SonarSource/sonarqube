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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class FeedComponentUuidsStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File reportDir;

  DbSession session;

  DbClient dbClient;

  Settings projectSettings;

  LanguageRepository languageRepository = mock(LanguageRepository.class);

  FeedComponentUuidsStep sut;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao());

    reportDir = temp.newFolder();

    projectSettings = new Settings();
    sut = new FeedComponentUuidsStep(dbClient);
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }

  @Test
  public void add_components() throws Exception {
    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    BatchReportReader batchReportReader = new BatchReportReader(reportDir);
    ComputationContext context = new ComputationContext(batchReportReader, PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(batchReportReader), languageRepository);
    sut.execute(context);

    Map<Integer, Component> componentsByRef = getComponentsByRef(context.getRoot());

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
    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .setName("Module")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.MODULE)
      .setKey("SUB_MODULE_KEY")
      .setName("Sub Module")
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(5)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    BatchReportReader batchReportReader = new BatchReportReader(reportDir);
    ComputationContext context = new ComputationContext(batchReportReader, PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(batchReportReader), languageRepository);
    sut.execute(context);

    Map<Integer, Component> componentsByRef = getComponentsByRef(context.getRoot());

    assertThat(componentsByRef.get(4).getKey()).isEqualTo("SUB_MODULE_KEY:src/main/java/dir");
    assertThat(componentsByRef.get(5).getKey()).isEqualTo("SUB_MODULE_KEY:src/main/java/dir/Foo.java");
  }

  @Test
  public void use_branch_to_generate_keys() throws Exception {
    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setBranch("origin/master")
      .setProjectKey("")
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .setName("Module")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    BatchReportReader batchReportReader = new BatchReportReader(reportDir);
    ComputationContext context = new ComputationContext(batchReportReader, PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(batchReportReader), languageRepository);
    sut.execute(context);

    Map<Integer, Component> componentsByRef = getComponentsByRef(context.getRoot());

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
