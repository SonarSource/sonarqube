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

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.db.DbClient;

public class ValidateProjectStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String MODULE_KEY = "MODULE_KEY";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  DbClient dbClient;

  DbSession dbSession;

  Settings settings;

  ValidateProjectStep sut;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao());
    dbSession = dbClient.openSession(false);
    settings = new Settings();

    sut = new ValidateProjectStep(dbClient, settings);
  }

  @After
  public void tearDown() throws Exception {
    dbSession.close();
  }

  @Test
  public void not_fail_if_provisioning_enforced_and_project_exists() throws Exception {
    settings.appendProperty(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION, "true");
    dbClient.componentDao().insert(dbSession, ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY));
    dbSession.commit();

    sut.execute(new ComputationContext(createBasicBatchReportReader(), null, null, null,
      ComponentTreeBuilders.from(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY)), null));
  }

  @Test
  public void fail_if_provisioning_enforced_and_project_does_not_exists() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Unable to scan non-existing project '" + PROJECT_KEY + "'");

    settings.appendProperty(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION, "true");

    sut.execute(new ComputationContext(createBasicBatchReportReader(), null, null, null,
      ComponentTreeBuilders.from(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY)), null));
  }

  @Test
  public void fail_if_provisioning_not_enforced_and_project_does_not_exists() throws Exception {
    settings.appendProperty(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION, "false");

    sut.execute(new ComputationContext(createBasicBatchReportReader(), null, null, null,
      ComponentTreeBuilders.from(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY)), null));
  }

  @Test
  public void not_fail_on_valid_branch() throws Exception {
    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setBranch("origin/master")
      .build());

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), null, null, null,
      ComponentTreeBuilders.from(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY)), null));
  }

  @Test
  public void fail_on_invalid_branch() throws Exception {
    File reportDir = temp.newFolder();
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o \"bran#ch\" is not a valid branch name. Allowed characters are alphanumeric, '-', '_', '.' and '/'.");

    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setBranch("bran#ch")
      .build());

    sut.execute(new ComputationContext(new BatchReportReader(reportDir), null, null, null,
      ComponentTreeBuilders.from(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY)), null));
  }

  @Test
  public void fail_on_invalid_key() throws Exception {
    String invalidProjectKey = "Project\\Key";

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o \"Project\\Key\" is not a valid project or module key. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.\n" +
      "  o \"Module$Key\" is not a valid project or module key. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit");

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", invalidProjectKey,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", "Module$Key"));
    sut.execute(new ComputationContext(createBasicBatchReportReader(), null, null, null, ComponentTreeBuilders.from(root), null));
  }

  @Test
  public void fail_if_module_key_is_already_used_as_project_key() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o The project \"" + MODULE_KEY + "\" is already defined in SonarQube but not as a module of project \"" + PROJECT_KEY + "\". " +
      "If you really want to stop directly analysing project \"" + MODULE_KEY + "\", please first delete it from SonarQube and then relaunch the analysis of project \""
      + PROJECT_KEY + "\".");

    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(MODULE_KEY);
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", MODULE_KEY));
    sut.execute(new ComputationContext(createBasicBatchReportReader(), null, null, null,
      ComponentTreeBuilders.from(root), null));
  }

  @Test
  public void fail_if_module_key_already_exists_in_another_project() throws Exception {
    String anotherProjectKey = "ANOTHER_PROJECT_KEY";
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o Module \"" + MODULE_KEY + "\" is already part of project \"" + anotherProjectKey + "\"");

    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    ComponentDto anotherProject = ComponentTesting.newProjectDto().setKey(anotherProjectKey);
    dbClient.componentDao().insert(dbSession, project, anotherProject);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", anotherProject).setKey(MODULE_KEY);
    dbClient.componentDao().insert(dbSession, module);
    dbSession.commit();

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", MODULE_KEY));
    sut.execute(new ComputationContext(createBasicBatchReportReader(), null, null, null,
      ComponentTreeBuilders.from(root), null));
  }

  @Test
  public void fail_if_project_key_already_exists_as_module() throws Exception {
    String anotherProjectKey = "ANOTHER_PROJECT_KEY";
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Validation of project failed:\n" +
      "  o The project \"" + PROJECT_KEY + "\" is already defined in SonarQube but as a module of project \"" + anotherProjectKey + "\". " +
      "If you really want to stop directly analysing project \"" + anotherProjectKey + "\", please first delete it from SonarQube and then relaunch the analysis of project \""
      + PROJECT_KEY + "\".");

    ComponentDto anotherProject = ComponentTesting.newProjectDto().setKey(anotherProjectKey);
    dbClient.componentDao().insert(dbSession, anotherProject);
    ComponentDto module = ComponentTesting.newModuleDto("ABCD", anotherProject).setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbSession, module);
    dbSession.commit();

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", MODULE_KEY));
    sut.execute(new ComputationContext(createBasicBatchReportReader(), null, null, null,
      ComponentTreeBuilders.from(root), null));
  }

  private BatchReportReader createBasicBatchReportReader() throws IOException {
    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .build());
    return new BatchReportReader(reportDir);
  }
}
