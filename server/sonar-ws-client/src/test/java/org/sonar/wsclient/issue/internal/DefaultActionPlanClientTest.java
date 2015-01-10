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

package org.sonar.wsclient.issue.internal;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.ActionPlanClient;
import org.sonar.wsclient.issue.NewActionPlan;
import org.sonar.wsclient.issue.UpdateActionPlan;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.fail;

public class DefaultActionPlanClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void should_find_action_plans() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"actionPlans\": [{\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\",\n" +
      "\"name\": \"Long term\",\n" +
      "\"desc\": \"Long term acton plan\",\n" +
      "\"status\": \"CLOSED\",\n" +
      "\"project\": \"com.sonarsource.it.samples:simple-sample\",\n" +
      "\"userLogin\": \"admin\",\n" +
      "\"deadLine\": \"2013-05-30T00:00:00+0200\",\n" +
      "\"totalIssues\": 3,\n" +
      "\"unresolvedIssues\": 2,\n" +
      "\"createdAt\": \"2013-05-13T12:50:29+0200\",\n" +
      "\"updatedAt\": \"2013-05-13T12:50:44+0200\"}]}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    List<ActionPlan> actionPlans = client.find("com.sonarsource.it.samples:simple-sample");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/search?project=com.sonarsource.it.samples:simple-sample");
    assertThat(actionPlans).hasSize(1);
    ActionPlan actionPlan = actionPlans.get(0);
    assertThat(actionPlan.key()).isEqualTo("382f6f2e-ad9d-424a-b973-9b065e04348a");
    assertThat(actionPlan.name()).isEqualTo("Long term");
    assertThat(actionPlan.description()).isEqualTo("Long term acton plan");
    assertThat(actionPlan.project()).isEqualTo("com.sonarsource.it.samples:simple-sample");
    assertThat(actionPlan.status()).isEqualTo("CLOSED");
    assertThat(actionPlan.userLogin()).isEqualTo("admin");
    assertThat(actionPlan.deadLine()).isNotNull();
    assertThat(actionPlan.totalIssues()).isEqualTo(3);
    assertThat(actionPlan.unresolvedIssues()).isEqualTo(2);
    assertThat(actionPlan.createdAt()).isNotNull();
    assertThat(actionPlan.updatedAt()).isNotNull();
  }

  @Test
  public void should_create_action_plan() throws Exception {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"actionPlan\": {\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\"}}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    ActionPlan result = client.create(
      NewActionPlan.create().name("Short term").project("org.sonar.Sample").description("Short term issues").deadLine(stringToDate("2014-01-01")));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/create");
    assertThat(httpServer.requestParams()).contains(
      entry("project", "org.sonar.Sample"),
      entry("description", "Short term issues"),
      entry("name", "Short term"),
      entry("deadLine", "2014-01-01")
      );
    assertThat(result).isNotNull();
  }

  @Test
  public void should_update_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"actionPlan\": {\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\"}}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    ActionPlan result = client.update(
      UpdateActionPlan.create().key("382f6f2e-ad9d-424a-b973-9b065e04348a").name("Short term").description("Short term issues").deadLine(stringToDate("2014-01-01")));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/update");
    assertThat(httpServer.requestParams()).contains(
      entry("key", "382f6f2e-ad9d-424a-b973-9b065e04348a"),
      entry("description", "Short term issues"),
      entry("name", "Short term"),
      entry("deadLine", "2014-01-01")
      );
    assertThat(result).isNotNull();
  }

  @Test
  public void should_delete_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    client.delete("382f6f2e-ad9d-424a-b973-9b065e04348a");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/delete");
    assertThat(httpServer.requestParams()).contains(
      entry("key", "382f6f2e-ad9d-424a-b973-9b065e04348a")
      );
  }

  @Test
  public void should_fail_to_delete_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubStatusCode(500);

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    try {
      client.delete("382f6f2e-ad9d-424a-b973-9b065e04348a");
      fail();
    } catch (HttpException e) {
      assertThat(e.status()).isEqualTo(500);
      assertThat(e.url()).startsWith("http://localhost");
      assertThat(e.url()).endsWith("/api/action_plans/delete");
      assertThat(httpServer.requestParams()).contains(
        entry("key", "382f6f2e-ad9d-424a-b973-9b065e04348a")
        );
    }
  }

  @Test
  public void should_open_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"actionPlan\": {\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\"}}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    ActionPlan result = client.open("382f6f2e-ad9d-424a-b973-9b065e04348a");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/open");
    assertThat(httpServer.requestParams()).containsEntry("key", "382f6f2e-ad9d-424a-b973-9b065e04348a");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_close_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"actionPlan\": {\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\"}}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    ActionPlan result = client.close("382f6f2e-ad9d-424a-b973-9b065e04348a");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/close");
    assertThat(httpServer.requestParams()).containsEntry("key", "382f6f2e-ad9d-424a-b973-9b065e04348a");
    assertThat(result).isNotNull();
  }

  private static Date stringToDate(String sDate) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-dd-MM");
      return sdf.parse(sDate);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

}
