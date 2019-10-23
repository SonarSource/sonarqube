/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.newcodeperiod.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class SetActionTest {
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
  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private NewCodePeriodDao dao = new NewCodePeriodDao(System2.INSTANCE, UuidFactoryFast.getInstance());

  private SetAction underTest = new SetAction(dbClient, userSession, componentFinder, editionProvider, dao);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("set");
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.since()).isEqualTo("8.0");
    assertThat(definition.isPost()).isTrue();

    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("value", "type", "project", "branch");
    assertThat(definition.param("value").isRequired()).isFalse();
    assertThat(definition.param("type").isRequired()).isTrue();
    assertThat(definition.param("project").isRequired()).isFalse();
    assertThat(definition.param("branch").isRequired()).isFalse();
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
    expectedException.expectMessage("Invalid type 'SPECIFIC_ANALYSIS'. Overall setting can only be set with types: [PREVIOUS_VERSION, NUMBER_OF_DAYS]");

    ws.newRequest()
      .setParam("type", "specific_analysis")
      .execute();
  }

  @Test
  public void throw_IAE_if_type_is_invalid_for_project() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid type 'SPECIFIC_ANALYSIS'. Projects can only be set with types: [PREVIOUS_VERSION, NUMBER_OF_DAYS]");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "specific_analysis")
      .execute();
  }

  @Test
  public void throw_IAE_if_no_value_for_days() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("New Code Period type 'NUMBER_OF_DAYS' requires a value");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "master")
      .setParam("type", "number_of_days")
      .execute();
  }

  @Test
  public void throw_IAE_if_no_value_for_analysis() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("New Code Period type 'SPECIFIC_ANALYSIS' requires a value");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "specific_analysis")
      .setParam("branch", "master")
      .execute();
  }

  @Test
  public void throw_IAE_if_days_is_invalid() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Failed to parse number of days: unknown");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "number_of_days")
      .setParam("branch", "master")
      .setParam("value", "unknown")
      .execute();
  }

  @Test
  public void throw_IAE_if_analysis_is_not_found() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Analysis 'unknown' is not found");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "specific_analysis")
      .setParam("branch", "master")
      .setParam("value", "unknown")
      .execute();
  }

  @Test
  public void throw_IAE_if_analysis_doesnt_belong_to_branch() {
    ComponentDto project = componentDb.insertPublicProject();
    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));

    SnapshotDto analysisMaster = db.components().insertSnapshot(project);
    SnapshotDto analysisBranch = db.components().insertSnapshot(branch);

    logInAsProjectAdministrator(project);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Analysis '" + analysisBranch.getUuid() + "' does not belong to branch 'master' of project '" + project.getKey() + "'");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "specific_analysis")
      .setParam("branch", "master")
      .setParam("value", analysisBranch.getUuid())
      .execute();
  }

  // validation of project/branch
  @Test
  public void throw_IAE_if_branch_is_specified_without_project() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If branch key is specified, project key needs to be specified too");

    ws.newRequest()
      .setParam("branch", "branch")
      .execute();
  }

  @Test
  public void throw_NFE_if_project_not_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project 'unknown' not found");

    ws.newRequest()
      .setParam("type", "previous_version")
      .setParam("project", "unknown")
      .execute();
  }

  @Test
  public void throw_NFE_if_branch_not_found() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Branch 'unknown' in project '" + project.getKey() + "' not found");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "previous_version")
      .setParam("branch", "unknown")
      .execute();
  }

  // permission
  @Test
  public void throw_NFE_if_no_project_permission() {
    ComponentDto project = componentDb.insertPublicProject();
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setParam("project", project.getKey())
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

    assertTableContainsOnly(null, null, NewCodePeriodType.PREVIOUS_VERSION, null);
  }

  @Test
  public void set_project_period_to_number_of_days() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "number_of_days")
      .setParam("value", "5")
      .execute();
    assertTableContainsOnly(project.uuid(), null, NewCodePeriodType.NUMBER_OF_DAYS, "5");
  }

  @Test
  @UseDataProvider("provideNewCodePeriodTypeAndValue")
  public void never_set_project_value_in_community_edition(NewCodePeriodType type, @Nullable String value) {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    ComponentDto project = componentDb.insertPublicProject();

    if (value != null && NewCodePeriodType.SPECIFIC_ANALYSIS.equals(type)) {
      db.components().insertSnapshot(project, snapshotDto -> snapshotDto.setUuid(value));
    }

    logInAsProjectAdministrator(project);
    TestRequest request = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", type.name());

    if (value != null) {
      request.setParam("value", value);
    }

    request.execute();
    assertTableContainsOnly(project.uuid(), project.uuid(), type, value);
  }

  @DataProvider
  public static Object[][] provideNewCodePeriodTypeAndValue() {
    return new Object[][]{
      {NewCodePeriodType.NUMBER_OF_DAYS, "5"},
      {NewCodePeriodType.SPECIFIC_ANALYSIS, "analysis-uuid"},
      {NewCodePeriodType.PREVIOUS_VERSION, null}
    };
  }

  @Test
  public void set_project_twice_period_to_number_of_days() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "previous_version")
      .execute();
    assertTableContainsOnly(project.uuid(), null, NewCodePeriodType.PREVIOUS_VERSION, null);

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "number_of_days")
      .setParam("value", "5")
      .execute();
    assertTableContainsOnly(project.uuid(), null, NewCodePeriodType.NUMBER_OF_DAYS, "5");
  }

  @Test
  public void set_branch_period_to_analysis() {
    ComponentDto project = componentDb.insertPublicProject();
    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));

    SnapshotDto analysisMaster = db.components().insertSnapshot(project);
    SnapshotDto analysisBranch = db.components().insertSnapshot(branch);

    logInAsProjectAdministrator(project);

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "specific_analysis")
      .setParam("branch", "branch")
      .setParam("value", analysisBranch.getUuid())
      .execute();

    assertTableContainsOnly(project.uuid(), branch.uuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, analysisBranch.getUuid());
  }

  @Test
  public void set_branch_period_twice_to_analysis() {
    ComponentDto project = componentDb.insertPublicProject();
    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));

    SnapshotDto analysisMaster = db.components().insertSnapshot(project);
    SnapshotDto analysisBranch = db.components().insertSnapshot(branch);

    logInAsProjectAdministrator(project);

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "specific_analysis")
      .setParam("branch", "branch")
      .setParam("value", analysisBranch.getUuid())
      .execute();

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "previous_version")
      .setParam("branch", "branch")
      .execute();

    assertTableContainsOnly(project.uuid(), branch.uuid(), NewCodePeriodType.PREVIOUS_VERSION, null);
  }

  private void assertTableContainsOnly(@Nullable String projectUuid, @Nullable String branchUuid, NewCodePeriodType type, @Nullable String value) {
    assertThat(db.countRowsOfTable(dbSession, "new_code_periods")).isEqualTo(1);
    assertThat(db.selectFirst(dbSession, "select project_uuid, branch_uuid, type, value from new_code_periods"))
      .containsOnly(entry("PROJECT_UUID", projectUuid), entry("BRANCH_UUID", branchUuid), entry("TYPE", type.name()), entry("VALUE", value));
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
