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
package org.sonar.server.computation.task.projectanalysis.step;

import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;

public class ValidateProjectStepTest {

  static long DEFAULT_ANALYSIS_TIME = 1433131200000L; // 2015-06-01
  static final String PROJECT_KEY = "PROJECT_KEY";
  static final String MODULE_KEY = "MODULE_KEY";
  static final String DEFAULT_BRANCH = "origin/master";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setAnalysisDate(new Date(DEFAULT_ANALYSIS_TIME))
    .setBranch(DEFAULT_BRANCH);

  DbClient dbClient = dbTester.getDbClient();

  ValidateProjectStep underTest = new ValidateProjectStep(dbClient, reportReader, treeRootHolder, analysisMetadataHolder);

  @Test
  public void fail_if_root_component_is_not_a_project_in_db() {
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .build());
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    ComponentDto project = ComponentTesting.newView(dbTester.organizations().insert(), "ABCD").setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbTester.getSession().commit();

    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o Component (uuid=ABCD, key=PROJECT_KEY) is not a project");

    underTest.execute();
  }

  @Test
  public void not_fail_on_valid_branch() {
    analysisMetadataHolder.setBranch(DEFAULT_BRANCH);
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .build());
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY + ":origin/master").build());

    underTest.execute();
  }

  @Test
  public void fail_on_invalid_branch() {
    analysisMetadataHolder.setBranch("bran#ch");
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .build());
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY + ":bran#ch").build());

    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o \"bran#ch\" is not a valid branch name. Allowed characters are alphanumeric, '-', '_', '.' and '/'.");

    underTest.execute();
  }

  @Test
  public void fail_on_invalid_key() {
    String invalidProjectKey = "Project\\Key";

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .setKey(invalidProjectKey)
      .addChildRef(2)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(2)
      .setType(ComponentType.MODULE)
      .setKey("Module$Key")
      .build());
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(invalidProjectKey).addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("Module$Key").build())
      .build());

    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o \"Project\\Key\" is not a valid project or module key. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.\n" +
      "  o \"Module$Key\" is not a valid project or module key. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit");

    underTest.execute();
  }

  @Test
  public void fail_if_module_key_is_already_used_as_project_key() {
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(2)
      .setType(ComponentType.MODULE)
      .setKey(MODULE_KEY)
      .build());

    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), "ABCD").setKey(MODULE_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY).build())
      .build());

    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o The project \"" + MODULE_KEY + "\" is already defined in SonarQube but not as a module of project \"" + PROJECT_KEY + "\". " +
      "If you really want to stop directly analysing project \"" + MODULE_KEY + "\", please first delete it from SonarQube and then relaunch the analysis of project \""
      + PROJECT_KEY + "\".");

    underTest.execute();
  }

  @Test
  public void fail_if_module_key_already_exists_in_another_project() {
    String anotherProjectKey = "ANOTHER_PROJECT_KEY";
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(2)
      .setType(ComponentType.MODULE)
      .setKey(MODULE_KEY)
      .build());

    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto, "ABCD").setKey(PROJECT_KEY);
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(organizationDto).setKey(anotherProjectKey);
    dbClient.componentDao().insert(dbTester.getSession(), project, anotherProject);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", anotherProject).setKey(MODULE_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY).build())
      .build());

    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o Module \"" + MODULE_KEY + "\" is already part of project \"" + anotherProjectKey + "\"");

    underTest.execute();
  }

  @Test
  public void fail_if_project_key_already_exists_as_module() {
    String anotherProjectKey = "ANOTHER_PROJECT_KEY";

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(2)
      .setType(ComponentType.MODULE)
      .setKey(MODULE_KEY)
      .build());

    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert()).setKey(anotherProjectKey);
    dbClient.componentDao().insert(dbTester.getSession(), anotherProject);
    ComponentDto module = ComponentTesting.newModuleDto("ABCD", anotherProject).setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY).build())
      .build());

    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o Component (uuid=ABCD, key=PROJECT_KEY) is not a project\n" +
      "  o The project \"" + PROJECT_KEY + "\" is already defined in SonarQube but as a module of project \"" + anotherProjectKey + "\". " +
      "If you really want to stop directly analysing project \"" + anotherProjectKey + "\", please first delete it from SonarQube and then relaunch the analysis of project \""
      + PROJECT_KEY + "\".");

    underTest.execute();
  }

  @Test
  public void not_fail_if_analysis_date_is_after_last_analysis() {
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());

    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), "ABCD").setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbClient.snapshotDao().insert(dbTester.getSession(), SnapshotTesting.newAnalysis(project).setCreatedAt(1420088400000L)); // 2015-01-01
    dbTester.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    underTest.execute();
  }

  @Test
  public void fail_if_analysis_date_is_before_last_analysis() {
    analysisMetadataHolder.setAnalysisDate(DateUtils.parseDate("2015-01-01"));

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .addChildRef(2)
      .build());

    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), "ABCD").setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbClient.snapshotDao().insert(dbTester.getSession(), SnapshotTesting.newAnalysis(project).setCreatedAt(1433131200000L)); // 2015-06-01
    dbTester.getSession().commit();

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build());

    thrown.expect(MessageException.class);
    thrown.expectMessage("Validation of project failed:");
    thrown.expectMessage("Date of analysis cannot be older than the date of the last known analysis on this project. Value: ");
    thrown.expectMessage("Latest analysis: ");

    underTest.execute();
  }
}
