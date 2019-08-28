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
package org.sonar.server.metric.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.measure.custom.CustomMeasureTesting.newCustomMeasureDto;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_DESCRIPTION;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_DOMAIN;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_ID;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_KEY;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_NAME;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_TYPE;

public class UpdateActionTest {

  private static final String DEFAULT_KEY = "custom-metric-key";
  private static final String DEFAULT_NAME = "custom-metric-name";
  private static final String DEFAULT_DOMAIN = "custom-metric-domain";
  private static final String DEFAULT_DESCRIPTION = "custom-metric-description";
  private static final String DEFAULT_TYPE = ValueType.INT.name();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private UpdateAction underTest = new UpdateAction(dbClient, userSessionRule);
  private WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    userSessionRule.logIn().setSystemAdministrator();
  }

  @Test
  public void update_all_fields() {
    int id = insertMetric(newDefaultMetric());

    tester.newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_KEY, "another-key")
      .setParam(PARAM_NAME, "another-name")
      .setParam(PARAM_TYPE, ValueType.BOOL.name())
      .setParam(PARAM_DOMAIN, "another-domain")
      .setParam(PARAM_DESCRIPTION, "another-description")
      .execute();
    dbSession.commit();

    MetricDto result = dbClient.metricDao().selectById(dbSession, id);
    assertThat(result.getKey()).isEqualTo("another-key");
    assertThat(result.getShortName()).isEqualTo("another-name");
    assertThat(result.getValueType()).isEqualTo(ValueType.BOOL.name());
    assertThat(result.getDomain()).isEqualTo("another-domain");
    assertThat(result.getDescription()).isEqualTo("another-description");
  }

  @Test
  public void update_one_field() {
    int id = insertMetric(newDefaultMetric());
    dbClient.customMeasureDao().insert(dbSession, newCustomMeasureDto().setMetricId(id));
    dbSession.commit();

    tester.newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_DESCRIPTION, "another-description")
      .execute();
    dbSession.commit();

    MetricDto result = dbClient.metricDao().selectById(dbSession, id);
    assertThat(result.getKey()).isEqualTo(DEFAULT_KEY);
    assertThat(result.getShortName()).isEqualTo(DEFAULT_NAME);
    assertThat(result.getValueType()).isEqualTo(DEFAULT_TYPE);
    assertThat(result.getDomain()).isEqualTo(DEFAULT_DOMAIN);
    assertThat(result.getDescription()).isEqualTo("another-description");
  }

  @Test
  public void update_return_the_full_object_with_id() {
    int id = insertMetric(newDefaultMetric().setDescription("another-description"));

    TestResponse requestResult = tester.newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_DESCRIPTION, DEFAULT_DESCRIPTION)
      .execute();
    dbSession.commit();

    requestResult.assertJson(getClass(), "metric.json");
    assertThat(requestResult.getInput()).matches(".*\"id\"\\s*:\\s*\"" + id + "\".*");
  }

  @Test
  public void fail_when_changing_key_for_an_existing_one() {
    expectedException.expect(ServerException.class);
    expectedException.expectMessage("The key 'metric-key' is already used by an existing metric.");
    insertMetric(newDefaultMetric().setKey("metric-key"));
    int id = insertMetric(newDefaultMetric().setKey("another-key"));

    tester.newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_KEY, "metric-key")
      .execute();
  }

  @Test
  public void fail_when_metric_not_in_db() {
    expectedException.expect(ServerException.class);

    tester.newRequest().setParam(PARAM_ID, "42").execute();
  }

  @Test
  public void fail_when_metric_is_deactivated() {
    expectedException.expect(ServerException.class);
    int id = insertMetric(newDefaultMetric().setEnabled(false));

    tester.newRequest().setParam(PARAM_ID, String.valueOf(id)).execute();
  }

  @Test
  public void fail_when_metric_is_not_custom() {
    expectedException.expect(ServerException.class);
    int id = insertMetric(newDefaultMetric().setUserManaged(false));

    tester.newRequest().setParam(PARAM_ID, String.valueOf(id)).execute();
  }

  @Test
  public void fail_when_custom_measures_and_type_changed() {
    expectedException.expect(ServerException.class);
    int id = insertMetric(newDefaultMetric());
    dbClient.customMeasureDao().insert(dbSession, newCustomMeasureDto().setMetricId(id));
    dbSession.commit();

    tester.newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_TYPE, ValueType.BOOL.name())
      .execute();
  }

  @Test
  public void fail_when_no_id() {
    expectedException.expect(IllegalArgumentException.class);

    tester.newRequest().execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSessionRule.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest().execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSessionRule.anonymous();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    tester.newRequest().execute();
  }

  @Test
  public void fail_when_metric_key_is_not_well_formatted() {
    int id = insertMetric(newDefaultMetric());
    dbClient.customMeasureDao().insert(dbSession, newCustomMeasureDto().setMetricId(id));
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Malformed metric key 'not well formatted key'. Allowed characters are alphanumeric, '-', '_', with at least one non-digit.");

    tester.newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_KEY, "not well formatted key")
      .execute();
  }

  private MetricDto newDefaultMetric() {
    return new MetricDto()
      .setKey(DEFAULT_KEY)
      .setShortName(DEFAULT_NAME)
      .setValueType(DEFAULT_TYPE)
      .setDomain(DEFAULT_DOMAIN)
      .setDescription(DEFAULT_DESCRIPTION)
      .setUserManaged(true)
      .setEnabled(true);
  }

  private int insertMetric(MetricDto metricDto) {
    dbClient.metricDao().insert(dbSession, metricDto);
    dbSession.commit();
    return metricDto.getId();
  }

}
