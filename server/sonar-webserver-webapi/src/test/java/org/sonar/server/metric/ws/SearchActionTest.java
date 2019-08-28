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

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.metric.ws.SearchAction.PARAM_IS_CUSTOM;

public class SearchActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private SearchAction underTest = new SearchAction(dbClient);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void search_metrics_in_database() {
    insertNewCustomMetric("1", "2", "3");

    TestResponse result = tester.newRequest().execute();

    result.assertJson(getClass(), "search_metrics.json");
  }

  @Test
  public void search_metrics_ordered_by_name_case_insensitive() {
    insertNewCustomMetric("3", "1", "2");

    String firstResult = tester.newRequest().setParam(Param.PAGE, "1").setParam(Param.PAGE_SIZE, "1").execute().getInput();
    String secondResult = tester.newRequest().setParam(Param.PAGE, "2").setParam(Param.PAGE_SIZE, "1").execute().getInput();
    String thirdResult = tester.newRequest().setParam(Param.PAGE, "3").setParam(Param.PAGE_SIZE, "1").execute().getInput();

    assertThat(firstResult).contains("custom-key-1").doesNotContain("custom-key-2").doesNotContain("custom-key-3");
    assertThat(secondResult).contains("custom-key-2").doesNotContain("custom-key-1").doesNotContain("custom-key-3");
    assertThat(thirdResult).contains("custom-key-3").doesNotContain("custom-key-1").doesNotContain("custom-key-2");
  }

  @Test
  public void search_metrics_with_pagination() {
    insertNewCustomMetric("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

    TestResponse result = tester.newRequest()
      .setParam(Param.PAGE, "3")
      .setParam(Param.PAGE_SIZE, "4")
      .execute();

    assertThat(StringUtils.countMatches(result.getInput(), "custom-key")).isEqualTo(2);
  }

  @Test
  public void list_metric_with_is_custom_true() {
    insertNewCustomMetric("1", "2");
    insertNewNonCustomMetric("3");

    String result = tester.newRequest()
      .setParam(PARAM_IS_CUSTOM, "true").execute().getInput();

    assertThat(result).contains("custom-key-1", "custom-key-2")
      .doesNotContain("non-custom-key-3");
  }

  @Test
  public void list_metric_with_is_custom_false() {
    insertNewCustomMetric("1", "2");
    insertNewNonCustomMetric("3");

    String result = tester.newRequest()
      .setParam(PARAM_IS_CUSTOM, "false").execute().getInput();

    assertThat(result).doesNotContain("custom-key-1")
      .doesNotContain("custom-key-2")
      .contains("non-custom-key-3");
  }

  @Test
  public void list_metric_with_chosen_fields() {
    insertNewCustomMetric("1");

    String result = tester.newRequest().setParam(Param.FIELDS, "name").execute().getInput();

    assertThat(result).contains("id", "key", "name", "type")
      .doesNotContain("domain")
      .doesNotContain("description");
  }

  private void insertNewNonCustomMetric(String... ids) {
    for (String id : ids) {
      dbClient.metricDao().insert(dbSession, newMetricDto()
        .setKey("non-custom-key-" + id)
        .setEnabled(true)
        .setUserManaged(false));
    }
    dbSession.commit();
  }

  private void insertNewCustomMetric(String... ids) {
    for (String id : ids) {
      dbClient.metricDao().insert(dbSession, newCustomMetric(id));
    }
    dbSession.commit();
  }

  private MetricDto newCustomMetric(String id) {
    return newMetricDto()
      .setKey("custom-key-" + id)
      .setShortName("custom-name-" + id)
      .setDomain("custom-domain-" + id)
      .setDescription("custom-description-" + id)
      .setValueType("INT")
      .setUserManaged(true)
      .setDirection(0)
      .setHidden(false)
      .setQualitative(true)
      .setEnabled(true);
  }

}
