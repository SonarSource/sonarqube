/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Rule;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class IssuesChangesNotificationSerializer {
  private static final String FIELD_ISSUES_COUNT = "issues.count";
  private static final String FIELD_CHANGE_DATE = "change.date";
  private static final String FIELD_CHANGE_AUTHOR_UUID = "change.author.uuid";
  private static final String FIELD_CHANGE_AUTHOR_LOGIN = "change.author.login";
  private static final String FIELD_CHANGE_AUTHOR_NAME = "change.author.name";

  public IssuesChangesNotification serialize(IssuesChangesNotificationBuilder builder) {
    IssuesChangesNotification res = new IssuesChangesNotification();
    serializeIssueSize(res, builder.getIssues());
    serializeChange(res, builder.getChange());
    serializeIssues(res, builder.getIssues());
    serializeRules(res, builder.getIssues());
    serializeProjects(res, builder.getIssues());

    return res;
  }

  /**
   * @throws IllegalArgumentException if {@code notification} misses any field or of any has unsupported value
   */
  public IssuesChangesNotificationBuilder from(IssuesChangesNotification notification) {
    int issueCount = readIssueCount(notification);
    IssuesChangesNotificationBuilder.Change change = readChange(notification);
    List<Issue> issues = readIssues(notification, issueCount);
    Map<String, Project> projects = readProjects(notification, issues);
    Map<RuleKey, Rule> rules = readRules(notification, issues);

    return new IssuesChangesNotificationBuilder(buildChangedIssues(issues, projects, rules), change);
  }

  private static void serializeIssueSize(IssuesChangesNotification res, Set<ChangedIssue> issues) {
    res.setFieldValue(FIELD_ISSUES_COUNT, String.valueOf(issues.size()));
  }

  private static int readIssueCount(IssuesChangesNotification notification) {
    String fieldValue = notification.getFieldValue(FIELD_ISSUES_COUNT);
    checkArgument(fieldValue != null, "missing field %s", FIELD_ISSUES_COUNT);
    int issueCount = Integer.parseInt(fieldValue);
    checkArgument(issueCount > 0, "issue count must be >= 1");
    return issueCount;
  }

  private static Set<ChangedIssue> buildChangedIssues(List<Issue> issues, Map<String, Project> projects,
    Map<RuleKey, Rule> rules) {
    return issues.stream()
      .map(issue -> new ChangedIssue.Builder(issue.key)
        .setNewStatus(issue.newStatus)
        .setNewResolution(issue.newResolution)
        .setAssignee(issue.assignee)
        .setRule(rules.get(issue.ruleKey))
        .setProject(projects.get(issue.projectUuid))
        .build())
      .collect(toSet(issues.size()));
  }

  private static void serializeIssues(IssuesChangesNotification res, Set<ChangedIssue> issues) {
    int index = 0;
    for (ChangedIssue issue : issues) {
      serializeIssue(res, index, issue);
      index++;
    }
  }

  private static List<Issue> readIssues(IssuesChangesNotification notification, int issueCount) {
    List<Issue> res = new ArrayList<>(issueCount);
    for (int i = 0; i < issueCount; i++) {
      res.add(readIssue(notification, i));
    }
    return res;
  }

  private static void serializeIssue(IssuesChangesNotification notification, int index, ChangedIssue issue) {
    String issuePropertyPrefix = "issues." + index;
    notification.setFieldValue(issuePropertyPrefix + ".key", issue.getKey());
    issue.getAssignee()
      .ifPresent(assignee -> {
        notification.setFieldValue(issuePropertyPrefix + ".assignee.uuid", assignee.getUuid());
        notification.setFieldValue(issuePropertyPrefix + ".assignee.login", assignee.getLogin());
        assignee.getName()
          .ifPresent(name -> notification.setFieldValue(issuePropertyPrefix + ".assignee.name", name));
      });
    issue.getNewResolution()
      .ifPresent(newResolution -> notification.setFieldValue(issuePropertyPrefix + ".newResolution", newResolution));
    notification.setFieldValue(issuePropertyPrefix + ".newStatus", issue.getNewStatus());
    notification.setFieldValue(issuePropertyPrefix + ".ruleKey", issue.getRule().getKey().toString());
    notification.setFieldValue(issuePropertyPrefix + ".projectUuid", issue.getProject().getUuid());
  }

  private static Issue readIssue(IssuesChangesNotification notification, int index) {
    String issuePropertyPrefix = "issues." + index;
    User assignee = readAssignee(notification, issuePropertyPrefix, index);
    return new Issue.Builder()
      .setKey(getIssueFieldValue(notification, issuePropertyPrefix + ".key", index))
      .setNewStatus(getIssueFieldValue(notification, issuePropertyPrefix + ".newStatus", index))
      .setNewResolution(notification.getFieldValue(issuePropertyPrefix + ".newResolution"))
      .setAssignee(assignee)
      .setRuleKey(getIssueFieldValue(notification, issuePropertyPrefix + ".ruleKey", index))
      .setProjectUuid(getIssueFieldValue(notification, issuePropertyPrefix + ".projectUuid", index))
      .build();
  }

  @CheckForNull
  private static User readAssignee(IssuesChangesNotification notification, String issuePropertyPrefix, int index) {
    String uuid = notification.getFieldValue(issuePropertyPrefix + ".assignee.uuid");
    if (uuid == null) {
      return null;
    }
    String login = getIssueFieldValue(notification, issuePropertyPrefix + ".assignee.login", index);
    return new User(uuid, login, notification.getFieldValue(issuePropertyPrefix + ".assignee.name"));
  }

  private static String getIssueFieldValue(IssuesChangesNotification notification, String fieldName, int index) {
    String fieldValue = notification.getFieldValue(fieldName);
    checkState(fieldValue != null, "Can not find field %s for issue with index %s", fieldName, index);
    return fieldValue;
  }

  private static void serializeRules(IssuesChangesNotification res, Set<ChangedIssue> issues) {
    issues.stream()
      .map(ChangedIssue::getRule)
      .collect(Collectors.toSet())
      .forEach(rule -> res.setFieldValue("rules." + rule.getKey(), rule.getName()));
  }

  private static Map<RuleKey, Rule> readRules(IssuesChangesNotification notification, List<Issue> issues) {
    return issues.stream()
      .map(issue -> issue.ruleKey)
      .collect(Collectors.toSet())
      .stream()
      .map(ruleKey -> readRule(notification, ruleKey))
      .collect(uniqueIndex(Rule::getKey, t -> t));
  }

  private static Rule readRule(IssuesChangesNotification notification, RuleKey ruleKey) {
    String fieldName = "rules." + ruleKey;
    String ruleName = notification.getFieldValue(fieldName);
    checkState(ruleName != null, "can not find field %s", ruleKey);
    return new Rule(ruleKey, ruleName);
  }

  private static void serializeProjects(IssuesChangesNotification res, Set<ChangedIssue> issues) {
    issues.stream()
      .map(ChangedIssue::getProject)
      .collect(Collectors.toSet())
      .forEach(project -> {
        String projectPropertyPrefix = "projects." + project.getUuid();
        res.setFieldValue(projectPropertyPrefix + ".key", project.getKey());
        res.setFieldValue(projectPropertyPrefix + ".projectName", project.getProjectName());
        project.getBranchName()
          .ifPresent(branchName -> res.setFieldValue(projectPropertyPrefix + ".branchName", branchName));
      });
  }

  private static Map<String, Project> readProjects(IssuesChangesNotification notification, List<Issue> issues) {
    return issues.stream()
      .map(issue -> issue.projectUuid)
      .collect(Collectors.toSet())
      .stream()
      .map(projectUuid -> {
        String projectPropertyPrefix = "projects." + projectUuid;
        return new Project.Builder(projectUuid)
          .setKey(getProjectFieldValue(notification, projectPropertyPrefix + ".key", projectUuid))
          .setProjectName(getProjectFieldValue(notification, projectPropertyPrefix + ".projectName", projectUuid))
          .setBranchName(notification.getFieldValue(projectPropertyPrefix + ".branchName"))
          .build();
      })
      .collect(uniqueIndex(Project::getUuid, t -> t));
  }

  private static String getProjectFieldValue(IssuesChangesNotification notification, String fieldName, String uuid) {
    String fieldValue = notification.getFieldValue(fieldName);
    checkState(fieldValue != null, "Can not find field %s for project with uuid %s", fieldName, uuid);
    return fieldValue;
  }

  private static void serializeChange(IssuesChangesNotification notification, IssuesChangesNotificationBuilder.Change change) {
    notification.setFieldValue(FIELD_CHANGE_DATE, String.valueOf(change.date));
    if (change instanceof IssuesChangesNotificationBuilder.UserChange) {
      IssuesChangesNotificationBuilder.UserChange userChange = (IssuesChangesNotificationBuilder.UserChange) change;
      User user = userChange.getUser();
      notification.setFieldValue(FIELD_CHANGE_AUTHOR_UUID, user.getUuid());
      notification.setFieldValue(FIELD_CHANGE_AUTHOR_LOGIN, user.getLogin());
      user.getName().ifPresent(name -> notification.setFieldValue(FIELD_CHANGE_AUTHOR_NAME, name));
    }
  }

  private static IssuesChangesNotificationBuilder.Change readChange(IssuesChangesNotification notification) {
    String dateFieldValue = notification.getFieldValue(FIELD_CHANGE_DATE);
    checkState(dateFieldValue != null, "Can not find field %s", FIELD_CHANGE_DATE);
    long date = Long.parseLong(dateFieldValue);

    String uuid = notification.getFieldValue(FIELD_CHANGE_AUTHOR_UUID);
    if (uuid == null) {
      return new IssuesChangesNotificationBuilder.AnalysisChange(date);
    }
    String login = notification.getFieldValue(FIELD_CHANGE_AUTHOR_LOGIN);
    checkState(login != null, "Can not find field %s", FIELD_CHANGE_AUTHOR_LOGIN);
    return new IssuesChangesNotificationBuilder.UserChange(date, new User(uuid, login, notification.getFieldValue(FIELD_CHANGE_AUTHOR_NAME)));
  }

  @Immutable
  private static final class Issue {
    private final String key;
    private final String newStatus;
    @CheckForNull
    private final String newResolution;
    @CheckForNull
    private final User assignee;
    private final RuleKey ruleKey;
    private final String projectUuid;

    private Issue(Builder builder) {
      this.key = builder.key;
      this.newResolution = builder.newResolution;
      this.newStatus = builder.newStatus;
      this.assignee = builder.assignee;
      this.ruleKey = RuleKey.parse(builder.ruleKey);
      this.projectUuid = builder.projectUuid;
    }

    static class Builder {
      private String key = null;
      private String newStatus = null;
      @CheckForNull
      private String newResolution = null;
      @CheckForNull
      private User assignee = null;
      private String ruleKey = null;
      private String projectUuid = null;

      public Builder setKey(String key) {
        this.key = key;
        return this;
      }

      public Builder setNewStatus(String newStatus) {
        this.newStatus = newStatus;
        return this;
      }

      public Builder setNewResolution(@Nullable String newResolution) {
        this.newResolution = newResolution;
        return this;
      }

      public Builder setAssignee(@Nullable User assignee) {
        this.assignee = assignee;
        return this;
      }

      public Builder setRuleKey(String ruleKey) {
        this.ruleKey = ruleKey;
        return this;
      }

      public Builder setProjectUuid(String projectUuid) {
        this.projectUuid = projectUuid;
        return this;
      }

      public Issue build() {
        return new Issue(this);
      }
    }
  }
}
