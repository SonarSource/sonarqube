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
package org.sonar.server.setting.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SetNewCodePeriodActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentFinder componentFinder = TestComponentFinder.from(db);
  private NewCodePeriodDao dao = mock(NewCodePeriodDao.class);

  private SetNewCodePeriodAction underTest = new SetNewCodePeriodAction(dbClient, userSession, componentFinder, dao);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("set_new_code_period");
    assertThat(definition.isInternal()).isFalse();
    //assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.since()).isEqualTo("8.0");
    assertThat(definition.isPost()).isFalse();

    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("value", "type", "projectKey", "branchKey");
    assertThat(definition.param("value").isRequired()).isFalse();
    assertThat(definition.param("type").isRequired()).isTrue();
    assertThat(definition.param("projectKey").isRequired()).isFalse();
    assertThat(definition.param("branchKey").isRequired()).isFalse();

  }

  // validation of type
  @Test
  public void throw_IAE_if_no_type_specified() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'type' parameter is missing");

    ws.newRequest()
      .execute();
  }

  @Test
  public void throw_IAE_if_type_is_invalid() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid type: unknown");

    ws.newRequest()
      .setParam("type", "unknown")
      .execute();
  }

  @Test
  public void throw_IAE_if_type_is_invalid_for_global() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid type 'DATE'. Overall setting can only be set with types: [PREVIOUS_VERSION, DAYS]");

    ws.newRequest()
      .setParam("type", "date")
      .execute();
  }

  @Test
  public void throw_IAE_if_type_is_invalid_for_project() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid type 'ANALYSIS'. Projects can only be set with types: [DATE, PREVIOUS_VERSION, DAYS]");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "analysis")
      .execute();
  }

  // validation of value
  @Test
  public void throw_IAE_if_no_value_for_date() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("New Code Period type 'DATE' requires a value");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "date")
      .execute();
  }

  @Test
  public void throw_IAE_if_no_value_for_days() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("New Code Period type 'DAYS' requires a value");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("branchKey", "master")
      .setParam("type", "days")
      .execute();
  }

  @Test
  public void throw_IAE_if_no_value_for_analysis() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("New Code Period type 'ANALYSIS' requires a value");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "analysis")
      .setParam("branchKey", "master")
      .execute();
  }

  @Test
  public void throw_IAE_if_date_is_invalid() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Failed to parse date: unknown");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "date")
      .setParam("branchKey", "master")
      .setParam("value", "unknown")
      .execute();
  }

  @Test
  public void throw_IAE_if_days_is_invalid() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Failed to parse number of days: unknown");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "days")
      .setParam("branchKey", "master")
      .setParam("value", "unknown")
      .execute();
  }

  @Test
  public void throw_IAE_if_analysis_is_not_found() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Analysis 'unknown' is not found");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "analysis")
      .setParam("branchKey", "master")
      .setParam("value", "unknown")
      .execute();
  }

  @Test
  public void throw_IAE_if_analysis_doesnt_belong_to_branch() {
    ComponentDto project = componentDb.insertMainBranch();
    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));

    SnapshotDto analysisMaster = db.components().insertSnapshot(project);
    SnapshotDto analysisBranch = db.components().insertSnapshot(branch);

    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Analysis '" + analysisBranch.getUuid() + "' does not belong to branch 'master' of project '" + project.getKey() + "'");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "analysis")
      .setParam("branchKey", "master")
      .setParam("value", analysisBranch.getUuid())
      .execute();
  }

  // validation of project/branch
  @Test
  public void throw_IAE_if_branch_is_specified_without_project() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If branch key is specified, project key needs to be specified too");

    ws.newRequest()
      .setParam("branchKey", "branch")
      .execute();
  }

  @Test
  public void throw_NFE_if_project_not_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'unknown' not found");

    ws.newRequest()
      .setParam("type", "previous_version")
      .setParam("projectKey", "unknown")
      .execute();
  }

  @Test
  public void throw_NFE_if_branch_not_found() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component '" + project.getKey() + "' on branch 'unknown' not found");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "previous_version")
      .setParam("branchKey", "unknown")
      .execute();
  }

  @Test
  public void throw_IAE_if_branch_is_a_SLB() {
    ComponentDto project = componentDb.insertMainBranch();
    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch").setBranchType(BranchType.SHORT));
    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Not a long-living branch: 'branch'");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "previous_version")
      .setParam("branchKey", "branch")
      .execute();
  }

  // permission
  @Test
  public void throw_NFE_if_no_project_permission() {
    ComponentDto project = componentDb.insertMainBranch();
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "previous_version")
      .execute();
  }

  @Test
  public void throw_NFE_if_no_system_permission() {
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setParam("type", "previous_version")
      .execute();
  }

  // success cases
  @Test
  public void set_global_period_to_previous_version() {
    logInAsSystemAdministrator();
    ws.newRequest()
      .setParam("type", "previous_version")
      .execute();
    // TODO

  }

  @Test
  public void set_project_period_to_number_of_days() {
    ComponentDto project = componentDb.insertMainBranch();
    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "days")
      .setParam("value", "5")
      .execute();
    // TODO

  }

  @Test
  public void set_branch_period_to_analysis() {
    ComponentDto project = componentDb.insertMainBranch();
    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));

    SnapshotDto analysisMaster = db.components().insertSnapshot(project);
    SnapshotDto analysisBranch = db.components().insertSnapshot(branch);

    logInAsProjectAdministrator(project);

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .setParam("type", "analysis")
      .setParam("branchKey", "branch")
      .setParam("value", analysisBranch.getUuid())
      .execute();
    // TODO
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
