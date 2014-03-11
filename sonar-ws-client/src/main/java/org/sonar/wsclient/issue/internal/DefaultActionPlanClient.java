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

import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.EncodingUtils;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.issue.*;

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
    String json = requestFactory.get(ActionPlanQuery.BASE_URL, EncodingUtils.toMap("project", projectKey));
    List<ActionPlan> result = new ArrayList<ActionPlan>();
    Map jsonRoot = (Map) JSONValue.parse(json);
    List<Map> jsonActionPlans = (List<Map>) jsonRoot.get("actionPlans");
    if (jsonActionPlans != null) {
      for (Map jsonActionPlan : jsonActionPlans) {
        result.add(new DefaultActionPlan(jsonActionPlan));
      }
    }
    return result;
  }

  @Override
  public ActionPlan create(NewActionPlan newActionPlan) {
    String json = requestFactory.post("/api/action_plans/create", newActionPlan.urlParams());
    return createActionPlanResult(json);
  }

  @Override
  public ActionPlan update(UpdateActionPlan updateActionPlan) {
    String json = requestFactory.post("/api/action_plans/update", updateActionPlan.urlParams());
    return createActionPlanResult(json);
  }

  @Override
  public void delete(String actionPlanKey) {
    executeSimpleAction(actionPlanKey, "delete");
  }

  @Override
  public ActionPlan open(String actionPlanKey) {
    String json = executeSimpleAction(actionPlanKey, "open");
    return createActionPlanResult(json);
  }

  @Override
  public ActionPlan close(String actionPlanKey) {
    String json = executeSimpleAction(actionPlanKey, "close");
    return createActionPlanResult(json);
  }

  private String executeSimpleAction(String actionPlanKey, String action) {
    return requestFactory.post("/api/action_plans/" + action, EncodingUtils.toMap("key", actionPlanKey));
  }

  private ActionPlan createActionPlanResult(String json) {
    Map jsonRoot = (Map) JSONValue.parse(json);
    return new DefaultActionPlan((Map) jsonRoot.get("actionPlan"));
  }

}
