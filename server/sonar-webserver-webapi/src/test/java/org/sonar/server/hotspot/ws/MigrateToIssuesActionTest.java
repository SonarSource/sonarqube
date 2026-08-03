/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.hotspot.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MigrateToIssuesActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final HotspotsToIssuesMigrator migrator = mock(HotspotsToIssuesMigrator.class);
  private final WsActionTester actionTester = new WsActionTester(new MigrateToIssuesAction(userSession, migrator));

  @Test
  public void definition() {
    WebService.Action action = actionTester.getDef();

    assertThat(action.key()).isEqualTo("migrate_to_issues");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("2026.4");

    WebService.Param projectParam = action.param("project");
    assertThat(projectParam).isNotNull();
    assertThat(projectParam.isRequired()).isFalse();

    WebService.Param dryRunParam = action.param("dryRun");
    assertThat(dryRunParam).isNotNull();
    assertThat(dryRunParam.isRequired()).isFalse();
    assertThat(dryRunParam.defaultValue()).isEqualTo("false");
  }

  @Test
  public void handle_whenAnonymous_throwsForbiddenException() {
    TestRequest testRequest = actionTester.newRequest();
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void handle_whenNotSystemAdmin_throwsForbiddenException() {
    userSession.logIn().setNonSystemAdministrator();

    TestRequest testRequest = actionTester.newRequest();
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void handle_whenSystemAdmin_callsMigratorAndReturnsJson() {
    userSession.logIn().setSystemAdministrator();
    when(migrator.migrate(null, false)).thenReturn(new HotspotsToIssuesMigrator.MigrationResult(false, List.of()));

    String result = actionTester.newRequest().execute().getInput();

    verify(migrator).migrate(null, false);
    JsonAssert.assertJson(result).isSimilarTo("{\"dryRun\":false,\"projects\":[]}");
  }

  @Test
  public void handle_whenProjectParamProvided_passesThroughToMigrator() {
    userSession.logIn().setSystemAdministrator();
    when(migrator.migrate("my-project", false))
      .thenReturn(new HotspotsToIssuesMigrator.MigrationResult(false,
        List.of(new HotspotsToIssuesMigrator.ProjectMigrationResult("my-project", 0, 0))));

    actionTester.newRequest().setParam("project", "my-project").execute();

    verify(migrator).migrate("my-project", false);
  }

  @Test
  public void handle_whenDryRunTrue_passesThroughToMigrator() {
    userSession.logIn().setSystemAdministrator();
    when(migrator.migrate(null, true)).thenReturn(new HotspotsToIssuesMigrator.MigrationResult(true, List.of()));

    actionTester.newRequest().setParam("dryRun", "true").execute();

    verify(migrator).migrate(null, true);
  }
}
