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
package org.sonar.server.issue.ws;

import com.google.common.io.Resources;
import org.sonar.api.i18n.I18n;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.markdown.Markdown;
import org.sonar.server.db.DbClient;
import org.sonar.server.debt.DebtModelService;
import org.sonar.server.issue.IssueChangelog;
import org.sonar.server.issue.IssueChangelogService;
import org.sonar.server.issue.IssueCommentService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class IssueShowAction implements BaseIssuesWsAction {

  public static final String SHOW_ACTION = "show";

  private final DbClient dbClient;

  private final IssueService issueService;
  private final IssueChangelogService issueChangelogService;
  private final IssueCommentService commentService;
  private final IssueActionsWriter actionsWriter;
  private final ActionPlanService actionPlanService;
  private final UserFinder userFinder;
  private final DebtModelService debtModel;
  private final RuleService ruleService;
  private final I18n i18n;
  private final Durations durations;

  public IssueShowAction(DbClient dbClient, IssueService issueService, IssueChangelogService issueChangelogService, IssueCommentService commentService,
                         IssueActionsWriter actionsWriter, ActionPlanService actionPlanService, UserFinder userFinder, DebtModelService debtModel, RuleService ruleService,
                         I18n i18n, Durations durations) {
    this.dbClient = dbClient;
    this.issueService = issueService;
    this.issueChangelogService = issueChangelogService;
    this.commentService = commentService;
    this.actionsWriter = actionsWriter;
    this.actionPlanService = actionPlanService;
    this.userFinder = userFinder;
    this.debtModel = debtModel;
    this.ruleService = ruleService;
    this.i18n = i18n;
    this.durations = durations;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(SHOW_ACTION)
      .setDescription("Detail of issue")
      .setSince("4.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-show.json"));
    action.createParam("key")
      .setDescription("Issue key")
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
  }

  @Override
  public void handle(Request request, Response response) {
    String issueKey = request.mandatoryParam("key");

    JsonWriter json = response.newJsonWriter();
    json.beginObject().name("issue").beginObject();

    DbSession session = dbClient.openSession(false);
    try {
      Issue issue = issueService.getByKey(issueKey);

      writeIssue(session, issue, json);
      actionsWriter.writeActions(issue, json);
      actionsWriter.writeTransitions(issue, json);
      writeComments(issue, json);
      writeChangelog(issue, json);

    } finally {
      session.close();
    }

    json.endObject().endObject().close();
  }

  private void writeIssue(DbSession session, Issue issue, JsonWriter json) {
    String actionPlanKey = issue.actionPlanKey();
    ActionPlan actionPlan = actionPlanKey != null ? actionPlanService.findByKey(actionPlanKey, UserSession.get()) : null;
    Duration debt = issue.debt();
    Rule rule = ruleService.getNonNullByKey(issue.ruleKey());
    Date updateDate = issue.updateDate();
    Date closeDate = issue.closeDate();

    json
      .prop("key", issue.key())
      .prop("rule", issue.ruleKey().toString())
      .prop("ruleName", rule.name())
      .prop("line", issue.line())
      .prop("message", issue.message())
      .prop("resolution", issue.resolution())
      .prop("status", issue.status())
      .prop("severity", issue.severity())
      .prop("author", issue.authorLogin())
      .prop("actionPlan", actionPlanKey)
      .prop("actionPlanName", actionPlan != null ? actionPlan.name() : null)
      .prop("debt", debt != null ? durations.encode(debt) : null)
      .prop("creationDate", DateUtils.formatDateTime(issue.creationDate()))
      .prop("fCreationDate", formatDate(issue.creationDate()))
      .prop("updateDate", updateDate != null ? DateUtils.formatDateTime(updateDate) : null)
      .prop("fUpdateDate", formatDate(updateDate))
      .prop("fUpdateAge", formatAgeDate(updateDate))
      .prop("closeDate", closeDate != null ? DateUtils.formatDateTime(closeDate) : null)
      .prop("fCloseDate", formatDate(issue.closeDate()));

    addComponents(session, issue, json);
    addUserWithLabel(issue.assignee(), "assignee", json);
    addUserWithLabel(issue.reporter(), "reporter", json);
    addCharacteristics(rule, json);
  }

  private void addComponents(DbSession session, Issue issue, JsonWriter json) {
    ComponentDto component = dbClient.componentDao().getByUuid(session, issue.componentUuid());
    Long parentProjectId = component.parentProjectId();
    ComponentDto parentProject = parentProjectId != null ? dbClient.componentDao().getNullableById(parentProjectId, session) : null;
    ComponentDto project = dbClient.componentDao().getByUuid(session, component.projectUuid());

    String projectName = project.longName() != null ? project.longName() : project.name();
    // Do not display sub project long name if sub project and project are the same
    boolean displayParentProjectLongName = parentProject != null && !parentProject.getId().equals(project.getId());
    String parentProjectKey = displayParentProjectLongName ? parentProject.key() : null;
    String parentProjectName = displayParentProjectLongName ? parentProject.longName() != null ? parentProject.longName() : parentProject.name() : null;

    json
      .prop("component", component.key())
      .prop("componentLongName", component.longName())
      .prop("componentQualifier", component.qualifier())
      .prop("componentEnabled", component.isEnabled())
      .prop("project", project.key())
      .prop("projectName", projectName)
      //TODO replace subProject names by parentProject
      .prop("subProject", parentProjectKey)
      .prop("subProjectName", parentProjectName);
  }

  private void writeComments(Issue issue, JsonWriter json) {
    json.name("comments").beginArray();
    String login = UserSession.get().login();

    Map<String, User> usersByLogin = newHashMap();
    List<DefaultIssueComment> comments = commentService.findComments(issue.key());
    for (IssueComment comment : comments) {
      String userLogin = comment.userLogin();
      User user = usersByLogin.get(userLogin);
      if (user == null) {
        user = userFinder.findByLogin(userLogin);
        if (user != null) {
          usersByLogin.put(userLogin, user);
        }
      }
    }

    for (IssueComment comment : comments) {
      String userLogin = comment.userLogin();
      User user = usersByLogin.get(userLogin);
      json
        .beginObject()
        .prop("key", comment.key())
        .prop("userName", user != null ? user.name() : null)
        .prop("raw", comment.markdownText())
        .prop("html", Markdown.convertToHtml(comment.markdownText()))
        .prop("createdAt", DateUtils.formatDateTime(comment.createdAt()))
        .prop("fCreatedAge", formatAgeDate(comment.createdAt()))
        .prop("updatable", login != null && login.equals(userLogin))
        .endObject();
    }
    json.endArray();
  }

  private void writeChangelog(Issue issue, JsonWriter json) {
    json.name("changelog").beginArray()
      .beginObject()
      .prop("creationDate", DateUtils.formatDateTime(issue.creationDate()))
      .prop("fCreationDate", formatDate(issue.creationDate()))
      .name("diffs").beginArray()
      .value(i18n.message(UserSession.get().locale(), "created", null))
      .endArray()
      .endObject();

    IssueChangelog changelog = issueChangelogService.changelog(issue);
    for (FieldDiffs diffs : changelog.changes()) {
      User user = changelog.user(diffs);
      json
        .beginObject()
        .prop("userName", user != null ? user.name() : null)
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

  private void addUserWithLabel(@Nullable String value, String field, JsonWriter json) {
    if (value != null) {
      User user = userFinder.findByLogin(value);
      json
        .prop(field, value)
        .prop(field + "Name", user != null ? user.name() : null);
    }
  }

  @CheckForNull
  private String formatDate(@Nullable Date date) {
    if (date != null) {
      return i18n.formatDateTime(UserSession.get().locale(), date);
    }
    return null;
  }

  @CheckForNull
  private String formatAgeDate(@Nullable Date date) {
    if (date != null) {
      return i18n.ageFromNow(UserSession.get().locale(), date);
    }
    return null;
  }

  private void addCharacteristics(Rule rule, JsonWriter json) {
    String subCharacteristicKey = rule.debtCharacteristicKey();
    DebtCharacteristic subCharacteristic = characteristicByKey(subCharacteristicKey);
    if (subCharacteristic != null) {
      json.prop("subCharacteristic", subCharacteristic.name());
      DebtCharacteristic characteristic = characteristicById(((DefaultDebtCharacteristic) subCharacteristic).parentId());
      json.prop("characteristic", characteristic != null ? characteristic.name() : null);
    }
  }

  @CheckForNull
  private DebtCharacteristic characteristicById(@Nullable Integer id) {
    if (id != null) {
      return debtModel.characteristicById(id);
    }
    return null;
  }

  @CheckForNull
  private DebtCharacteristic characteristicByKey(@Nullable String key) {
    if (key != null) {
      return debtModel.characteristicByKey(key);
    }
    return null;
  }
}
