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
package org.sonar.server.issue;

import com.google.common.primitives.Ints;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.issue.*;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.server.platform.UserSession;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * All the issue features that are not published to public API.
 */
public class WebIssuesInternal implements ServerComponent {

  private final ServerIssueActions actions;
  private final ActionPlanManager actionPlanManager;

  public WebIssuesInternal(ServerIssueActions actions, ActionPlanManager actionPlanManager) {
    this.actions = actions;
    this.actionPlanManager = actionPlanManager;
  }

  public List<Transition> listTransitions(String issueKey) {
    return actions.listTransitions(issueKey, UserSession.get());
  }

  public Issue doTransition(String issueKey, String transitionKey) {
    return actions.doTransition(issueKey, transitionKey, UserSession.get());
  }

  public Issue assign(String issueKey, String transitionKey) {
    return actions.assign(issueKey, transitionKey, UserSession.get());
  }

  public Issue setSeverity(String issueKey, String severity) {
    return actions.setSeverity(issueKey, severity, UserSession.get());
  }

  public Issue plan(String issueKey, String actionPlanKey) {
    return actions.plan(issueKey, actionPlanKey, UserSession.get());
  }

  public IssueComment addComment(String issueKey, String comment) {
    return actions.addComment(issueKey, comment, UserSession.get());
  }

  public DefaultIssueComment[] comments(String issueKey) {
    return actions.comments(issueKey, UserSession.get());
  }

  public FieldDiffs[] changes(String issueKey) {
    return actions.changes(issueKey, UserSession.get());
  }

  public Issue create(Map<String, String> parameters) {
    String componentKey = parameters.get("component");
    // TODO verify authorization
    // TODO check existence of component
    DefaultIssueBuilder builder = new DefaultIssueBuilder().componentKey(componentKey);
    String line = parameters.get("line");
    builder.line(line != null ? Integer.parseInt(line) : null);
    builder.description(parameters.get("description"));
    builder.severity(parameters.get("severity"));
    String effortToFix = parameters.get("effortToFix");
    builder.effortToFix(effortToFix != null ? Double.parseDouble(effortToFix) : null);
    // TODO verify existence of rule
    builder.ruleKey(RuleKey.parse(parameters.get("rule")));
    builder.manual(true);
    Issue issue = builder.build();
    return actions.create((DefaultIssue) issue, UserSession.get());
  }

  Collection<ActionPlan> findOpenActionPlans(String projectKey) {
    return actionPlanManager.findOpenByProjectKey(projectKey);
  }

  ActionPlan findActionPlan(String actionPlanKey) {
    return actionPlanManager.findByKey(actionPlanKey);
  }

  List<ActionPlanStats> findOpenActionPlanStats(String projectKey) {
    return actionPlanManager.findOpenActionPlanStats(projectKey);
  }

  List<ActionPlanStats> findClosedActionPlanStats(String projectKey) {
    return actionPlanManager.findClosedActionPlanStats(projectKey);
  }

  public ActionPlan createActionPlan(Map<String, String> parameters) {
    // TODO verify authorization
    // TODO verify deadLine, uniquness of name, ...
    // TODO check existence of projectId
    Integer projectId = toInteger(parameters.get("projectId"));

    DefaultActionPlan actionPlan = DefaultActionPlan.create(parameters.get("name"))
                                     .setDescription(parameters.get("description"))
                                     .setUserLogin(UserSession.get().login())
                                     .setDeadLine(toDate(parameters.get("deadLine")));

    return actionPlanManager.create(actionPlan, projectId);
  }

  public ActionPlan updateActionPlan(String key, Map<String, String> parameters) {
    // TODO verify authorization
    // TODO verify deadLine
    // TODO check existence of projectId
    Integer projectId = toInteger(parameters.get("projectId"));
    DefaultActionPlan defaultActionPlan = (DefaultActionPlan) actionPlanManager.findByKey(key);
    defaultActionPlan.setName(parameters.get("name"));
    defaultActionPlan.setDescription(parameters.get("description"));
    defaultActionPlan.setDeadLine(toDate(parameters.get("deadLine")));
    return actionPlanManager.update(defaultActionPlan, projectId);
  }

  public ActionPlan closeActionPlan(String actionPlanKey) {
    // TODO verify authorization
    return actionPlanManager.setStatus(actionPlanKey, ActionPlan.STATUS_CLOSED);
  }

  public ActionPlan openActionPlan(String actionPlanKey) {
    // TODO verify authorization
    return actionPlanManager.setStatus(actionPlanKey, ActionPlan.STATUS_OPEN);
  }

  public void deleteActionPlan(String actionPlanKey) {
    // TODO verify authorization
    actionPlanManager.delete(actionPlanKey);
  }

  Date toDate(Object o) {
    if (o instanceof Date) {
      return (Date) o;
    }
    if (o instanceof String) {
      return DateUtils.parseDateTime((String) o);
    }
    return null;
  }

  Integer toInteger(Object o) {
    if (o instanceof Integer) {
      return (Integer) o;
    }
    if (o instanceof Long) {
      return Ints.checkedCast((Long) o);
    }

    if (o instanceof String) {
      return Integer.parseInt((String) o);
    }
    return null;
  }

}
