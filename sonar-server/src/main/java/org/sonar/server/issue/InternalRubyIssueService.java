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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
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
import org.sonar.server.util.RubyUtils;

import javax.annotation.Nullable;
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
    return issueService.listTransitions(issueKey);
  }

  public List<Transition> listTransitions(Issue issue) {
    return issueService.listTransitions(issue);
  }

  public List<String> listStatus() {
    return issueService.listStatus();
  }

  public Issue doTransition(String issueKey, String transitionKey) {
    return issueService.doTransition(issueKey, transitionKey, UserSession.get());
  }

  public Issue assign(String issueKey, @Nullable String assignee) {
    return issueService.assign(issueKey, StringUtils.defaultIfBlank(assignee, null), UserSession.get());
  }

  public Issue setSeverity(String issueKey, String severity) {
    return issueService.setSeverity(issueKey, severity, UserSession.get());
  }

  public Issue plan(String issueKey, @Nullable String actionPlanKey) {
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

  public IssueComment findComment(String commentKey) {
    return commentService.findComment(commentKey);
  }

  /**
   * Create manual issue
   */
  public Result<DefaultIssue> create(Map<String, String> params) {
    Result<DefaultIssue> result = Result.of();
    try {
      // mandatory parameters
      String componentKey = params.get("component");
      if (StringUtils.isBlank(componentKey)) {
        result.addError("Component is not set");
      }
      RuleKey ruleKey = null;
      String rule = params.get("rule");
      if (StringUtils.isBlank(rule)) {
        result.addError(Result.Message.ofL10n("issue.manual.missing_rule"));
      } else {
        ruleKey = RuleKey.parse(rule);
      }

      if (result.ok()) {
        DefaultIssue issue = (DefaultIssue)new DefaultIssueBuilder()
          .componentKey(componentKey)
          .line(RubyUtils.toInteger(params.get("line")))
          .message(params.get("message"))
          .severity(params.get("severity"))
          .effortToFix(RubyUtils.toDouble(params.get("effortToFix")))
          .ruleKey(ruleKey)
          .reporter(UserSession.get().login())
          .build();
        issue = issueService.createManualIssue(issue, UserSession.get());
        result.set(issue);
      }

    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public Collection<ActionPlan> findOpenActionPlans(String issueKey) {
    String componentKey = issueService.loadIssue(issueKey).componentKey();
    return actionPlanService.findOpenByComponentKey(componentKey);
  }

  public ActionPlan findActionPlan(String actionPlanKey) {
    return actionPlanService.findByKey(actionPlanKey);
  }

  public List<ActionPlanStats> findActionPlanStats(String projectKey) {
    return actionPlanService.findActionPlanStats(projectKey);
  }

  public Result<ActionPlan> createActionPlan(Map<String, String> parameters) {
    // TODO verify authorization

    Result<ActionPlan> result = createActionPlanResult(parameters);
    if (result.ok()) {
      result.set(actionPlanService.create(result.get()));
    }
    return result;
  }

  public Result<ActionPlan> updateActionPlan(String key, Map<String, String> parameters) {
    // TODO verify authorization

    DefaultActionPlan existingActionPlan = (DefaultActionPlan) actionPlanService.findByKey(key);
    if (existingActionPlan == null) {
      Result<ActionPlan> result = Result.of();
      result.addError(Result.Message.ofL10n("action_plans.errors.action_plan_does_not_exist", key));
      return result;
    } else {
      Result<ActionPlan> result = createActionPlanResult(parameters, existingActionPlan.name());
      if (result.ok()) {
        DefaultActionPlan actionPlan = (DefaultActionPlan) result.get();
        actionPlan.setKey(existingActionPlan.key());
        actionPlan.setUserLogin(existingActionPlan.userLogin());
        result.set(actionPlanService.update(actionPlan));
      }
      return result;
    }
  }

  public Result<ActionPlan> closeActionPlan(String actionPlanKey) {
    // TODO verify authorization
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      result.set(actionPlanService.setStatus(actionPlanKey, ActionPlan.STATUS_CLOSED));
    }
    return result;
  }

  public Result<ActionPlan> openActionPlan(String actionPlanKey) {
    // TODO verify authorization
    Result<ActionPlan> result = createResultForExistingActionPlan(actionPlanKey);
    if (result.ok()) {
      result.set(actionPlanService.setStatus(actionPlanKey, ActionPlan.STATUS_OPEN));
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
  Result<ActionPlan> createActionPlanResult(Map<String, String> parameters, @Nullable String oldName) {
    Result<ActionPlan> result = Result.of();

    String name = parameters.get("name");
    String description = parameters.get("description");
    String deadLineParam = parameters.get("deadLine");
    String projectParam = parameters.get("project");
    Date deadLine = null;

    if (Strings.isNullOrEmpty(name)) {
      result.addError(Result.Message.ofL10n("errors.cant_be_empty", "name"));
    } else {
      if (name.length() > 200) {
        result.addError(Result.Message.ofL10n("errors.is_too_long", "name", 200));
      }
    }

    if (!Strings.isNullOrEmpty(description) && description.length() > 1000) {
      result.addError(Result.Message.ofL10n("errors.is_too_long", "description", 1000));
    }

    if (Strings.isNullOrEmpty(projectParam) && oldName == null) {
      result.addError(Result.Message.ofL10n("errors.cant_be_empty", "project"));
    } else {
      ResourceDto project = resourceDao.getResource(ResourceQuery.create().setKey(projectParam));
      if (project == null) {
        result.addError(Result.Message.ofL10n("action_plans.errors.project_does_not_exist", projectParam));
      }
    }

    if (!Strings.isNullOrEmpty(deadLineParam)) {
      try {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        deadLine = dateFormat.parse(deadLineParam);
        Date today = new Date();
        if (deadLine.before(today) && !DateUtils.isSameDay(deadLine, today)) {
          result.addError(Result.Message.ofL10n("action_plans.date_cant_be_in_past"));
        }
      } catch (Exception e) {
        result.addError(Result.Message.ofL10n("errors.is_not_valid", "date"));
      }
    }

    if (!Strings.isNullOrEmpty(projectParam) && !Strings.isNullOrEmpty(name) && !name.equals(oldName)
      && actionPlanService.isNameAlreadyUsedForProject(name, projectParam)) {
      result.addError(Result.Message.ofL10n("action_plans.same_name_in_same_project"));
    }

    if (result.ok()) {
      DefaultActionPlan actionPlan = DefaultActionPlan.create(name)
        .setProjectKey(projectParam)
        .setDescription(description)
        .setUserLogin(UserSession.get().login())
        .setDeadLine(deadLine);
      result.set(actionPlan);
    }
    return result;
  }

  private Result<ActionPlan> createResultForExistingActionPlan(String actionPlanKey) {
    Result<ActionPlan> result = Result.of();
    if (findActionPlan(actionPlanKey) == null) {
      result.addError(Result.Message.ofL10n("action_plans.errors.action_plan_does_not_exist", actionPlanKey));
    }
    return result;
  }
}