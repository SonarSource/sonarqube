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
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.test.DbTests;

@Category(DbTests.class)
public class ValidateProjectStepTest {

  private static long DEFAULT_ANALYSIS_TIME = 1433131200000L; // 2015-06-01
  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String MODULE_KEY = "MODULE_KEY";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  DbClient dbClient = dbTester.getDbClient();

  Settings settings;

  ValidateProjectStep underTest;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    settings = new Settings();

    underTest = new ValidateProjectStep(dbClient, settings, reportReader, treeRootHolder);
  }

  @Test
  public void not_fail_if_provisioning_enforced_and_project_exists() {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder().setAnalysisDate(DEFAULT_ANALYSIS_TIME).build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .build());

    settings.appendProperty(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION, "true");
    dbClient.componentDao().insert(dbTester.getSession(), ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY));
    dbTester.getSession().commit();
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    underTest.execute();
  }

  @Test
  public void fail_if_provisioning_enforced_and_project_does_not_exists() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("Unable to scan non-existing project '" + PROJECT_KEY + "'");

    reportReader.setMetadata(BatchReport.Metadata.newBuilder().setAnalysisDate(DEFAULT_ANALYSIS_TIME).build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .build());

    settings.appendProperty(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION, "true");
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    underTest.execute();
  }

  @Test
  public void fail_if_provisioning_not_enforced_and_project_does_not_exists() {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder().setAnalysisDate(DEFAULT_ANALYSIS_TIME).build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .build());

    settings.appendProperty(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION, "false");
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    underTest.execute();
  }

  @Test
  public void not_fail_on_valid_branch() {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setAnalysisDate(DEFAULT_ANALYSIS_TIME)
      .setBranch("origin/master")
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .build());
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY + ":origin/master").build());

    underTest.execute();
  }

  @Test
  public void fail_on_invalid_branch() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o \"bran#ch\" is not a valid branch name. Allowed characters are alphanumeric, '-', '_', '.' and '/'.");

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setAnalysisDate(DEFAULT_ANALYSIS_TIME)
      .setBranch("bran#ch")
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .build());
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY + ":bran#ch").build());

    underTest.execute();
  }

  @Test
  public void fail_on_invalid_key() {
    String invalidProjectKey = "Project\\Key";

    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o \"Project\\Key\" is not a valid project or module key. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.\n" +
      "  o \"Module$Key\" is not a valid project or module key. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit");

    reportReader.setMetadata(BatchReport.Metadata.newBuilder().setAnalysisDate(DEFAULT_ANALYSIS_TIME).build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(invalidProjectKey)
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("Module$Key")
      .build());
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(invalidProjectKey).addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("Module$Key").build())
      .build());

    underTest.execute();
  }

  @Test
  public void fail_if_module_key_is_already_used_as_project_key() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o The project \"" + MODULE_KEY + "\" is already defined in SonarQube but not as a module of project \"" + PROJECT_KEY + "\". " +
      "If you really want to stop directly analysing project \"" + MODULE_KEY + "\", please first delete it from SonarQube and then relaunch the analysis of project \""
      + PROJECT_KEY + "\".");

    reportReader.setMetadata(BatchReport.Metadata.newBuilder().setAnalysisDate(DEFAULT_ANALYSIS_TIME).build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey(MODULE_KEY)
      .build());

    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(MODULE_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY).build())
      .build());

    underTest.execute();
  }

  @Test
  public void fail_if_module_key_already_exists_in_another_project() {
    String anotherProjectKey = "ANOTHER_PROJECT_KEY";
    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o Module \"" + MODULE_KEY + "\" is already part of project \"" + anotherProjectKey + "\"");

    reportReader.setMetadata(BatchReport.Metadata.newBuilder().setAnalysisDate(DEFAULT_ANALYSIS_TIME).build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey(MODULE_KEY)
      .build());

    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    ComponentDto anotherProject = ComponentTesting.newProjectDto().setKey(anotherProjectKey);
    dbClient.componentDao().insert(dbTester.getSession(), project, anotherProject);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", anotherProject).setKey(MODULE_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY).build())
      .build());

    underTest.execute();
  }

  @Test
  public void fail_if_project_key_already_exists_as_module() {
    String anotherProjectKey = "ANOTHER_PROJECT_KEY";
    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o The project \"" + PROJECT_KEY + "\" is already defined in SonarQube but as a module of project \"" + anotherProjectKey + "\". " +
      "If you really want to stop directly analysing project \"" + anotherProjectKey + "\", please first delete it from SonarQube and then relaunch the analysis of project \""
      + PROJECT_KEY + "\".");

    reportReader.setMetadata(BatchReport.Metadata.newBuilder().setAnalysisDate(DEFAULT_ANALYSIS_TIME).build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey(MODULE_KEY)
      .build());

    ComponentDto anotherProject = ComponentTesting.newProjectDto().setKey(anotherProjectKey);
    dbClient.componentDao().insert(dbTester.getSession(), anotherProject);
    ComponentDto module = ComponentTesting.newModuleDto("ABCD", anotherProject).setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY).build())
      .build());

    underTest.execute();
  }

  @Test
  public void not_fail_if_analysis_date_is_after_last_analysis() {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setAnalysisDate(DEFAULT_ANALYSIS_TIME) // 2015-06-01
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());

    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbClient.snapshotDao().insert(dbTester.getSession(), SnapshotTesting.createForProject(project).setCreatedAt(1420088400000L)); // 2015-01-01
    dbTester.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    underTest.execute();
  }

  @Test
  public void fail_if_analysis_date_is_before_last_analysis() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:");
    thrown.expectMessage("Date of analysis cannot be older than the date of the last known analysis on this project. Value: ");
    thrown.expectMessage("Latest analysis: ");

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setAnalysisDate(1420088400000L) // 2015-01-01
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());

    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbClient.snapshotDao().insert(dbTester.getSession(), SnapshotTesting.createForProject(project).setCreatedAt(1433131200000L)); // 2015-06-01
    dbTester.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    underTest.execute();
  }
}
