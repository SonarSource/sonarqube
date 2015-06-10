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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.user.User;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.ws.UserJsonWriter;

public class IssueJsonWriter {

  public static final String ACTIONS_EXTRA_FIELD = "actions";
  public static final String TRANSITIONS_EXTRA_FIELD = "transitions";
  public static final String REPORTER_NAME_EXTRA_FIELD = "reporterName";
  public static final String ACTION_PLAN_NAME_EXTRA_FIELD = "actionPlanName";

  public static final Set<String> EXTRA_FIELDS = ImmutableSet.of(
    ACTIONS_EXTRA_FIELD, TRANSITIONS_EXTRA_FIELD, REPORTER_NAME_EXTRA_FIELD, ACTION_PLAN_NAME_EXTRA_FIELD);

  private final I18n i18n;
  private final Durations durations;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;
  private final IssueActionsWriter actionsWriter;

  public IssueJsonWriter(I18n i18n, Durations durations, UserSession userSession, UserJsonWriter userWriter, IssueActionsWriter actionsWriter) {
    this.i18n = i18n;
    this.durations = durations;
    this.userSession = userSession;
    this.userWriter = userWriter;
    this.actionsWriter = actionsWriter;
  }

  public void write(JsonWriter json, Issue issue, Map<String, User> usersByLogin, Map<String, ComponentDto> componentsByUuid,
    Map<String, ComponentDto> projectsByComponentUuid, Multimap<String, DefaultIssueComment> commentsByIssues, Map<String, ActionPlan> actionPlanByKeys, List<String> extraFields) {
    json.beginObject();

    String actionPlanKey = issue.actionPlanKey();
    ComponentDto file = componentsByUuid.get(issue.componentUuid());
    ComponentDto project = null, subProject = null;
    if (file != null) {
      project = projectsByComponentUuid.get(file.uuid());
      if (!file.projectUuid().equals(file.moduleUuid())) {
        subProject = componentsByUuid.get(file.moduleUuid());
      }
    }
    Duration debt = issue.debt();
    Date updateDate = issue.updateDate();

    json
      .prop("key", issue.key())
      .prop("component", file != null ? file.getKey() : null)
      // Only used for the compatibility with the Issues Java WS Client <= 4.4 used by Eclipse
      .prop("componentId", file != null ? file.getId() : null)
      .prop("project", project != null ? project.getKey() : null)
      .prop("subProject", subProject != null ? subProject.getKey() : null)
      .prop("rule", issue.ruleKey().toString())
      .prop("status", issue.status())
      .prop("resolution", issue.resolution())
      .prop("severity", issue.severity())
      .prop("message", issue.message())
      .prop("line", issue.line())
      .prop("debt", debt != null ? durations.encode(debt) : null)
      .prop("reporter", issue.reporter())
      .prop("author", issue.authorLogin())
      .prop("actionPlan", actionPlanKey)
      .prop("creationDate", isoDate(issue.creationDate()))
      .prop("updateDate", isoDate(updateDate))
      // TODO Remove as part of Front-end rework on Issue Domain
      .prop("fUpdateAge", formatAgeDate(updateDate))
      .prop("closeDate", isoDate(issue.closeDate()));

    json.name("assignee");
    userWriter.write(json, usersByLogin.get(issue.assignee()));

    writeTags(issue, json);
    writeIssueComments(commentsByIssues.get(issue.key()), usersByLogin, json);
    writeIssueAttributes(issue, json);
    writeIssueExtraFields(issue, usersByLogin, actionPlanByKeys, extraFields, json);
    json.endObject();
  }

  @CheckForNull
  private static String isoDate(@Nullable Date date) {
    if (date != null) {
      return DateUtils.formatDateTime(date);
    }
    return null;
  }

  private static void writeTags(Issue issue, JsonWriter json) {
    Collection<String> tags = issue.tags();
    if (tags != null && !tags.isEmpty()) {
      json.name("tags").beginArray();
      for (String tag : tags) {
        json.value(tag);
      }
      json.endArray();
    }
  }

  private void writeIssueComments(Collection<DefaultIssueComment> issueComments, Map<String, User> usersByLogin, JsonWriter json) {
    if (!issueComments.isEmpty()) {
      json.name("comments").beginArray();
      String login = userSession.getLogin();
      for (IssueComment comment : issueComments) {
        String userLogin = comment.userLogin();
        User user = userLogin != null ? usersByLogin.get(userLogin) : null;
        json.beginObject()
          .prop("key", comment.key())
          .prop("login", comment.userLogin())
          .prop("email", user != null ? user.email() : null)
          .prop("userName", user != null ? user.name() : null)
          .prop("htmlText", Markdown.convertToHtml(comment.markdownText()))
          .prop("markdown", comment.markdownText())
          .prop("updatable", login != null && login.equals(userLogin))
          .prop("createdAt", DateUtils.formatDateTime(comment.createdAt()))
          .endObject();
      }
      json.endArray();
    }
  }

  private static void writeIssueAttributes(Issue issue, JsonWriter json) {
    if (!issue.attributes().isEmpty()) {
      json.name("attr").beginObject();
      for (Map.Entry<String, String> entry : issue.attributes().entrySet()) {
        json.prop(entry.getKey(), entry.getValue());
      }
      json.endObject();
    }
  }

  private void writeIssueExtraFields(Issue issue, Map<String, User> usersByLogin, Map<String, ActionPlan> actionPlanByKeys,
    @Nullable List<String> extraFields,
    JsonWriter json) {
    if (extraFields != null) {
      if (extraFields.contains(ACTIONS_EXTRA_FIELD)) {
        actionsWriter.writeActions(issue, json);
      }

      if (extraFields.contains(TRANSITIONS_EXTRA_FIELD)) {
        actionsWriter.writeTransitions(issue, json);
      }

      writeReporterIfNeeded(issue, usersByLogin, extraFields, json);

      writeActionPlanIfNeeded(issue, actionPlanByKeys, extraFields, json);
    }
  }

  private void writeReporterIfNeeded(Issue issue, Map<String, User> usersByLogin, List<String> extraFields, JsonWriter json) {
    String reporter = issue.reporter();
    if (extraFields.contains(REPORTER_NAME_EXTRA_FIELD) && reporter != null) {
      User user = usersByLogin.get(reporter);
      json.prop(REPORTER_NAME_EXTRA_FIELD, user != null ? user.name() : null);
    }
  }

  private void writeActionPlanIfNeeded(Issue issue, Map<String, ActionPlan> actionPlanByKeys, List<String> extraFields, JsonWriter json) {
    String actionPlanKey = issue.actionPlanKey();
    if (extraFields.contains(ACTION_PLAN_NAME_EXTRA_FIELD) && actionPlanKey != null) {
      ActionPlan actionPlan = actionPlanByKeys.get(actionPlanKey);
      json.prop(ACTION_PLAN_NAME_EXTRA_FIELD, actionPlan != null ? actionPlan.name() : null);
    }
  }

  @CheckForNull
  private String formatAgeDate(@Nullable Date date) {
    if (date != null) {
      return i18n.ageFromNow(userSession.locale(), date);
    }
    return null;
  }
}
