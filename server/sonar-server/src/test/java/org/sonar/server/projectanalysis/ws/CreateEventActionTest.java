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
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.event.EventDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.ProjectAnalyses;
import org.sonarqube.ws.ProjectAnalyses.CreateEventResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newApplication;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.component.SnapshotTesting.newSnapshot;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.WsRequest.Method.POST;
import static org.sonar.server.projectanalysis.ws.EventCategory.OTHER;
import static org.sonar.server.projectanalysis.ws.EventCategory.VERSION;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_ANALYSIS;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_CATEGORY;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_NAME;

public class CreateEventActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private System2 system = mock(System2.class);

  private WsActionTester ws = new WsActionTester(new CreateEventAction(dbClient, uuidFactory, system, userSession));

  @Before
  public void setUp() {
    when(system.now()).thenReturn(42L);
  }

  @Test
  public void json_example() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto analysis = dbClient.snapshotDao().insert(dbSession, SnapshotTesting.newAnalysis(project).setUuid("A2"));
    db.commit();
    uuidFactory = mock(UuidFactory.class);
    when(uuidFactory.create()).thenReturn("E1");
    ws = new WsActionTester(new CreateEventAction(dbClient, uuidFactory, system, userSession));
    logInAsProjectAdministrator(project);

    String result = ws.newRequest()
      .setParam(PARAM_ANALYSIS, analysis.getUuid())
      .setParam(PARAM_CATEGORY, OTHER.name())
      .setParam(PARAM_NAME, "My Custom Event")
      .execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("create_event-example.json"));
  }

  @Test
  public void create_event_in_db() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    when(system.now()).thenReturn(123_456_789L);
    logInAsProjectAdministrator(project);

    CreateEventResponse result = call(VERSION.name(), "5.6.3", analysis.getUuid());

    List<EventDto> dbEvents = dbClient.eventDao().selectByComponentUuid(dbSession, analysis.getComponentUuid());
    assertThat(dbEvents).hasSize(1);
    EventDto dbEvent = dbEvents.get(0);
    assertThat(dbEvent.getName()).isEqualTo("5.6.3");
    assertThat(dbEvent.getCategory()).isEqualTo(VERSION.getLabel());
    assertThat(dbEvent.getDescription()).isNull();
    assertThat(dbEvent.getAnalysisUuid()).isEqualTo(analysis.getUuid());
    assertThat(dbEvent.getComponentUuid()).isEqualTo(analysis.getComponentUuid());
    assertThat(dbEvent.getUuid()).isEqualTo(result.getEvent().getKey());
    assertThat(dbEvent.getCreatedAt()).isEqualTo(123_456_789L);
    assertThat(dbEvent.getDate()).isEqualTo(analysis.getCreatedAt());
  }

  @Test
  public void create_event_as_project_admin() {
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), "P1");
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    logInAsProjectAdministrator(project);

    CreateEventResponse result = call(VERSION.name(), "5.6.3", analysis.getUuid());

    assertThat(result.getEvent().getKey()).isNotEmpty();
  }

  @Test
  public void create_version_event() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    logInAsProjectAdministrator(project);

    call(VERSION.name(), "5.6.3", analysis.getUuid());

    Optional<SnapshotDto> newAnalysis = dbClient.snapshotDao().selectByUuid(dbSession, analysis.getUuid());
    assertThat(newAnalysis.get().getVersion()).isEqualTo("5.6.3");
  }

  @Test
  public void create_other_event_with_ws_response() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    logInAsProjectAdministrator(project);

    CreateEventResponse result = call(OTHER.name(), "Project Import", analysis.getUuid());

    SnapshotDto newAnalysis = dbClient.snapshotDao().selectByUuid(dbSession, analysis.getUuid()).get();
    assertThat(analysis.getVersion()).isEqualTo(newAnalysis.getVersion());
    ProjectAnalyses.Event wsEvent = result.getEvent();
    assertThat(wsEvent.getKey()).isNotEmpty();
    assertThat(wsEvent.getCategory()).isEqualTo(OTHER.name());
    assertThat(wsEvent.getName()).isEqualTo("Project Import");
    assertThat(wsEvent.hasDescription()).isFalse();
    assertThat(wsEvent.getAnalysis()).isEqualTo(analysis.getUuid());
  }

  @Test
  public void create_event_without_description() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    logInAsProjectAdministrator(project);

    CreateEventResponse result = call(OTHER.name(), "Project Import", analysis.getUuid());

    ProjectAnalyses.Event event = result.getEvent();
    assertThat(event.getKey()).isNotEmpty();
    assertThat(event.hasDescription()).isFalse();
  }

  @Test
  public void create_event_on_application() {
    ComponentDto application = ComponentTesting.newApplication(db.getDefaultOrganization());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(application);
    logInAsProjectAdministrator(application);

    CreateEventResponse result = call(OTHER.name(), "Application Event", analysis.getUuid());

    ProjectAnalyses.Event event = result.getEvent();
    assertThat(event.getName()).isEqualTo("Application Event");
  }

  @Test
  public void create_2_version_events_on_same_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto firstAnalysis = db.components().insertProjectAndSnapshot(project);
    SnapshotDto secondAnalysis = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    db.commit();
    logInAsProjectAdministrator(project);

    call(VERSION.name(), "5.6.3", firstAnalysis.getUuid());
    call(VERSION.name(), "6.3", secondAnalysis.getUuid());

    List<EventDto> events = dbClient.eventDao().selectByComponentUuid(dbSession, project.uuid());
    assertThat(events).hasSize(2);
  }

  @Test
  public void fail_if_not_blank_name() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A non empty name is required");

    call(OTHER.name(), "    ", analysis.getUuid());
  }

  @Test
  public void fail_if_analysis_is_not_found() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Analysis 'A42' is not found");

    call(OTHER.name(), "Project Import", "A42");
  }

  @Test
  public void fail_if_2_version_events_on_the_same_analysis() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    logInAsProjectAdministrator(project);
    call(VERSION.name(), "5.6.3", analysis.getUuid());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A version event already exists on analysis '" + analysis.getUuid() + "'");

    call(VERSION.name(), "6.3", analysis.getUuid());
  }

  @Test
  public void fail_if_2_other_events_on_same_analysis_with_same_name() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    logInAsProjectAdministrator(project);
    call(OTHER.name(), "Project Import", analysis.getUuid());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("An 'Other' event with the same name already exists on analysis '" + analysis.getUuid() + "'");

    call(OTHER.name(), "Project Import", analysis.getUuid());
  }

  @Test
  public void fail_if_category_other_than_authorized() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    logInAsProjectAdministrator(project);

    expectedException.expect(IllegalArgumentException.class);
    ws.newRequest()
      .setParam(PARAM_ANALYSIS, analysis.getUuid())
      .setParam(PARAM_NAME, "Project Import")
      .setParam(PARAM_CATEGORY, "QP")
      .execute();
  }

  @Test
  public void fail_if_create_on_view() {
    ComponentDto view = newView(db.organizations().insert());
    SnapshotDto analysis = db.components().insertViewAndSnapshot(view);
    logInAsProjectAdministrator(view);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("An event must be created on a project or an application");

    call(OTHER.name(), "View Event", analysis.getUuid());
  }

  @Test
  public void fail_if_create_version_event_on_application() {
    ComponentDto application = newApplication(db.organizations().insert());
    SnapshotDto analysis = db.components().insertViewAndSnapshot(application);
    logInAsProjectAdministrator(application);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A version event must be created on a project");

    call(VERSION.name(), "5.6.3", analysis.getUuid());
  }

  @Test
  public void fail_if_project_is_not_found() {
    userSession.logIn();
    SnapshotDto analysis = dbClient.snapshotDao().insert(dbSession, newSnapshot().setUuid("A1"));
    db.commit();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Project of analysis 'A1' is not found");

    call(VERSION.name(), "5.6.3", analysis.getUuid());
  }

  @Test
  public void throw_ForbiddenException_if_not_project_administrator() {
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(newPrivateProjectDto(db.organizations().insert(), "P1"));
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call(VERSION.name(), "5.6.3", analysis.getUuid());
  }

  @Test
  public void ws_parameters() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.isPost()).isTrue();
    assertThat(definition.key()).isEqualTo("create_event");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

  private CreateEventResponse call(String categoryName, String name, String analysis) {
    TestRequest httpRequest = ws.newRequest()
      .setMethod(POST.name());

    httpRequest.setParam(PARAM_CATEGORY, categoryName)
      .setParam(PARAM_NAME, name)
      .setParam(PARAM_ANALYSIS, analysis);

    return httpRequest.executeProtobuf(CreateEventResponse.class);
  }
}
