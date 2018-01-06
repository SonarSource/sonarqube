/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.projectanalysis.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_ANALYSIS;

public class DeleteActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private WsActionTester ws = new WsActionTester(new DeleteAction(dbClient, userSession));

  @Test
  public void project_administrator_deletes_analysis() {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(project).setUuid("A1").setLast(false).setStatus(STATUS_PROCESSED));
    db.components().insertSnapshot(newAnalysis(project).setUuid("A2").setLast(true).setStatus(STATUS_PROCESSED));
    logInAsProjectAdministrator(project);

    call("A1");

    db.commit();
    assertThat(dbClient.snapshotDao().selectByUuids(dbSession, newArrayList("A1", "A2"))).extracting(SnapshotDto::getUuid, SnapshotDto::getStatus).containsExactly(
      tuple("A1", STATUS_UNPROCESSED),
      tuple("A2", STATUS_PROCESSED));
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("delete");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.param("analysis").isRequired()).isTrue();
  }

  @Test
  public void last_analysis_cannot_be_deleted() {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(project).setUuid("A1").setLast(true));
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The last analysis 'A1' cannot be deleted");

    call("A1");
  }

  @Test
  public void fail_when_analysis_not_found() {
    userSession.logIn().setRoot();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Analysis 'A42' not found");

    call("A42");
  }

  @Test
  public void fail_when_analysis_is_unprocessed() {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(project).setUuid("A1").setLast(false).setStatus(STATUS_UNPROCESSED));
    logInAsProjectAdministrator(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Analysis 'A1' not found");

    call("A1");
  }

  @Test
  public void fail_when_not_enough_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(project).setUuid("A1").setLast(false));
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    call("A1");
  }

  private void call(String analysis) {
    ws.newRequest()
      .setParam(PARAM_ANALYSIS, analysis)
      .execute();
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }
}
