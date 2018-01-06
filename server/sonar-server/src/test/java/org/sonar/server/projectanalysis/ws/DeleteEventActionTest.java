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

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.event.EventTesting.newEvent;
import static org.sonar.server.projectanalysis.ws.EventCategory.VERSION;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_EVENT;

public class DeleteEventActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private WsActionTester ws = new WsActionTester(new DeleteEventAction(db.getDbClient(), userSession));

  @Test
  public void delete_event() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    db.events().insertEvent(newEvent(analysis).setUuid("E1"));
    db.events().insertEvent(newEvent(analysis).setUuid("E2"));
    logInAsProjectAdministrator(project);

    call("E2");

    List<EventDto> events = db.getDbClient().eventDao().selectByAnalysisUuid(db.getSession(), analysis.getUuid());
    assertThat(events).extracting(EventDto::getUuid).containsExactly("E1");
  }

  @Test
  public void delete_version_event() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto analysis = db.components().insertSnapshot(newAnalysis(project).setVersion("5.6.3").setLast(false));
    db.events().insertEvent(newEvent(analysis).setUuid("E1").setCategory(VERSION.getLabel()));
    logInAsProjectAdministrator(project);

    call("E1");

    SnapshotDto newAnalysis = dbClient.snapshotDao().selectByUuid(dbSession, analysis.getUuid()).get();
    assertThat(newAnalysis.getVersion()).isNull();
  }

  @Test
  public void fail_if_version_for_last_analysis() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto analysis = db.components().insertSnapshot(newAnalysis(project).setVersion("5.6.3").setLast(true));
    db.events().insertEvent(newEvent(analysis).setUuid("E1").setCategory(VERSION.getLabel()));
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Cannot delete the version event of last analysis");

    call("E1");
  }

  @Test
  public void fail_if_category_different_than_other_and_version() {
    ComponentDto project = newPrivateProjectDto(db.organizations().insert(), "P1");
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    db.events().insertEvent(newEvent(analysis).setUuid("E1").setCategory("Profile"));
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Event of category 'QUALITY_PROFILE' cannot be modified. Authorized categories: VERSION, OTHER");

    call("E1");
  }

  @Test
  public void fail_if_event_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("E42' not found");

    call("E42");
  }

  @Test
  public void fail_if_not_enough_permission() {
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(db.organizations().insert()));
    db.events().insertEvent(newEvent(analysis).setUuid("E1"));
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    call("E1");
  }

  @Test
  public void fail_if_event_not_provided() {
    expectedException.expect(IllegalArgumentException.class);

    call(null);
  }

  @Test
  public void ws_definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition.key()).isEqualTo("delete_event");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.param(PARAM_EVENT).isRequired()).isTrue();
  }

  private void call(@Nullable String event) {
    TestRequest request = ws.newRequest();
    if (event != null) {
      request.setParam(PARAM_EVENT, event);
    }

    request.execute();
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }
}
