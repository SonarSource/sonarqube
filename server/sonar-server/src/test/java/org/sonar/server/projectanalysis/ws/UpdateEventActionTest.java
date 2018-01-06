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
import org.sonarqube.ws.ProjectAnalyses;
import org.sonarqube.ws.ProjectAnalyses.UpdateEventResponse;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.event.EventTesting.newEvent;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.WsRequest.Method.POST;
import static org.sonar.server.projectanalysis.ws.EventCategory.OTHER;
import static org.sonar.server.projectanalysis.ws.EventCategory.VERSION;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_EVENT;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_NAME;

public class UpdateEventActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private WsActionTester ws = new WsActionTester(new UpdateEventAction(dbClient, userSession));

  @Test
  public void json_example() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto analysis = db.components().insertSnapshot(newAnalysis(project).setUuid("A2"));
    db.events().insertEvent(newEvent(analysis)
      .setUuid("E1")
      .setCategory(OTHER.getLabel())
      .setName("Original Name")
      .setDescription("Original Description"));
    logInAsProjectAdministrator(project);

    String result = ws.newRequest()
      .setParam(PARAM_EVENT, "E1")
      .setParam(PARAM_NAME, "My Custom Event")
      .execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("update_event-example.json"));
  }

  @Test
  public void update_name_in_db() {
    SnapshotDto analysis = createAnalysisAndLogInAsProjectAdministrator("5.6");
    EventDto originalEvent = db.events().insertEvent(newEvent(analysis).setUuid("E1").setName("Original Name"));

    call("E1", "name");

    EventDto newEvent = dbClient.eventDao().selectByUuid(dbSession, "E1").get();
    assertThat(newEvent.getName()).isEqualTo("name");
    assertThat(newEvent.getDescription()).isNull();
    assertThat(newEvent.getCategory()).isEqualTo(originalEvent.getCategory());
    assertThat(newEvent.getDate()).isEqualTo(originalEvent.getDate());
    assertThat(newEvent.getCreatedAt()).isEqualTo(originalEvent.getCreatedAt());
  }

  @Test
  public void ws_response_with_updated_name() {
    SnapshotDto analysis = createAnalysisAndLogInAsProjectAdministrator("5.6");
    EventDto originalEvent = db.events().insertEvent(newEvent(analysis).setUuid("E1").setName("Original Name"));

    ProjectAnalyses.Event result = call("E1", "name").getEvent();

    assertThat(result.getName()).isEqualTo("name");
    assertThat(result.hasDescription()).isFalse();
    assertThat(result.getCategory()).isEqualTo(OTHER.name());
    assertThat(result.getAnalysis()).isEqualTo(originalEvent.getAnalysisUuid());
    assertThat(result.getKey()).isEqualTo("E1");
  }

  @Test
  public void update_VERSION_event_update_analysis_version() {
    SnapshotDto analysis = createAnalysisAndLogInAsProjectAdministrator("5.6");
    db.events().insertEvent(newEvent(analysis).setUuid("E1").setCategory(VERSION.getLabel()));

    call("E1", "6.3");

    SnapshotDto updatedAnalysis = dbClient.snapshotDao().selectByUuid(dbSession, analysis.getUuid()).get();
    assertThat(updatedAnalysis.getVersion()).isEqualTo("6.3");
  }

  @Test
  public void update_OTHER_event_does_not_update_analysis_version() {
    SnapshotDto analysis = createAnalysisAndLogInAsProjectAdministrator("5.6");
    db.events().insertEvent(newEvent(analysis).setUuid("E1").setCategory(OTHER.getLabel()));

    call("E1", "6.3");

    SnapshotDto updatedAnalysis = dbClient.snapshotDao().selectByUuid(dbSession, analysis.getUuid()).get();
    assertThat(updatedAnalysis.getVersion()).isEqualTo("5.6");
  }

  @Test
  public void update_name_only_in_db() {
    SnapshotDto analysis = createAnalysisAndLogInAsProjectAdministrator("5.6");
    EventDto originalEvent = db.events().insertEvent(newEvent(analysis).setUuid("E1").setName("Original Name").setDescription("Original Description"));

    call("E1", "name");

    EventDto newEvent = dbClient.eventDao().selectByUuid(dbSession, "E1").get();
    assertThat(newEvent.getName()).isEqualTo("name");
    assertThat(newEvent.getDescription()).isEqualTo(originalEvent.getDescription());
  }

  @Test
  public void test_ws_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("update_event");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.since()).isEqualTo("6.3");
    assertThat(definition.param(PARAM_EVENT).isRequired()).isTrue();
  }

  @Test
  public void throw_ForbiddenException_if_not_project_administrator() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    db.events().insertEvent(newEvent(analysis).setUuid("E1"));
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    expectedException.expect(ForbiddenException.class);

    call("E1", "name");
  }

  @Test
  public void fail_if_event_is_not_found() {
    userSession.logIn().setSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Event 'E42' not found");

    call("E42", "name");
  }

  @Test
  public void fail_if_no_name() {
    SnapshotDto analysis = createAnalysisAndLogInAsProjectAdministrator("5.6");
    db.events().insertEvent(newEvent(analysis).setUuid("E1"));

    expectedException.expect(NullPointerException.class);

    call("E1", null);
  }

  @Test
  public void fail_if_blank_name() {
    SnapshotDto analysis = createAnalysisAndLogInAsProjectAdministrator("5.6");
    db.events().insertEvent(newEvent(analysis).setUuid("E1"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A non empty name is required");

    call("E1", "     ");
  }

  @Test
  public void fail_if_category_other_than_other_or_version() {
    SnapshotDto analysis = createAnalysisAndLogInAsProjectAdministrator("5.6");
    db.events().insertEvent(newEvent(analysis).setUuid("E1").setCategory("Profile"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Event of category 'QUALITY_PROFILE' cannot be modified. Authorized categories: VERSION, OTHER");

    call("E1", "name");
  }

  @Test
  public void fail_if_other_event_with_same_name_on_same_analysis() {
    SnapshotDto analysis = createAnalysisAndLogInAsProjectAdministrator("5.6");
    db.events().insertEvent(newEvent(analysis).setUuid("E1").setCategory(OTHER.getLabel()).setName("E1 name"));
    db.events().insertEvent(newEvent(analysis).setUuid("E2").setCategory(OTHER.getLabel()).setName("E2 name"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("An 'Other' event with the same name already exists on analysis '" + analysis.getUuid() + "'");

    call("E2", "E1 name");
  }

  @Test
  public void limit_version_name_length_to_100_for_analysis_events() {
    SnapshotDto analysis = createAnalysisAndLogInAsProjectAdministrator("5.6");
    db.events().insertEvent(newEvent(analysis).setUuid("E1").setCategory(OTHER.getLabel()).setName("E1 name"));
    db.events().insertEvent(newEvent(analysis).setUuid("E2").setCategory(VERSION.getLabel()).setName("E2 name"));

    call("E1", repeat("a", 100));
    call("E1", repeat("a", 101));
    call("E2", repeat("a", 100));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Version length (101) is longer than the maximum authorized (100). 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' was provided");

    call("E2", repeat("a", 101));
  }

  private UpdateEventResponse call(@Nullable String eventUuid, @Nullable String name) {
    TestRequest request = ws.newRequest()
      .setMethod(POST.name());
    setNullable(eventUuid, e -> request.setParam(PARAM_EVENT, e));
    setNullable(name, n -> request.setParam(PARAM_NAME, n));

    return request.executeProtobuf(UpdateEventResponse.class);
  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

  private SnapshotDto createAnalysisAndLogInAsProjectAdministrator(String version) {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto analysis = db.components().insertSnapshot(newAnalysis(project).setVersion(version));
    logInAsProjectAdministrator(project);
    return analysis;
  }
}
