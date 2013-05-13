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

import com.github.kevinsawicki.http.HttpRequest;
import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.EncodingUtils;
import org.sonar.wsclient.internal.HttpRequestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Do not instantiate this class, but use {@link org.sonar.wsclient.SonarClient#actionPlanClient()}.
 */
public class DefaultActionPlanClient implements ActionPlanClient {

  private final HttpRequestFactory requestFactory;

  /**
   * For internal use. Use {@link org.sonar.wsclient.SonarClient} to get an instance.
   */
  public DefaultActionPlanClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public List<ActionPlan> find(String projectKey) {
    HttpRequest request = requestFactory.get(ActionPlanQuery.BASE_URL, EncodingUtils.toMap("project", projectKey));
    if (!request.ok()) {
      throw new IllegalStateException("Fail to search for action plans. Bad HTTP response status: " + request.code());
    }
    List<ActionPlan> result = new ArrayList<ActionPlan>();
    String json = request.body("UTF-8");
    Map jsonRoot = (Map) JSONValue.parse(json);
    List<Map> jsonActionPlans = (List) jsonRoot.get("actionPlans");
    if (jsonActionPlans != null) {
      for (Map jsonActionPlan : jsonActionPlans) {
        result.add(new ActionPlan(jsonActionPlan));
      }
    }
    return result;
  }

  @Override
  public ActionPlan get(String actionPlanKey) {
    HttpRequest request = requestFactory.get("/api/action_plans/show", EncodingUtils.toMap("key", actionPlanKey));
    if (!request.ok()) {
      throw new IllegalStateException("Fail to search action plan. Bad HTTP response status: " + request.code());
    }
    return createActionPlanResult(request);
  }

  @Override
  public ActionPlan create(NewActionPlan newActionPlan) {
    HttpRequest request = requestFactory.post(NewActionPlan.BASE_URL, newActionPlan.urlParams());
    if (!request.ok()) {
      throw new IllegalStateException("Fail to create action plan. Bad HTTP response status: " + request.code());
    }
    return createActionPlanResult(request);
  }

  @Override
  public ActionPlan update(UpdateActionPlan updateActionPlan) {
    HttpRequest request = requestFactory.post(UpdateActionPlan.BASE_URL, updateActionPlan.urlParams());
    if (!request.ok()) {
      throw new IllegalStateException("Fail to update action plan. Bad HTTP response status: " + request.code());
    }
    return createActionPlanResult(request);
  }

  @Override
  public void delete(String actionPlanKey) {
    executeSimpleAction(actionPlanKey, "delete");
  }

  @Override
  public ActionPlan open(String actionPlanKey) {
    HttpRequest request = executeSimpleAction(actionPlanKey, "open");
    return createActionPlanResult(request);
  }

  @Override
  public ActionPlan close(String actionPlanKey) {
    HttpRequest request = executeSimpleAction(actionPlanKey, "close");
    return createActionPlanResult(request);
  }

  private HttpRequest executeSimpleAction(String actionPlanKey, String action) {
    HttpRequest request = requestFactory.post("/api/action_plans/" + action, EncodingUtils.toMap("key", actionPlanKey));
    if (!request.ok()) {
      throw new IllegalStateException("Fail to " + action + " action plan. Bad HTTP response status: " + request.code());
    }
    return request;
  }

  private ActionPlan createActionPlanResult(HttpRequest request){
    String json = request.body("UTF-8");
    Map jsonRoot = (Map) JSONValue.parse(json);
    return new ActionPlan((Map) jsonRoot.get("actionPlan"));
  }

}
