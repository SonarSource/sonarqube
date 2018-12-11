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
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ws.UserJsonWriter;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.server.util.TypeValidationsTesting.newFullTypeValidations;
import static org.sonar.test.JsonAssert.assertJson;

public class CreateActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();

  private WsActionTester ws = new WsActionTester(
    new CreateAction(db.getDbClient(), userSession, System2.INSTANCE, new CustomMeasureValidator(newFullTypeValidations()),
      new CustomMeasureJsonWriter(new UserJsonWriter(userSession)), TestComponentFinder.from(db)));

  @Test
  public void create_boolean_custom_measure_in_db() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(BOOL.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, Integer.toString(metric.getId()))
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "true")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple("custom-measure-description", null, 1d, project.uuid()));
  }

  @Test
  public void create_int_custom_measure_in_db() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(INT.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "42")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple(null, null, 42d, project.uuid()));
  }

  @Test
  public void create_text_custom_measure_in_db() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "custom-measure-free-text")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple(null, "custom-measure-free-text", 0d, project.uuid()));
  }

  @Test
  public void create_text_custom_measure_with_metric_key() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_KEY, metric.getKey())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple(null, "whatever-value", 0d, project.uuid()));
  }

  @Test
  public void create_text_custom_measure_with_project_key() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_KEY, project.getKey())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple(null, "whatever-value", 0d, project.uuid()));
  }

  @Test
  public void create_float_custom_measure_in_db() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(FLOAT.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "4.2")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple(null, null, 4.2d, project.uuid()));
  }

  @Test
  public void create_work_duration_custom_measure_in_db() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(WORK_DUR.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "253")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple(null, null, 253d, project.uuid()));
  }

  @Test
  public void create_level_type_custom_measure_in_db() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(LEVEL.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, Metric.Level.ERROR.name())
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple(null, Metric.Level.ERROR.name(), 0d, project.uuid()));
  }

  @Test
  public void response_with_object_and_id() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    String response = ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "custom-measure-free-text")
      .execute()
      .getInput();

    CustomMeasureDto customMeasure = db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()).get(0);
    assertJson(response).isSimilarTo("{\n" +
      "  \"id\": \"" + customMeasure.getId() + "\",\n" +
      "  \"value\": \"custom-measure-free-text\",\n" +
      "  \"description\": \"custom-measure-description\",\n" +
      "  \"metric\": {\n" +
      "    \"id\": \"" + metric.getId() + "\",\n" +
      "    \"key\": \"" + metric.getKey() + "\",\n" +
      "    \"type\": \"" + metric.getValueType() + "\",\n" +
      "    \"name\": \"" + metric.getShortName() + "\",\n" +
      "    \"domain\": \"" + metric.getDomain() + "\"\n" +
      "  },\n" +
      "  \"projectId\": \"" + project.uuid() + "\",\n" +
      "  \"projectKey\": \"" + project.getKey() + "\",\n" +
      "  \"pending\": true\n" +
      "}");
  }

  @Test
  public void create_custom_measure_on_module() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(BOOL.name()));
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, module.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, Integer.toString(metric.getId()))
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "true")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple("custom-measure-description", null, 1d, module.uuid()));
  }

  @Test
  public void create_custom_measure_on_a_view() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(BOOL.name()));
    OrganizationDto organization = db.organizations().insert();
    ComponentDto view = db.components().insertPrivatePortfolio(organization);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, view);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, view.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "true")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple("custom-measure-description", null, 1d, view.uuid()));
  }

  @Test
  public void create_custom_measure_on_a_sub_view() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(BOOL.name()));
    OrganizationDto organization = db.organizations().insert();
    ComponentDto view = db.components().insertPrivatePortfolio(organization);
    ComponentDto subView = db.components().insertSubView(view);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, view);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, subView.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "true")
      .execute();

    assertThat(db.getDbClient().customMeasureDao().selectByMetricId(db.getSession(), metric.getId()))
      .extracting(CustomMeasureDto::getDescription, CustomMeasureDto::getTextValue, CustomMeasureDto::getValue, CustomMeasureDto::getComponentUuid)
      .containsExactlyInAnyOrder(tuple("custom-measure-description", null, 1d, subView.uuid()));
  }

  @Test
  public void fail_when_project_id_nor_project_key_provided() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'projectId' or 'projectKey' must be provided");

    ws.newRequest()
      .setParam(CreateAction.PARAM_METRIC_ID, "whatever-id")
      .setParam(CreateAction.PARAM_VALUE, metric.getId().toString())
      .execute();
  }

  @Test
  public void fail_when_project_id_and_project_key_are_provided() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'projectId' or 'projectKey' must be provided");

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_PROJECT_KEY, project.getKey())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_project_key_does_not_exist_in_db() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'another-project-key' not found");

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_KEY, "another-project-key")
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_project_id_does_not_exist_in_db() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'another-project-uuid' not found");

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, "another-project-uuid")
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_metric_id_nor_metric_key_is_provided() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either the metric id or the metric key must be provided");

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_metric_id_and_metric_key_are_provided() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either the metric id or the metric key must be provided");

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_METRIC_KEY, metric.getKey())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_metric_key_is_not_found_in_db() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric with key 'unknown' does not exist");

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_KEY, "unknown")
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_metric_id_is_not_found_in_db() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric with id '42' does not exist");

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, "42")
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_measure_already_exists_on_same_project_and_same_metric() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(STRING.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto userMeasureCreator = db.users().insertUser();
    db.measures().insertCustomMeasure(userMeasureCreator, project, metric);
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addProjectPermission(ADMIN, project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("A measure already exists for project '%s' and metric '%s'", project.getKey(), metric.getKey()));

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_value_is_not_well_formatted() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(BOOL.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Value 'non-correct-boolean-value' must be one of \"true\" or \"false\"");

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "non-correct-boolean-value")
      .execute();
  }

  @Test
  public void fail_when_system_administrator() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(BOOL.name()));
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, project.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_not_a_project() {
    MetricDto metric = db.measures().insertMetric(m -> m.setUserManaged(true).setValueType(BOOL.name()));
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(project, "dir"));
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Component '%s' must be a project or a module.", directory.getKey()));

    ws.newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, directory.uuid())
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }
}
