/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
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
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnsetActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentFinder componentFinder = TestComponentFinder.from(db);
  private NewCodePeriodDao dao = new NewCodePeriodDao(System2.INSTANCE, UuidFactoryFast.getInstance());
  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);

  private UnsetAction underTest = new UnsetAction(dbClient, userSession, componentFinder, editionProvider, dao);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("unset");
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.since()).isEqualTo("8.0");
    assertThat(definition.isPost()).isTrue();

    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("project", "branch");
    assertThat(definition.param("project").isRequired()).isFalse();
    assertThat(definition.param("branch").isRequired()).isFalse();
  }

  // validation of project/branch
  @Test
  public void throw_IAE_if_branch_is_specified_without_project() {
    assertThatThrownBy(() -> ws.newRequest()
      .setParam("branch", "branch")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("If branch key is specified, project key needs to be specified too");
  }

  @Test
  public void throw_NFE_if_project_not_found() {
    assertThatThrownBy(() -> ws.newRequest()
      .setParam("type", "previous_version")
      .setParam("project", "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Project 'unknown' not found");
  }

  @Test
  public void throw_NFE_if_branch_not_found() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "previous_version")
      .setParam("branch", "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Branch 'unknown' in project '" + project.getKey() + "' not found");
  }

  // permission
  @Test
  public void throw_NFE_if_no_project_permission() {
    ComponentDto project = componentDb.insertPublicProject();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("type", "previous_version")
      .execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void throw_NFE_if_no_system_permission() {
    assertThatThrownBy(() -> ws.newRequest()
      .setParam("type", "previous_version")
      .execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  // success cases
  @Test
  public void delete_global_period() {
    logInAsSystemAdministrator();
    ws.newRequest()
      .execute();

    assertTableEmpty();
  }

  @Test
  public void delete_project_period() {
    ComponentDto project = componentDb.insertPublicProject();
    logInAsProjectAdministrator(project);
    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    assertTableEmpty();
  }

  @Test
  public void delete_project_period_twice() {
    ComponentDto project1 = componentDb.insertPublicProject();
    ComponentDto project2 = componentDb.insertPublicProject();
    db.newCodePeriods().insert(project1.uuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid1");
    db.newCodePeriods().insert(project2.uuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid2");

    logInAsProjectAdministrator(project1);
    ws.newRequest()
      .setParam("project", project1.getKey())
      .execute();
    assertTableContainsOnly(project2.uuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid2");

    ws.newRequest()
      .setParam("project", project1.getKey())
      .execute();

    assertTableContainsOnly(project2.uuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid2");
  }

  @Test
  public void delete_branch_period() {
    ComponentDto project = componentDb.insertPublicProject();
    ComponentDto branch = componentDb.insertProjectBranch(project, b -> b.setKey("branch"));

    db.newCodePeriods().insert(project.uuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid1");
    db.newCodePeriods().insert(project.uuid(), branch.uuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid2");

    logInAsProjectAdministrator(project);

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "branch")
      .execute();

    assertTableContainsOnly(project.uuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid1");
  }

  @Test
  public void delete_branch_and_project_period_in_community_edition() {
    ComponentDto project = componentDb.insertPublicProject();

    db.newCodePeriods().insert(project.uuid(), null, NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid1");
    db.newCodePeriods().insert(project.uuid(), project.uuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid2");

    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));

    logInAsProjectAdministrator(project);

    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    assertTableEmpty();
  }

  private void assertTableEmpty() {
    assertThat(db.countRowsOfTable(dbSession, "new_code_periods")).isZero();
  }

  private void assertTableContainsOnly(@Nullable String projectUuid, @Nullable String branchUuid, NewCodePeriodType type, @Nullable String value) {
    assertThat(db.countRowsOfTable(dbSession, "new_code_periods")).isOne();
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
