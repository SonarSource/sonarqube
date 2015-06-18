/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.measure.custom.ws;

import java.util.Arrays;
import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.measure.custom.db.CustomMeasureDto;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.measure.custom.persistence.CustomMeasureDao;
import org.sonar.server.metric.persistence.MetricDao;
import org.sonar.server.metric.ws.MetricTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.BooleanTypeValidation;
import org.sonar.server.util.FloatTypeValidation;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.LongTypeValidation;
import org.sonar.server.util.MetricLevelTypeValidation;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@Category(DbTests.class)
public class CreateActionTest {

  private static final String DEFAULT_PROJECT_UUID = "project-uuid";
  private static final String DEFAULT_PROJECT_KEY = "project-key";
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @ClassRule
  public static DbTester db = new DbTester();
  DbClient dbClient;
  DbSession dbSession;

  WsTester ws;

  @Before
  public void setUp() {
    dbClient = new DbClient(db.database(), db.myBatis(), new CustomMeasureDao(), new MetricDao(), new ComponentDao());
    dbSession = dbClient.openSession(false);
    TypeValidations typeValidations = new TypeValidations(Arrays.asList(new BooleanTypeValidation(), new IntegerTypeValidation(), new FloatTypeValidation(),
      new MetricLevelTypeValidation(), new LongTypeValidation()));
    ws = new WsTester(new CustomMeasuresWs(new CreateAction(dbClient, userSession, System2.INSTANCE, typeValidations, new CustomMeasureJsonWriter())));
    db.truncateTables();
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void create_boolean_custom_measure_in_db() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.BOOL, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "true")
      .execute();

    List<CustomMeasureDto> customMeasures = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId());
    CustomMeasureDto customMeasure = customMeasures.get(0);
    assertThat(customMeasures).hasSize(1);
    assertThat(customMeasure.getDescription()).isEqualTo("custom-measure-description");
    assertThat(customMeasure.getTextValue()).isNullOrEmpty();
    assertThat(customMeasure.getValue()).isCloseTo(1.0d, offset(0.01d));
    assertThat(customMeasure.getComponentUuid()).isEqualTo(DEFAULT_PROJECT_UUID);
  }

  @Test
  public void create_int_custom_measure_in_db() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.INT, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "42")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure.getTextValue()).isNullOrEmpty();
    assertThat(customMeasure.getValue()).isCloseTo(42.0d, offset(0.01d));
  }

  @Test
  public void create_text_custom_measure_in_db() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "custom-measure-free-text")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure.getTextValue()).isEqualTo("custom-measure-free-text");
  }

  @Test
  public void create_text_custom_measure_as_project_admin() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);
    userSession.login("login").addProjectUuidPermissions(UserRole.ADMIN, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "custom-measure-free-text")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure).isNotNull();
  }

  @Test
  public void create_text_custom_measure_with_metric_key() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_KEY, metric.getKey())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure).isNotNull();
  }

  @Test
  public void create_text_custom_measure_with_project_key() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_KEY, DEFAULT_PROJECT_KEY)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure).isNotNull();
  }

  @Test
  public void create_float_custom_measure_indb() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.FLOAT, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "4.2")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure.getValue()).isCloseTo(4.2d, Offset.offset(0.01d));
    assertThat(customMeasure.getTextValue()).isNullOrEmpty();
  }

  @Test
  public void create_work_duration_custom_measure_in_db() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.WORK_DUR, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "253")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure.getTextValue()).isNullOrEmpty();
    assertThat(customMeasure.getValue()).isCloseTo(253, offset(0.01d));
  }

  @Test
  public void create_level_type_custom_measure_in_db() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.LEVEL, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, Metric.Level.WARN.name())
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure.getTextValue()).isEqualTo(Metric.Level.WARN.name());
  }

  @Test
  public void response_with_object_and_id() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    WsTester.Result response = newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "custom-measure-free-text")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    response.assertJson(getClass(), "custom-measure.json");
    assertThat(response.outputAsString()).matches(String.format(".*\"id\"\\s*:\\s*\"%d\".*", customMeasure.getId()));
    assertThat(response.outputAsString()).matches(String.format(".*\"id\"\\s*:\\s*\"%d\".*", metric.getId()));
  }

  @Test
  public void fail_when_get_request() throws Exception {
    expectedException.expect(ServerException.class);

    ws.newGetRequest(CustomMeasuresWs.ENDPOINT, CreateAction.ACTION)
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, "whatever-id")
      .setParam(CreateAction.PARAM_VALUE, "custom-measure-free-text")
      .execute();
  }

  @Test
  public void fail_when_project_id_nor_project_key_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The project key or the project id must be provided, not both.");
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_METRIC_ID, "whatever-id")
      .setParam(CreateAction.PARAM_VALUE, metric.getId().toString())
      .execute();
  }

  @Test
  public void fail_when_project_id_and_project_key_are_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The project key or the project id must be provided, not both.");
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_PROJECT_KEY, DEFAULT_PROJECT_KEY)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_project_key_does_not_exist_in_db() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project key 'another-project-key' not found");
    insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_KEY, "another-project-key")
      .setParam(CreateAction.PARAM_METRIC_ID, "whatever-id")
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_project_id_does_not_exist_in_db() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project id 'another-project-uuid' not found");
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, "another-project-uuid")
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_metric_id_nor_metric_key_is_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The metric id or the metric key must be provided, not both.");
    insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_metric_id_and_metric_key_are_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The metric id or the metric key must be provided, not both.");
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_METRIC_KEY, metric.getKey())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();

  }

  @Test
  public void fail_when_metric_is_not_found_in_db() throws Exception {
    dbClient.componentDao().insert(dbSession, ComponentTesting.newProjectDto(DEFAULT_PROJECT_UUID));
    dbSession.commit();

    expectedException.expect(ServerException.class);
    expectedException.expectMessage("Metric id '42' not found");

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, "42")
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_measure_already_exists_on_same_project_and_same_metric() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    expectedException.expect(ServerException.class);
    expectedException.expectMessage(String.format("A measure already exists for project id 'project-uuid' and metric id '%d'", metric.getId()));

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_value_is_not_well_formatted() throws Exception {
    MetricDto metric = insertMetricAndProject(ValueType.BOOL, DEFAULT_PROJECT_UUID);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Ill formatted value 'non-correct-boolean-value' for metric type 'Yes/No'");

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "non-correct-boolean-value")
      .execute();
  }

  @Test
  public void fail_when_not_enough_permission() throws Exception {
    expectedException.expect(ForbiddenException.class);
    userSession.login("login");
    MetricDto metric = insertMetricAndProject(ValueType.STRING, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest(CustomMeasuresWs.ENDPOINT, CreateAction.ACTION);
  }

  private MetricDto insertMetricAndProject(ValueType metricType, String projectUuid) {
    MetricDto metric = MetricTesting.newMetricDto().setEnabled(true).setValueType(metricType.name()).setKey("metric-key");
    dbClient.metricDao().insert(dbSession, metric);
    dbClient.componentDao().insert(dbSession, ComponentTesting.newProjectDto(projectUuid).setKey(DEFAULT_PROJECT_KEY));
    dbSession.commit();

    return metric;
  }
}
