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

package org.sonar.server.issue;

import com.google.common.base.Strings;
import org.sonar.api.ServerSide;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.Map;

@ServerSide
public class PlanAction extends Action {

  public static final String KEY = "plan";
  public static final String VERIFIED_ACTION_PLAN = "verifiedActionPlan";

  private final ActionPlanService actionPlanService;
  private final IssueUpdater issueUpdater;

  public PlanAction(ActionPlanService actionPlanService, IssueUpdater issueUpdater) {
    super(KEY);
    this.actionPlanService = actionPlanService;
    this.issueUpdater = issueUpdater;
    super.setConditions(new IsUnResolved());
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<Issue> issues, UserSession userSession) {
    String actionPlanValue = planValue(properties);
    if (!Strings.isNullOrEmpty(actionPlanValue)) {
      ActionPlan actionPlan = selectActionPlan(actionPlanValue, userSession);
      if (actionPlan == null) {
        throw new IllegalArgumentException("Unknown action plan: " + actionPlanValue);
      }
      verifyIssuesAreAllRelatedOnActionPlanProject(issues, actionPlan);
      properties.put(VERIFIED_ACTION_PLAN, actionPlan);
    } else {
      properties.put(VERIFIED_ACTION_PLAN, null);
    }
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    if (!properties.containsKey(VERIFIED_ACTION_PLAN)) {
      throw new IllegalArgumentException("Action plan is missing from the execution parameters");
    }
    ActionPlan actionPlan = (ActionPlan) properties.get(VERIFIED_ACTION_PLAN);
    return issueUpdater.plan((DefaultIssue) context.issue(), actionPlan, context.issueChangeContext());
  }

  private String planValue(Map<String, Object> properties) {
    return (String) properties.get("plan");
  }

  private void verifyIssuesAreAllRelatedOnActionPlanProject(Collection<Issue> issues, ActionPlan actionPlan) {
    String projectKey = actionPlan.projectKey();
    for (Issue issue : issues) {
      DefaultIssue defaultIssue = (DefaultIssue) issue;
      String issueProjectKey = defaultIssue.projectKey();
      if (issueProjectKey == null || !issueProjectKey.equals(projectKey)) {
        throw new IllegalArgumentException("Issues are not all related to the action plan project: " + projectKey);
      }
    }
  }

  private ActionPlan selectActionPlan(String planValue, UserSession userSession) {
    return actionPlanService.findByKey(planValue, userSession);
  }
}
