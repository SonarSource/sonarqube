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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
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
import org.sonar.server.ws.JsonWriterUtils;

import static org.sonar.server.ws.JsonWriterUtils.writeIfNeeded;

public class IssueJsonWriter {

  private static final String FIELD_KEY = "key";
  private static final String FIELD_COMPONENT = "component";
  private static final String FIELD_COMPONENT_ID = "componentId";
  private static final String FIELD_PROJECT = "project";
  private static final String FIELD_SUB_PROJECT = "subProject";
  private static final String FIELD_RULE = "rule";
  private static final String FIELD_STATUS = "status";
  private static final String FIELD_RESOLUTION = "resolution";
  private static final String FIELD_AUTHOR = "author";
  private static final String FIELD_REPORTER = "reporter";
  private static final String FIELD_ASSIGNEE = "assignee";
  private static final String FIELD_DEBT = "debt";
  private static final String FIELD_LINE = "line";
  private static final String FIELD_MESSAGE = "message";
  private static final String FIELD_SEVERITY = "severity";
  private static final String FIELD_ACTION_PLAN = "actionPlan";
  private static final String FIELD_CREATION_DATE = "creationDate";
  private static final String FIELD_UPDATE_DATE = "updateDate";
  private static final String FIELD_CLOSE_DATE = "closeDate";
  private static final String FIELD_TAGS = "tags";
  private static final String FIELD_COMMENTS = "comments";
  private static final String FIELD_ATTRIBUTES = "attr";
  public static final String FIELD_ACTIONS = "actions";
  public static final String FIELD_TRANSITIONS = "transitions";
  public static final String FIELD_ACTION_PLAN_NAME = "actionPlanName";

  public static final Set<String> EXTRA_FIELDS = ImmutableSet.of(
    FIELD_ACTIONS, FIELD_TRANSITIONS, FIELD_ACTION_PLAN_NAME);

  public static final Set<String> SELECTABLE_FIELDS = ImmutableSet.of(FIELD_COMPONENT, FIELD_PROJECT, FIELD_SUB_PROJECT, FIELD_RULE, FIELD_STATUS, FIELD_RESOLUTION, FIELD_AUTHOR,
    FIELD_REPORTER, FIELD_ASSIGNEE, FIELD_DEBT, FIELD_LINE, FIELD_MESSAGE, FIELD_SEVERITY, FIELD_ACTION_PLAN, FIELD_CREATION_DATE, FIELD_UPDATE_DATE, FIELD_CLOSE_DATE,
    FIELD_COMPONENT_ID, FIELD_TAGS, FIELD_COMMENTS, FIELD_ATTRIBUTES, FIELD_ACTIONS, FIELD_TRANSITIONS, FIELD_ACTION_PLAN_NAME);

  private static final List<String> SELECTABLE_MINUS_EXTRAS = ImmutableList.copyOf(Sets.difference(SELECTABLE_FIELDS, EXTRA_FIELDS));

  private final I18n i18n;
  private final Durations durations;
  private final UserSession userSession;
  private final IssueActionsWriter actionsWriter;

  public IssueJsonWriter(I18n i18n, Durations durations, UserSession userSession, IssueActionsWriter actionsWriter) {
    this.i18n = i18n;
    this.durations = durations;
    this.userSession = userSession;
    this.actionsWriter = actionsWriter;
  }

  public void write(JsonWriter json, Issue issue, Map<String, User> usersByLogin, Map<String, ComponentDto> componentsByUuid,
    Map<String, ComponentDto> projectsByComponentUuid, Multimap<String, DefaultIssueComment> commentsByIssues, Map<String, ActionPlan> actionPlanByKeys,
    @Nullable List<String> selectedFields) {

    List<String> fields = Lists.newArrayList();
    if (selectedFields == null || selectedFields.isEmpty()) {
      fields.addAll(SELECTABLE_MINUS_EXTRAS);
    } else {
      fields.addAll(selectedFields);
    }

    json.beginObject();

    String actionPlanKey = issue.actionPlanKey();
    ComponentDto file = componentsByUuid.get(issue.componentUuid());
    ComponentDto project = null;
    ComponentDto subProject = null;
    if (file != null) {
      project = projectsByComponentUuid.get(file.uuid());
      if (!file.projectUuid().equals(file.moduleUuid())) {
        subProject = componentsByUuid.get(file.moduleUuid());
      }
    }
    Duration debt = issue.debt();
    Date updateDate = issue.updateDate();

    json.prop(FIELD_KEY, issue.key());
    JsonWriterUtils.writeIfNeeded(json, file != null ? file.getKey() : null, FIELD_COMPONENT, fields);
    // Only used for the compatibility with the Issues Java WS Client <= 4.4 used by Eclipse
    writeIfNeeded(json, file != null ? file.getId() : null, FIELD_COMPONENT_ID, fields);
    writeIfNeeded(json, project != null ? project.getKey() : null, FIELD_PROJECT, fields);
    writeIfNeeded(json, subProject != null ? subProject.getKey() : null, FIELD_SUB_PROJECT, fields);
    writeIfNeeded(json, issue.ruleKey().toString(), FIELD_RULE, fields);
    writeIfNeeded(json, issue.status(), FIELD_STATUS, fields);
    writeIfNeeded(json, issue.resolution(), FIELD_RESOLUTION, fields);
    writeIfNeeded(json, issue.severity(), FIELD_SEVERITY, fields);
    writeIfNeeded(json, issue.message(), FIELD_MESSAGE, fields);
    writeIfNeeded(json, issue.line(), FIELD_LINE, fields);
    writeIfNeeded(json, debt != null ? durations.encode(debt) : null, FIELD_DEBT, fields);
    writeIfNeeded(json, issue.assignee(), FIELD_ASSIGNEE, fields);
    writeIfNeeded(json, issue.reporter(), FIELD_REPORTER, fields);
    writeIfNeeded(json, issue.authorLogin(), FIELD_AUTHOR, fields);
    writeIfNeeded(json, actionPlanKey, FIELD_ACTION_PLAN, fields);
    writeIfNeeded(json, isoDate(issue.creationDate()), FIELD_CREATION_DATE, fields);
    writeIfNeeded(json, isoDate(updateDate), FIELD_UPDATE_DATE, fields);
    writeIfNeeded(json, isoDate(issue.closeDate()), FIELD_CLOSE_DATE, fields);

    if (JsonWriterUtils.isFieldWanted(FIELD_TAGS, fields)) {
      writeTags(issue, json);
    }
    if (JsonWriterUtils.isFieldWanted(FIELD_COMMENTS, fields)) {
      writeIssueComments(commentsByIssues.get(issue.key()), usersByLogin, json);
    }
    if (JsonWriterUtils.isFieldWanted(FIELD_ATTRIBUTES, fields)) {
      writeIssueAttributes(issue, json);
    }
    writeIssueExtraFields(issue, actionPlanByKeys, fields, json);
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
      json.name(FIELD_TAGS).beginArray();
      for (String tag : tags) {
        json.value(tag);
      }
      json.endArray();
    }
  }

  private void writeIssueComments(Collection<DefaultIssueComment> issueComments, Map<String, User> usersByLogin, JsonWriter json) {
    if (!issueComments.isEmpty()) {
      json.name(FIELD_COMMENTS).beginArray();
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
      json.name(FIELD_ATTRIBUTES).beginObject();
      for (Map.Entry<String, String> entry : issue.attributes().entrySet()) {
        json.prop(entry.getKey(), entry.getValue());
      }
      json.endObject();
    }
  }

  private void writeIssueExtraFields(Issue issue, Map<String, ActionPlan> actionPlanByKeys,
    @Nullable List<String> fields, JsonWriter json) {
    if (JsonWriterUtils.isFieldWanted(FIELD_ACTIONS, fields)) {
      actionsWriter.writeActions(issue, json);
    }

    if (JsonWriterUtils.isFieldWanted(FIELD_TRANSITIONS, fields)) {
      actionsWriter.writeTransitions(issue, json);
    }

    if (JsonWriterUtils.isFieldWanted(FIELD_ACTION_PLAN_NAME, fields)) {
      writeActionPlanName(issue, actionPlanByKeys, json);
    }
  }

  private void writeActionPlanName(Issue issue, Map<String, ActionPlan> actionPlanByKeys, JsonWriter json) {
    String actionPlanKey = issue.actionPlanKey();
    if (actionPlanKey != null) {
      ActionPlan actionPlan = actionPlanByKeys.get(actionPlanKey);
      json.prop(FIELD_ACTION_PLAN_NAME, actionPlan != null ? actionPlan.name() : null);
    }
  }
}
