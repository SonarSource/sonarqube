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
package org.sonar.server.measure.custom.ws;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ws.UserJsonWriter;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();

  private WsActionTester ws = new WsActionTester(
    new SearchAction(db.getDbClient(), new CustomMeasureJsonWriter(new UserJsonWriter(userSession)), userSession, TestComponentFinder.from(db)));

  @Test
  public void json_well_formatted() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);
    UserDto userMeasureCreator = db.users().insertUser();
    MetricDto metric1 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    MetricDto metric2 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    MetricDto metric3 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    CustomMeasureDto customMeasure1 = db.measures().insertCustomMeasure(userMeasureCreator, project, metric1, m -> m.setValue(0d));
    CustomMeasureDto customMeasure2 = db.measures().insertCustomMeasure(userMeasureCreator, project, metric2, m -> m.setValue(0d));
    CustomMeasureDto customMeasure3 = db.measures().insertCustomMeasure(userMeasureCreator, project, metric3, m -> m.setValue(0d));

    String response = ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, project.uuid())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"customMeasures\": [\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure1.getId() + "\",\n" +
      "      \"value\": \"" + customMeasure1.getTextValue() + "\",\n" +
      "      \"description\": \"" + customMeasure1.getDescription() + "\",\n" +
      "      \"metric\": {\n" +
      "        \"id\": \"" + metric1.getId() + "\",\n" +
      "        \"key\": \"" + metric1.getKey() + "\",\n" +
      "        \"type\": \"" + metric1.getValueType() + "\",\n" +
      "        \"name\": \"" + metric1.getShortName() + "\",\n" +
      "        \"domain\": \"" + metric1.getDomain() + "\"\n" +
      "      },\n" +
      "      \"projectId\": \"" + project.uuid() + "\",\n" +
      "      \"projectKey\": \"" + project.getKey() + "\",\n" +
      "      \"pending\": true,\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure2.getId() + "\",\n" +
      "      \"value\": \"" + customMeasure2.getTextValue() + "\",\n" +
      "      \"description\": \"" + customMeasure2.getDescription() + "\",\n" +
      "      \"metric\": {\n" +
      "        \"id\": \"" + metric2.getId() + "\",\n" +
      "        \"key\": \"" + metric2.getKey() + "\",\n" +
      "        \"type\": \"" + metric2.getValueType() + "\",\n" +
      "        \"name\": \"" + metric2.getShortName() + "\",\n" +
      "        \"domain\": \"" + metric2.getDomain() + "\"\n" +
      "      },\n" +
      "      \"projectId\": \"" + project.uuid() + "\",\n" +
      "      \"projectKey\": \"" + project.getKey() + "\",\n" +
      "      \"pending\": true,\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure3.getId() + "\",\n" +
      "      \"value\": \"" + customMeasure3.getTextValue() + "\",\n" +
      "      \"description\": \"" + customMeasure3.getDescription() + "\",\n" +
      "      \"metric\": {\n" +
      "        \"id\": \"" + metric3.getId() + "\",\n" +
      "        \"key\": \"" + metric3.getKey() + "\",\n" +
      "        \"type\": \"" + metric3.getValueType() + "\",\n" +
      "        \"name\": \"" + metric3.getShortName() + "\",\n" +
      "        \"domain\": \"" + metric3.getDomain() + "\"\n" +
      "      },\n" +
      "      \"projectId\": \"" + project.uuid() + "\",\n" +
      "      \"projectKey\": \"" + project.getKey() + "\",\n" +
      "      \"pending\": true,\n" +
      "    }\n" +
      "  ],\n" +
      "  \"total\": 3,\n" +
      "  \"p\": 1,\n" +
      "  \"ps\": 100\n" +
      "}");
  }

  @Test
  public void return_users() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    MetricDto metric1 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    MetricDto metric2 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    MetricDto metric3 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    CustomMeasureDto customMeasure1 = db.measures().insertCustomMeasure(user1, project, metric1, m -> m.setValue(0d));
    CustomMeasureDto customMeasure2 = db.measures().insertCustomMeasure(user1, project, metric2, m -> m.setValue(0d));
    CustomMeasureDto customMeasure3 = db.measures().insertCustomMeasure(user2, project, metric3, m -> m.setValue(0d));

    String response = ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, project.uuid())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"customMeasures\": [\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure1.getId() + "\",\n" +
      "      \"user\": {\n" +
      "        \"login\": \"" + user1.getLogin() +"\",\n" +
      "        \"name\": \"" + user1.getName() +"\",\n" +
      "        \"email\": \"" + user1.getEmail() +"\",\n" +
      "        \"active\": true\n" +
      "      }" +
      "    },\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure2.getId() + "\",\n" +
      "      \"user\": {\n" +
      "        \"login\": \"" + user1.getLogin() +"\",\n" +
      "        \"name\": \"" + user1.getName() +"\",\n" +
      "        \"email\": \"" + user1.getEmail() +"\",\n" +
      "        \"active\": true\n" +
      "      }" +
      "    },\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure3.getId() + "\",\n" +
      "      \"user\": {\n" +
      "        \"login\": \"" + user2.getLogin() +"\",\n" +
      "        \"name\": \"" + user2.getName() +"\",\n" +
      "        \"email\": \"" + user2.getEmail() +"\",\n" +
      "        \"active\": true\n" +
      "      }" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void search_by_project_uuid() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);
    UserDto userMeasureCreator = db.users().insertUser();
    MetricDto metric1 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    MetricDto metric2 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    MetricDto metric3 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    CustomMeasureDto customMeasure1 = db.measures().insertCustomMeasure(userMeasureCreator, project, metric1, m -> m.setValue(0d));
    CustomMeasureDto customMeasure2 = db.measures().insertCustomMeasure(userMeasureCreator, project, metric2, m -> m.setValue(0d));
    CustomMeasureDto customMeasure3 = db.measures().insertCustomMeasure(userMeasureCreator, project, metric3, m -> m.setValue(0d));

    String response = ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, project.uuid())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"customMeasures\": [\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure1.getId() + "\",\n" +
      "      \"value\": \"" + customMeasure1.getTextValue() + "\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure2.getId() + "\",\n" +
      "      \"value\": \"" + customMeasure2.getTextValue() + "\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure3.getId() + "\",\n" +
      "      \"value\": \"" + customMeasure3.getTextValue() + "\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"total\": 3,\n" +
      "  \"p\": 1,\n" +
      "  \"ps\": 100\n" +
      "}");
  }

  @Test
  public void search_by_project_key() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);
    UserDto userMeasureCreator = db.users().insertUser();
    MetricDto metric1 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    MetricDto metric2 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    MetricDto metric3 = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    CustomMeasureDto customMeasure1 = db.measures().insertCustomMeasure(userMeasureCreator, project, metric1, m -> m.setValue(0d));
    CustomMeasureDto customMeasure2 = db.measures().insertCustomMeasure(userMeasureCreator, project, metric2, m -> m.setValue(0d));
    CustomMeasureDto customMeasure3 = db.measures().insertCustomMeasure(userMeasureCreator, project, metric3, m -> m.setValue(0d));

    String response = ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_KEY, project.getKey())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"customMeasures\": [\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure1.getId() + "\",\n" +
      "      \"value\": \"" + customMeasure1.getTextValue() + "\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure2.getId() + "\",\n" +
      "      \"value\": \"" + customMeasure2.getTextValue() + "\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure3.getId() + "\",\n" +
      "      \"value\": \"" + customMeasure3.getTextValue() + "\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"total\": 3,\n" +
      "  \"p\": 1,\n" +
      "  \"ps\": 100\n" +
      "}");
  }

  @Test
  public void search_with_pagination() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);
    UserDto userMeasureCreator = db.users().insertUser();
    List<CustomMeasureDto> measureById = new ArrayList<>();
    IntStream.range(0, 10).forEach(i -> {
      MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true));
      CustomMeasureDto customMeasure = db.measures().insertCustomMeasure(userMeasureCreator, project, metric);
      measureById.add(customMeasure);
    });

    String response = ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_KEY, project.getKey())
      .setParam(WebService.Param.PAGE, "3")
      .setParam(WebService.Param.PAGE_SIZE, "4")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"customMeasures\": [\n" +
      "    {\n" +
      "      \"id\": \"" + measureById.get(8).getId() + "\",\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\": \"" + measureById.get(9).getId() + "\",\n" +
      "    },\n" +
      "  ],\n" +
      "  \"total\": 10,\n" +
      "  \"p\": 3,\n" +
      "  \"ps\": 4\n" +
      "}");
  }

  @Test
  public void search_with_selectable_fields() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);
    UserDto userMeasureCreator = db.users().insertUser();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    db.measures().insertCustomMeasure(userMeasureCreator, project, metric, m -> m.setValue(0d));

    String response = ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_KEY, project.getKey())
      .setParam(WebService.Param.FIELDS, "value, description")
      .execute()
      .getInput();

    assertThat(response).contains("id", "value", "description")
      .doesNotContain("createdAt")
      .doesNotContain("updatedAt")
      .doesNotContain("user")
      .doesNotContain("metric");
  }

  @Test
  public void search_with_more_recent_analysis() {
    long yesterday = DateUtils.addDays(new Date(), -1).getTime();
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);
    UserDto userMeasureCreator = db.users().insertUser();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    CustomMeasureDto customMeasure = db.measures().insertCustomMeasure(userMeasureCreator, project, metric, m -> m.setCreatedAt(yesterday).setUpdatedAt(yesterday));
    db.components().insertSnapshot(project);

    String response = ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, project.uuid())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"customMeasures\": [\n" +
      "    {\n" +
      "      \"id\": \"" + customMeasure.getId() + "\",\n" +
      "      \"value\": \"" + customMeasure.getTextValue() + "\",\n" +
      "      \"pending\": false\n" +
      "    },\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void empty_json_when_no_measure() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);

    String response = ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_KEY, project.getKey())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"customMeasures\": [],\n" +
      "  \"total\": 0,\n" +
      "  \"p\": 1,\n" +
      "  \"ps\": 100\n" +
      "}");
  }

  @Test
  public void fail_when_project_id_and_project_key_provided() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'projectId' or 'projectKey' must be provided");

    ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(SearchAction.PARAM_PROJECT_KEY, project.getKey())
      .execute();
  }

  @Test
  public void fail_when_project_id_nor_project_key_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'projectId' or 'projectKey' must be provided");

    ws.newRequest()
      .execute();
  }

  @Test
  public void fail_when_project_not_found_in_db() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'wrong-project-uuid' not found");

    ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, "wrong-project-uuid")
      .execute();
  }

  @Test
  public void fail_when_not_enough_privileges() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(USER, project);

    expectedException.expect(ForbiddenException.class);

    String response = ws.newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, project.uuid())
      .execute()
      .getInput();

    assertThat(response).contains("text-value-1");
  }

}
