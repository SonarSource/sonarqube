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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.ActionPlanStats;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueBuilder;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.server.platform.UserSession;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * All the issue features that are not published to public API.
 */
public class InternalRubyIssueService implements ServerComponent {

  private final IssueService issueService;
  private final IssueCommentService commentService;
  private final ActionPlanService actionPlanService;
  private final ResourceDao resourceDao;

  public InternalRubyIssueService(IssueService issueService,
                                  IssueCommentService commentService,
                                  ActionPlanService actionPlanService,
                                  ResourceDao resourceDao) {
    this.issueService = issueService;
    this.commentService = commentService;
    this.actionPlanService = actionPlanService;
    this.resourceDao = resourceDao;
  }

  public List<Transition> listTransitions(String issueKey) {
    return issueService.listTransitions(issueKey, UserSession.get());
  }

  public Issue doTransition(String issueKey, String transitionKey) {
    return issueService.doTransition(issueKey, transitionKey, UserSession.get());
  }

  public Issue assign(String issueKey, String transitionKey) {
    return issueService.assign(issueKey, transitionKey, UserSession.get());
  }

  public Issue setSeverity(String issueKey, String severity) {
    return issueService.setSeverity(issueKey, severity, UserSession.get());
  }

  public Issue plan(String issueKey, String actionPlanKey) {
    return issueService.plan(issueKey, actionPlanKey, UserSession.get());
  }

  public IssueComment addComment(String issueKey, String text) {
    return commentService.addComment(issueKey, text, UserSession.get());
  }

  public IssueComment deleteComment(String commentKey) {
    return commentService.deleteComment(commentKey, UserSession.get());
  }

  public IssueComment editComment(String commentKey, String newText) {
    return commentService.editComment(commentKey, newText, UserSession.get());
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
    return issueService.create((DefaultIssue) issue, UserSession.get());
  }

  Collection<ActionPlan> findOpenActionPlans(String projectKey) {
    return actionPlanService.findOpenByProjectKey(projectKey);
  }

  ActionPlan findActionPlan(String actionPlanKey) {
    return actionPlanService.findByKey(actionPlanKey);
  }

  List<ActionPlanStats> findActionPlanStats(String projectKey) {
    return actionPlanService.findActionPlanStats(projectKey);
  }

  public Result<ActionPlan> createActionPlan(Map<String, String> parameters) {
    // TODO verify authorization

    Result<ActionPlan> result = createActionPlanResult(parameters);
    if (result.ok()) {
      result.setObject(actionPlanService.create(result.get()));
    }
    return result;
  }

  public Result<ActionPlan> updateActionPlan(String key, Map<String, String> parameters) {
    // TODO verify authorization

    DefaultActionPlan existingActionPlan = (DefaultActionPlan) actionPlanService.findByKey(key);
    if (existingActionPlan == null) {
      Result<ActionPlan> result = new Result<ActionPlan>();
      result.addError("issues_action_plans.errors.action_plan_does_not_exists", key);
      return result;
    } else {
      Result<ActionPlan> result = createActionPlanResult(parameters, existingActionPlan.name());
      if (result.ok()) {
        DefaultActionPlan actionPlan = (DefaultActionPlan) result.get();
        actionPlan.setKey(existingActionPlan.key());
        actionPlan.setUserLogin(existingActionPlan.userLogin());
        result.setObject(actionPlanService.update(actionPlan));
      }
      return result;
    }
  }

  public Result<ActionPlan> closeActionPlan(String actionPlanKey) {
    // TODO verify authorization
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      result.setObject(actionPlanService.setStatus(actionPlanKey, ActionPlan.STATUS_CLOSED));
    }
    return result;
  }

  public Result<ActionPlan> openActionPlan(String actionPlanKey) {
    // TODO verify authorization
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      result.setObject(actionPlanService.setStatus(actionPlanKey, ActionPlan.STATUS_OPEN));
    }
    return result;
  }

  public Result<ActionPlan> deleteActionPlan(String actionPlanKey) {
    // TODO verify authorization
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      actionPlanService.delete(actionPlanKey);
    }
    return result;
  }

  @VisibleForTesting
  Result createActionPlanResult(Map<String, String> parameters) {
    return createActionPlanResult(parameters, null);
  }

  @VisibleForTesting
  Result<ActionPlan> createActionPlanResult(Map<String, String> parameters, String oldName) {
    Result<ActionPlan> result = new Result<ActionPlan>();

    String name = parameters.get("name");
    String description = parameters.get("description");
    String deadLineParam = parameters.get("deadLine");
    String projectParam = parameters.get("project");
    Date deadLine = null;

    if (Strings.isNullOrEmpty(name)) {
      result.addError("errors.cant_be_empty", "name");
    } else {
      if (name.length() > 200) {
        result.addError("errors.is_too_long", "name", 200);
      }
    }

    if (!Strings.isNullOrEmpty(description) && description.length() > 1000) {
      result.addError("errors.is_too_long", "description", 1000);
    }

    if (Strings.isNullOrEmpty(projectParam) && oldName == null) {
      result.addError("errors.cant_be_empty", "project");
    } else {
      ResourceDto project = resourceDao.getResource(ResourceQuery.create().setKey(projectParam));
      if (project == null) {
        result.addError("issues_action_plans.errors.project_does_not_exists", projectParam);
      }
    }

    if (!Strings.isNullOrEmpty(deadLineParam)) {
      try {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        deadLine = dateFormat.parse(deadLineParam);
        if (deadLine.before(new Date())) {
          result.addError("issues_action_plans.date_cant_be_in_past");
        }
      } catch (Exception e) {
        result.addError("errors.is_not_valid", "date");
      }
    }

    if (!Strings.isNullOrEmpty(projectParam) && !Strings.isNullOrEmpty(name) && !name.equals(oldName)
          && actionPlanService.isNameAlreadyUsedForProject(name, projectParam)) {
      result.addError("issues_action_plans.same_name_in_same_project");
    }

    if (result.ok()) {
      DefaultActionPlan actionPlan = DefaultActionPlan.create(name)
                                       .setProjectKey(projectParam)
                                       .setDescription(description)
                                       .setUserLogin(UserSession.get().login())
                                       .setDeadLine(deadLine);
      result.setObject(actionPlan);
    }
    return result;
  }

  private Result<ActionPlan> createResultForExistingActionPlan(String actionPlanKey) {
    Result<ActionPlan> result = new Result<ActionPlan>();
    if (findActionPlan(actionPlanKey) == null) {
      result.addError("issues_action_plans.errors.action_plan_does_not_exists", actionPlanKey);
    }
    return result;
  }
}