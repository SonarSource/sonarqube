/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SearchActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final SearchAction underTest = new SearchAction(dbClient);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void verify_definition() {
    Action wsDef = ws.getDef();

    assertThat(wsDef.deprecatedSince()).isNull();
    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isFalse();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription)
      .containsExactlyInAnyOrder(
        tuple("8.4", "Field 'id' in the response is deprecated"));
  }

  @Test
  public void search_metrics_in_database() {
    db.measures().insertMetric(metricDto -> metricDto
      .setKey("custom-key-1")
      .setShortName("custom-name-1")
      .setValueType("INT")
      .setDomain("custom-domain-1")
      .setDescription("custom-description-1")
      .setDirection(0)
      .setQualitative(true)
      .setHidden(false)
      .setEnabled(true));
    db.measures().insertMetric(metricDto -> metricDto
      .setKey("custom-key-2")
      .setShortName("custom-name-2")
      .setValueType("INT")
      .setDomain("custom-domain-2")
      .setDescription("custom-description-2")
      .setDirection(0)
      .setQualitative(true)
      .setHidden(false)
      .setEnabled(true));
    db.measures().insertMetric(metricDto -> metricDto
      .setKey("custom-key-3")
      .setShortName("custom-name-3")
      .setValueType("INT")
      .setDomain("custom-domain-3")
      .setDescription("custom-description-3")
      .setDirection(0)
      .setQualitative(true)
      .setHidden(false)
      .setEnabled(true));

    TestResponse result = ws.newRequest().execute();

    result.assertJson(getClass(), "search_metrics.json");
  }

  @Test
  public void search_metrics_ordered_by_name_case_insensitive() {
    insertMetrics("uuid-3", "uuid-1", "uuid-2");

    String firstResult = ws.newRequest().setParam(Param.PAGE, "1").setParam(Param.PAGE_SIZE, "1").execute().getInput();
    String secondResult = ws.newRequest().setParam(Param.PAGE, "2").setParam(Param.PAGE_SIZE, "1").execute().getInput();
    String thirdResult = ws.newRequest().setParam(Param.PAGE, "3").setParam(Param.PAGE_SIZE, "1").execute().getInput();

    assertThat(firstResult).contains("uuid-1").doesNotContain("uuid-2").doesNotContain("uuid-3");
    assertThat(secondResult).contains("uuid-2").doesNotContain("uuid-1").doesNotContain("uuid-3");
    assertThat(thirdResult).contains("uuid-3").doesNotContain("uuid-1").doesNotContain("uuid-2");
  }

  @Test
  public void search_metrics_with_pagination() {
    insertMetrics("uuid-1", "uuid-2", "uuid-3", "uuid-4", "uuid-5", "uuid-6", "uuid-7", "uuid-8", "uuid-9", "uuid-10");

    TestResponse result = ws.newRequest()
      .setParam(Param.PAGE, "3")
      .setParam(Param.PAGE_SIZE, "4")
      .execute();

    assertThat(StringUtils.countMatches(result.getInput(), "name-uuid-")).isEqualTo(2);
  }

  private void insertMetrics(String... ids) {
    for (String id : ids) {
      db.measures().insertMetric(metricDto -> metricDto.setUuid(id).setShortName("name-" + id).setEnabled(true));
    }
    dbSession.commit();
  }

}
