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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ws.UserJsonWriter;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.measure.custom.ws.UpdateAction.PARAM_DESCRIPTION;
import static org.sonar.server.measure.custom.ws.UpdateAction.PARAM_ID;
import static org.sonar.server.measure.custom.ws.UpdateAction.PARAM_VALUE;
import static org.sonar.server.util.TypeValidationsTesting.newFullTypeValidations;
import static org.sonar.test.JsonAssert.assertJson;

public class UpdateActionTest {

  private static final long NOW = 10_000_000_000L;

  private System2 system = new TestSystem2().setNow(NOW);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private WsActionTester ws = new WsActionTester(new UpdateAction(db.getDbClient(), userSession, system, new CustomMeasureValidator(newFullTypeValidations()),
    new CustomMeasureJsonWriter(new UserJsonWriter(userSession))));

  @Test
  public void update_text_value_and_description_in_db() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    UserDto userMeasureCreator = db.users().insertUser();
    CustomMeasureDto customMeasure = db.measures().insertCustomMeasure(userMeasureCreator, project, metric, m -> m.setValue(0d));

    ws.newRequest()
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "new-text-measure-value")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getUserUuid, CustomMeasureDto::getComponentUuid,
        CustomMeasureDto::getCreatedAt, CustomMeasureDto::getUpdatedAt)
      .containsExactlyInAnyOrder(
        tuple("new-custom-measure-description", "new-text-measure-value", 0d, userAuthenticated.getUuid(), project.uuid(), customMeasure.getCreatedAt(), NOW));
  }

  @Test
  public void update_int_value_and_description_in_db() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(INT.name()));
    UserDto userMeasureCreator = db.users().insertUser();
    CustomMeasureDto customMeasure = db.measures().insertCustomMeasure(userMeasureCreator, project, metric, m -> m.setValue(42d).setTextValue(null));

    ws.newRequest()
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getUserUuid, CustomMeasureDto::getComponentUuid,
        CustomMeasureDto::getCreatedAt, CustomMeasureDto::getUpdatedAt)
      .containsExactlyInAnyOrder(
        tuple("new-custom-measure-description", null, 1984d, userAuthenticated.getUuid(), project.uuid(), customMeasure.getCreatedAt(), NOW));
  }

  @Test
  public void returns_full_object_in_response() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    UserDto userMeasureCreator = db.users().insertUser();
    CustomMeasureDto customMeasure = db.measures().insertCustomMeasure(userMeasureCreator, project, metric, m -> m.setValue(0d).setCreatedAt(100_000_000L));

    String response = ws.newRequest()
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "new-text-measure-value")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo("{\n" +
      "  \"id\": \"" + customMeasure.getId() + "\",\n" +
      "  \"value\": \"new-text-measure-value\",\n" +
      "  \"description\": \"new-custom-measure-description\",\n" +
      "  \"metric\": {\n" +
      "    \"id\": \"" + metric.getId() + "\",\n" +
      "    \"key\": \"" + metric.getKey() + "\",\n" +
      "    \"type\": \"" + metric.getValueType() + "\",\n" +
      "    \"name\": \"" + metric.getShortName() + "\",\n" +
      "    \"domain\": \"" + metric.getDomain() + "\"\n" +
      "  },\n" +
      "  \"projectId\": \"" + project.uuid() + "\",\n" +
      "  \"projectKey\": \"" + project.getKey() + "\",\n" +
      "}");
  }

  @Test
  public void update_description_only() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    CustomMeasureDto customMeasure = db.measures().insertCustomMeasure(user, project, metric);

    ws.newRequest()
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue)
      .containsExactlyInAnyOrder(
        tuple("new-custom-measure-description", customMeasure.getTextValue(), customMeasure.getValue()));
  }

  @Test
  public void update_value_only() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    CustomMeasureDto customMeasure = db.measures().insertCustomMeasure(user, project, metric);

    ws.newRequest()
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_VALUE, "new-text-measure-value")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue)
      .containsExactlyInAnyOrder(
        tuple(customMeasure.getDescription(), "new-text-measure-value", customMeasure.getValue()));
  }

  @Test
  public void fail_if_measure_is_not_in_db() {
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    UserDto user = db.users().insertUser();
    db.measures().insertCustomMeasure(user, project, metric);
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Custom measure with id '0' does not exist");

    ws.newRequest()
      .setParam(PARAM_ID, "0")
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();
  }

  @Test
  public void fail_if_insufficient_privileges() {
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    UserDto user = db.users().insertUser();
    CustomMeasureDto customMeasure = db.measures().insertCustomMeasure(user, project, metric);
    userSession.logIn(user).addProjectPermission(USER, project);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    CustomMeasureDto customMeasure = db.measures().insertCustomMeasure(user, project, metric);
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    ws.newRequest()
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();
  }

  @Test
  public void fail_if_custom_measure_id_is_missing_in_request() {
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    UserDto user = db.users().insertUser();
    db.measures().insertCustomMeasure(user, project, metric);
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'id' parameter is missing");

    ws.newRequest()
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();
  }

  @Test
  public void fail_if_custom_measure_value_and_description_are_missing_in_request() {
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    UserDto user = db.users().insertUser();
    db.measures().insertCustomMeasure(user, project, metric);
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value or description must be provided.");

    ws.newRequest()
      .setParam(PARAM_ID, "42")
      .execute();
  }

}
