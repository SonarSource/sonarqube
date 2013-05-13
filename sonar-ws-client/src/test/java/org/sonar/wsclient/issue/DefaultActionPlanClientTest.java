/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.wsclient.issue;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class DefaultActionPlanClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void should_find_action_plans() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"actionPlans\": [{\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\",\n" +
                              "\"name\": \"Long term\",\n" +
                              "\"status\": \"CLOSED\",\n" +
                              "\"project\": \"com.sonarsource.it.samples:simple-sample\",\n" +
                              "\"userLogin\": \"admin\",\n" +
                              "\"deadLine\": \"2013-05-30T00:00:00+0200\",\n" +
                              "\"totalIssues\": 0,\n" +
                              "\"openIssues\": 0,\n" +
                              "\"createdAt\": \"2013-05-13T12:50:29+0200\",\n" +
                              "\"updatedAt\": \"2013-05-13T12:50:44+0200\"}]}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    List<ActionPlan> actionPlans = client.find("com.sonarsource.it.samples:simple-sample");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/search?project=com.sonarsource.it.samples:simple-sample");
    assertThat(actionPlans).hasSize(1);
    ActionPlan actionPlan = actionPlans.get(0);
    assertThat(actionPlan.key()).isEqualTo("382f6f2e-ad9d-424a-b973-9b065e04348a");
    assertThat(actionPlan.name()).isEqualTo("Long term");
    assertThat(actionPlan.project()).isEqualTo("com.sonarsource.it.samples:simple-sample");
    assertThat(actionPlan.status()).isEqualTo("CLOSED");
    assertThat(actionPlan.userLogin()).isEqualTo("admin");
    assertThat(actionPlan.deadLine()).isNotNull();
    assertThat(actionPlan.totalIssues()).isEqualTo(0);
    assertThat(actionPlan.openIssues()).isEqualTo(0);
    assertThat(actionPlan.createdAt()).isNotNull();
    assertThat(actionPlan.updatedAt()).isNotNull();
  }

  @Test
  public void should_find_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"actionPlan\": {\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\",\n" +
        "\"name\": \"Long term\",\n" +
        "\"status\": \"CLOSED\",\n" +
        "\"project\": \"com.sonarsource.it.samples:simple-sample\",\n" +
        "\"userLogin\": \"admin\",\n" +
        "\"deadLine\": \"2013-05-30T00:00:00+0200\",\n" +
        "\"totalIssues\": 0,\n" +
        "\"openIssues\": 0,\n" +
        "\"createdAt\": \"2013-05-13T12:50:29+0200\",\n" +
        "\"updatedAt\": \"2013-05-13T12:50:44+0200\"}}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    ActionPlan actionPlan = client.get("382f6f2e-ad9d-424a-b973-9b065e04348a");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/show?key=382f6f2e-ad9d-424a-b973-9b065e04348a");
    assertThat(actionPlan).isNotNull();
    assertThat(actionPlan.key()).isEqualTo("382f6f2e-ad9d-424a-b973-9b065e04348a");
    assertThat(actionPlan.name()).isEqualTo("Long term");
    assertThat(actionPlan.project()).isEqualTo("com.sonarsource.it.samples:simple-sample");
    assertThat(actionPlan.status()).isEqualTo("CLOSED");
    assertThat(actionPlan.userLogin()).isEqualTo("admin");
    assertThat(actionPlan.deadLine()).isNotNull();
    assertThat(actionPlan.totalIssues()).isEqualTo(0);
    assertThat(actionPlan.openIssues()).isEqualTo(0);
    assertThat(actionPlan.createdAt()).isNotNull();
    assertThat(actionPlan.updatedAt()).isNotNull();
  }

  @Test
  public void should_create_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"actionPlan\": {\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\"}}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    ActionPlan result = client.create(NewActionPlan.create().name("Short term").project("org.sonar.Sample").description("Short term issues").deadLine("01/01/2014"));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/create?project=org.sonar.Sample&description=Short%20term%20issues&name=Short%20term&deadLine=01/01/2014");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_update_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"actionPlan\": {\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\"}}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    ActionPlan result = client.update(UpdateActionPlan.create().key("382f6f2e-ad9d-424a-b973-9b065e04348a").name("Short term").description("Short term issues").deadLine("01/01/2014"));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/update?description=Short%20term%20issues&name=Short%20term&deadLine=01/01/2014&key=382f6f2e-ad9d-424a-b973-9b065e04348a");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_delete_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    client.delete("382f6f2e-ad9d-424a-b973-9b065e04348a");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/delete?key=382f6f2e-ad9d-424a-b973-9b065e04348a");
  }

  @Test
  public void should_fail_to_delete_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnStatus(500);

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    try {
      client.delete("382f6f2e-ad9d-424a-b973-9b065e04348a");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Fail to delete action plan. Bad HTTP response status: 500");
    }
  }

  @Test
  public void should_open_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"actionPlan\": {\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\"}}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    ActionPlan result = client.open("382f6f2e-ad9d-424a-b973-9b065e04348a");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/open?key=382f6f2e-ad9d-424a-b973-9b065e04348a");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_close_action_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"actionPlan\": {\"key\": \"382f6f2e-ad9d-424a-b973-9b065e04348a\"}}");

    ActionPlanClient client = new DefaultActionPlanClient(requestFactory);
    ActionPlan result = client.close("382f6f2e-ad9d-424a-b973-9b065e04348a");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/action_plans/close?key=382f6f2e-ad9d-424a-b973-9b065e04348a");
    assertThat(result).isNotNull();
  }

}
