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
package org.sonar.server.measure.custom.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.ws.UserJsonWriter;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.measure.custom.CustomMeasureTesting.newCustomMeasureDto;
import static org.sonar.server.measure.custom.ws.UpdateAction.PARAM_DESCRIPTION;
import static org.sonar.server.measure.custom.ws.UpdateAction.PARAM_ID;
import static org.sonar.server.measure.custom.ws.UpdateAction.PARAM_VALUE;
import static org.sonar.server.util.TypeValidationsTesting.newFullTypeValidations;

public class UpdateActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(new MapSettings().asConfig()));

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private System2 system = mock(System2.class);
  private WsTester ws;

  @Before
  public void setUp() {
    CustomMeasureValidator validator = new CustomMeasureValidator(newFullTypeValidations());

    ws = new WsTester(new CustomMeasuresWs(new UpdateAction(dbClient, userSessionRule, system, validator, new CustomMeasureJsonWriter(new UserJsonWriter(userSessionRule)))));

    db.getDbClient().userDao().insert(dbSession, new UserDto()
      .setLogin("login")
      .setName("Login")
      .setEmail("login@login.com")
      .setActive(true)
      );
    dbSession.commit();
  }

  @Test
  public void update_text_value_and_description_in_db() throws Exception {
    MetricDto metric = insertNewMetric(ValueType.STRING);
    ComponentDto component = db.components().insertPrivateProject(db.getDefaultOrganization(), "project-uuid");
    CustomMeasureDto customMeasure = newCustomMeasure(component, metric)
      .setDescription("custom-measure-description")
      .setTextValue("text-measure-value");
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();
    when(system.now()).thenReturn(123_456_789L);
    logInAsProjectAdministrator(component);

    ws.newPostRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "new-text-measure-value")
      .execute();
    logInAsProjectAdministrator(component);

    CustomMeasureDto updatedCustomMeasure = dbClient.customMeasureDao().selectOrFail(dbSession, customMeasure.getId());
    assertThat(updatedCustomMeasure.getTextValue()).isEqualTo("new-text-measure-value");
    assertThat(updatedCustomMeasure.getDescription()).isEqualTo("new-custom-measure-description");
    assertThat(updatedCustomMeasure.getUpdatedAt()).isEqualTo(123_456_789L);
    assertThat(customMeasure.getCreatedAt()).isEqualTo(updatedCustomMeasure.getCreatedAt());
  }

  @Test
  public void update_double_value_and_description_in_db() throws Exception {
    MetricDto metric = insertNewMetric(ValueType.INT);
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto component = db.components().insertPrivateProject(organizationDto, "project-uuid");
    CustomMeasureDto customMeasure = newCustomMeasure(component, metric)
      .setDescription("custom-measure-description")
      .setValue(42d);
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();
    logInAsProjectAdministrator(component);

    ws.newPostRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();

    CustomMeasureDto updatedCustomMeasure = dbClient.customMeasureDao().selectOrFail(dbSession, customMeasure.getId());
    assertThat(updatedCustomMeasure.getValue()).isCloseTo(1984d, offset(0.01d));
    assertThat(updatedCustomMeasure.getDescription()).isEqualTo("new-custom-measure-description");
    assertThat(customMeasure.getCreatedAt()).isEqualTo(updatedCustomMeasure.getCreatedAt());
  }

  @Test
  public void returns_full_object_in_response() throws Exception {
    MetricDto metric = MetricTesting.newMetricDto().setEnabled(true)
      .setValueType(ValueType.STRING.name())
      .setKey("metric-key");
    dbClient.metricDao().insert(dbSession, metric);
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto component = ComponentTesting.newPrivateProjectDto(organizationDto, "project-uuid").setDbKey("project-key");
    dbClient.componentDao().insert(dbSession, component);
    CustomMeasureDto customMeasure = newCustomMeasure(component, metric)
      .setCreatedAt(100_000_000L)
      .setDescription("custom-measure-description")
      .setTextValue("text-measure-value");
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();
    when(system.now()).thenReturn(123_456_789L);
    logInAsProjectAdministrator(component);

    WsTester.Result response = ws.newPostRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "new-text-measure-value")
      .execute();

    response.assertJson(getClass(), "custom-measure.json");
    String responseAsString = response.outputAsString();
    assertThat(responseAsString).matches(String.format(".*\"id\"\\s*:\\s*\"%s\".*", customMeasure.getId()));
    assertThat(responseAsString).matches(String.format(".*\"id\"\\s*:\\s*\"%s\".*", metric.getId()));
    assertThat(responseAsString).matches(".*createdAt.*updatedAt.*");
  }

  @Test
  public void update_value_only() throws Exception {
    MetricDto metric = insertNewMetric(ValueType.STRING);
    ComponentDto component = db.components().insertPrivateProject(db.getDefaultOrganization(), "project-uuid");
    CustomMeasureDto customMeasure = newCustomMeasure(component, metric)
      .setDescription("custom-measure-description")
      .setTextValue("text-measure-value");
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();
    when(system.now()).thenReturn(123_456_789L);
    logInAsProjectAdministrator(component);

    ws.newPostRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .execute();
    logInAsProjectAdministrator(component);

    CustomMeasureDto updatedCustomMeasure = dbClient.customMeasureDao().selectOrFail(dbSession, customMeasure.getId());
    assertThat(updatedCustomMeasure.getTextValue()).isEqualTo("text-measure-value");
    assertThat(updatedCustomMeasure.getDescription()).isEqualTo("new-custom-measure-description");
    assertThat(updatedCustomMeasure.getUpdatedAt()).isEqualTo(123_456_789L);
    assertThat(customMeasure.getCreatedAt()).isEqualTo(updatedCustomMeasure.getCreatedAt());
  }

  @Test
  public void update_description_only() throws Exception {
    MetricDto metric = insertNewMetric(ValueType.STRING);
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto component = db.components().insertPrivateProject(organizationDto, "project-uuid");
    CustomMeasureDto customMeasure = newCustomMeasure(component, metric)
      .setMetricId(metric.getId())
      .setComponentUuid(component.uuid())
      .setCreatedAt(system.now())
      .setDescription("custom-measure-description")
      .setTextValue("text-measure-value");
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();
    when(system.now()).thenReturn(123_456_789L);
    logInAsProjectAdministrator(component);

    ws.newPostRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_VALUE, "new-text-measure-value")
      .execute();

    CustomMeasureDto updatedCustomMeasure = dbClient.customMeasureDao().selectOrFail(dbSession, customMeasure.getId());
    assertThat(updatedCustomMeasure.getTextValue()).isEqualTo("new-text-measure-value");
    assertThat(updatedCustomMeasure.getDescription()).isEqualTo("custom-measure-description");
    assertThat(updatedCustomMeasure.getUpdatedAt()).isEqualTo(123_456_789L);
    assertThat(customMeasure.getCreatedAt()).isEqualTo(updatedCustomMeasure.getCreatedAt());
  }

  @Test
  public void fail_if_get_request() throws Exception {
    expectedException.expect(ServerException.class);

    ws.newGetRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_ID, "42")
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();
  }

  @Test
  public void fail_if_not_in_db() throws Exception {
    expectedException.expect(RowNotFoundException.class);
    expectedException.expectMessage("Custom measure '42' not found.");

    ws.newPostRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_ID, "42")
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();
  }

  @Test
  public void fail_if_insufficient_privileges() throws Exception {
    userSessionRule.logIn();
    MetricDto metric = MetricTesting.newMetricDto().setEnabled(true).setValueType(ValueType.STRING.name());
    dbClient.metricDao().insert(dbSession, metric);
    ComponentDto component = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    dbClient.componentDao().insert(dbSession, component);
    CustomMeasureDto customMeasure = newCustomMeasureDto()
      .setMetricId(metric.getId())
      .setComponentUuid(component.uuid())
      .setCreatedAt(system.now())
      .setDescription("custom-measure-description")
      .setTextValue("text-measure-value");
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();

    expectedException.expect(ForbiddenException.class);

    ws.newPostRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() throws Exception {
    userSessionRule.anonymous();
    expectedException.expect(UnauthorizedException.class);
    MetricDto metric = MetricTesting.newMetricDto().setEnabled(true).setValueType(ValueType.STRING.name());
    dbClient.metricDao().insert(dbSession, metric);
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto component = ComponentTesting.newPrivateProjectDto(organizationDto, "project-uuid");
    dbClient.componentDao().insert(dbSession, component);
    CustomMeasureDto customMeasure = newCustomMeasureDto()
      .setMetricId(metric.getId())
      .setComponentUuid(component.uuid())
      .setCreatedAt(system.now())
      .setDescription("custom-measure-description")
      .setTextValue("text-measure-value");
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();

    ws.newPostRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_ID, String.valueOf(customMeasure.getId()))
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();
  }

  @Test
  public void fail_if_custom_measure_id_is_missing_in_request() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    ws.newPostRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_DESCRIPTION, "new-custom-measure-description")
      .setParam(PARAM_VALUE, "1984")
      .execute();
  }

  @Test
  public void fail_if_custom_measure_value_and_description_are_missing_in_request() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    ws.newPostRequest(CustomMeasuresWs.ENDPOINT, UpdateAction.ACTION)
      .setParam(PARAM_ID, "42")
      .execute();
  }

  private MetricDto insertNewMetric(ValueType metricType) {
    MetricDto metric = MetricTesting.newMetricDto().setEnabled(true).setValueType(metricType.name());
    dbClient.metricDao().insert(dbSession, metric);

    return metric;
  }

  private CustomMeasureDto newCustomMeasure(ComponentDto project, MetricDto metric) {
    return newCustomMeasureDto()
      .setMetricId(metric.getId())
      .setComponentUuid(project.uuid())
      .setCreatedAt(system.now());
  }

  private void logInAsProjectAdministrator(ComponentDto component) {
    userSessionRule.logIn("login").addProjectPermission(UserRole.ADMIN, component);
  }
}
