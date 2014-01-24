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
package org.sonar.server.issue.ws;

import org.sonar.api.i18n.I18n;
import org.sonar.api.issue.*;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.issue.internal.WorkDayDuration;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.markdown.Markdown;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.ActionService;
import org.sonar.server.issue.IssueChangelog;
import org.sonar.server.issue.IssueChangelogService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.technicaldebt.TechnicalDebtFormatter;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class IssueShowWsHandler implements RequestHandler {

  private final IssueFinder issueFinder;
  private final IssueService issueService;
  private final IssueChangelogService issueChangelogService;
  private final ActionService actionService;
  private final TechnicalDebtFormatter technicalDebtFormatter;
  private final I18n i18n;

  public IssueShowWsHandler(IssueFinder issueFinder, IssueService issueService, IssueChangelogService issueChangelogService, ActionService actionService,
                            TechnicalDebtFormatter technicalDebtFormatter, I18n i18n) {
    this.issueFinder = issueFinder;
    this.issueService = issueService;
    this.issueChangelogService = issueChangelogService;
    this.actionService = actionService;
    this.technicalDebtFormatter = technicalDebtFormatter;
    this.i18n = i18n;
  }

  @Override
  public void handle(Request request, Response response) {
    String issueKey = request.requiredParam("key");
    IssueQueryResult queryResult = issueFinder.find(IssueQuery.builder().issueKeys(Arrays.asList(issueKey)).build());
    if (queryResult.issues().size() != 1) {
      throw new NotFoundException("Issue not found: " + issueKey);
    }
    DefaultIssue issue = (DefaultIssue) queryResult.first();

    JsonWriter json = response.newJsonWriter();
    json.beginObject().name("issue").beginObject();

    writeIssue(queryResult, issue, json);
    writeTransitions(issue, json);
    writeActions(issue, json);
    writeComments(queryResult, issue, json);
    writeChangelog(issue, json);

    json.endObject().endObject().close();
  }

  private void writeIssue(IssueQueryResult result, DefaultIssue issue, JsonWriter json) {
    String actionPlanKey = issue.actionPlanKey();
    WorkDayDuration technicalDebt = issue.technicalDebt();
    Date updateDate = issue.updateDate();
    Date closeDate = issue.closeDate();

    json
      .prop("key", issue.key())
      .prop("component", issue.componentKey())
      .prop("componentLongName", result.component(issue).longName())
      .prop("componentQualifier", result.component(issue).qualifier())
      .prop("project", result.project(issue).key())
      .prop("projectLongName", result.project(issue).longName())
      .prop("rule", issue.ruleKey().toString())
      .prop("ruleName", result.rule(issue).getName())
      .prop("line", issue.line())
      .prop("message", issue.message())
      .prop("resolution", issue.resolution())
      .prop("status", issue.status())
      .prop("severity", issue.severity())
      .prop("author", issue.authorLogin())
      .prop("actionPlan", actionPlanKey)
      .prop("debt", technicalDebt != null ? technicalDebtFormatter.format(UserSession.get().locale(), technicalDebt) : null)
      .prop("actionPlanName", actionPlanKey != null ? result.actionPlan(issue).name() : null)
      .prop("creationDate", DateUtils.formatDateTime(issue.creationDate()))
      .prop("fCreationDate", formatDate(issue.creationDate()))
      .prop("updateDate", updateDate != null ? DateUtils.formatDateTime(updateDate) : null)
      .prop("fUpdateDate", formatDate(updateDate))
      .prop("closeDate", closeDate != null ? DateUtils.formatDateTime(closeDate) : null)
      .prop("fCloseDate", formatDate(issue.closeDate()));

    addUserWithLabel(result, issue.assignee(), "assignee", json);
    addUserWithLabel(result, issue.reporter(), "reporter", json);
  }

  private void writeTransitions(Issue issue, JsonWriter json) {
    json.name("transitions").beginArray();
    if (UserSession.get().isLoggedIn()) {
      List<Transition> transitions = issueService.listTransitions(issue, UserSession.get());
      for (Transition transition : transitions) {
        json.value(transition.key());
      }
    }
    json.endArray();
  }

  private void writeActions(DefaultIssue issue, JsonWriter json) {
    json.name("actions").beginArray();
    for (String action : actions(issue)) {
      json.value(action);
    }
    json.endArray();
  }

  // TODO all available actions should be returned by ActionService or another service
  private List<String> actions(DefaultIssue issue) {
    List<String> actions = newArrayList();
    if (UserSession.get().isLoggedIn()) {
      actions.add("comment");
      if (issue.resolution() == null) {
        actions.add("assign");
        if (!UserSession.get().login().equals(issue.assignee())) {
          actions.add("assign_to_me");
        }
        actions.add("plan");
        if (UserSession.get().hasProjectPermission(UserRole.ISSUE_ADMIN, issue.projectKey())) {
          actions.add("severity");
        }
        for (Action action : actionService.listAvailableActions(issue)) {
          actions.add(action.key());
        }
      }
    }
    return actions;
  }

  private void writeComments(IssueQueryResult queryResult, Issue issue, JsonWriter json) {
    json.name("comments").beginArray();
    for (IssueComment comment : issue.comments()) {
      String userLogin = comment.userLogin();
      json
        .beginObject()
        .prop("key", comment.key())
        .prop("userName", userLogin != null ? queryResult.user(userLogin).name() : null)
        .prop("raw", comment.markdownText())
        .prop("html", Markdown.convertToHtml(comment.markdownText()))
        .prop("createdAt", DateUtils.formatDateTime(comment.createdAt()))
        .prop("fCreatedAt", i18n.instant(UserSession.get().locale(), comment.createdAt()))
        .prop("updatable", UserSession.get().isLoggedIn() && UserSession.get().login().equals(comment.userLogin()))
        .endObject();
    }
    json.endArray();
  }

  private void writeChangelog(Issue issue, JsonWriter json) {
    json.name("changelog").beginArray();
    IssueChangelog changelog = issueChangelogService.changelog(issue);
    for (FieldDiffs diffs : changelog.changes()) {
      String userLogin = diffs.userLogin();
      json
        .beginObject()
        .prop("userName", userLogin != null ? changelog.user(diffs).name() : null)
        .prop("creationDate", DateUtils.formatDateTime(diffs.creationDate()))
        .prop("fCreationDate", formatDate(diffs.creationDate()));

      json.name("diffs").beginArray();
      List<String> diffsFormatted = issueChangelogService.formatDiffs(diffs);
      for (String diff : diffsFormatted) {
        json.value(diff);
      }
      json.endArray();
      json.endObject();
    }
    json.endArray();
  }

  private void addUserWithLabel(IssueQueryResult result, String value, String field, JsonWriter json) {
    if (value != null) {
      json
        .prop(field, value)
        .prop(field + "Name", result.user(value).name());
    }
  }

  @CheckForNull
  private String formatDate(@Nullable Date date) {
    if (date != null) {
      return i18n.formatDateTime(UserSession.get().locale(), date);
    }
    return null;
  }
}
