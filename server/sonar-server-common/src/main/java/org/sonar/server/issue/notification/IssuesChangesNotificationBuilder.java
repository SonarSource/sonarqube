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

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

@Immutable
public class IssuesChangesNotificationBuilder {

  private static final String KEY_CANT_BE_NULL_MESSAGE = "key can't be null";
  private final Set<ChangedIssue> issues;
  private final Change change;

  public IssuesChangesNotificationBuilder(Set<ChangedIssue> issues, Change change) {
    checkArgument(!issues.isEmpty(), "issues can't be empty");

    this.issues = ImmutableSet.copyOf(issues);
    this.change = requireNonNull(change, "change can't be null");
  }

  public Set<ChangedIssue> getIssues() {
    return issues;
  }

  public Change getChange() {
    return change;
  }

  @Immutable
  public static final class ChangedIssue {
    private final String key;
    private final String newStatus;
    @CheckForNull
    private final String newResolution;
    @CheckForNull
    private final User assignee;
    private final Rule rule;
    private final Project project;

    public ChangedIssue(Builder builder) {
      this.key = requireNonNull(builder.key, KEY_CANT_BE_NULL_MESSAGE);
      this.newStatus = requireNonNull(builder.newStatus, "newStatus can't be null");
      this.newResolution = builder.newResolution;
      this.assignee = builder.assignee;
      this.rule = requireNonNull(builder.rule, "rule can't be null");
      this.project = requireNonNull(builder.project, "project can't be null");
    }

    public String getKey() {
      return key;
    }

    public String getNewStatus() {
      return newStatus;
    }

    public Optional<String> getNewResolution() {
      return ofNullable(newResolution);
    }

    public Optional<User> getAssignee() {
      return ofNullable(assignee);
    }

    public Rule getRule() {
      return rule;
    }

    public Project getProject() {
      return project;
    }

    public static class Builder {
      private final String key;
      private String newStatus;
      @CheckForNull
      private String newResolution;
      @CheckForNull
      private User assignee;
      private Rule rule;
      private Project project;

      public Builder(String key) {
        this.key = key;
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

      public Builder setRule(Rule rule) {
        this.rule = rule;
        return this;
      }

      public Builder setProject(Project project) {
        this.project = project;
        return this;
      }

      public ChangedIssue build() {
        return new ChangedIssue(this);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ChangedIssue that = (ChangedIssue) o;
      return key.equals(that.key) &&
        newStatus.equals(that.newStatus) &&
        Objects.equals(newResolution, that.newResolution) &&
        Objects.equals(assignee, that.assignee) &&
        rule.equals(that.rule) &&
        project.equals(that.project);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, newStatus, newResolution, assignee, rule, project);
    }

    @Override
    public String toString() {
      return "ChangedIssue{" +
        "key='" + key + '\'' +
        ", newStatus='" + newStatus + '\'' +
        ", newResolution='" + newResolution + '\'' +
        ", assignee=" + assignee +
        ", rule=" + rule +
        ", project=" + project +
        '}';
    }
  }

  public static final class User {
    private final String uuid;
    private final String login;
    @CheckForNull
    private final String name;

    public User(String uuid, String login, @Nullable String name) {
      this.uuid = requireNonNull(uuid, "uuid can't be null");
      this.login = requireNonNull(login, "login can't be null");
      this.name = name;
    }

    public String getUuid() {
      return uuid;
    }

    public String getLogin() {
      return login;
    }

    public Optional<String> getName() {
      return ofNullable(name);
    }

    @Override
    public String toString() {
      return "User{" +
        "uuid='" + uuid + '\'' +
        ", login='" + login + '\'' +
        ", name='" + name + '\'' +
        '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      User user = (User) o;
      return uuid.equals(user.uuid) &&
        login.equals(user.login) &&
        Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(uuid, login, name);
    }
  }

  @Immutable
  public static final class Rule {
    private final RuleKey key;
    private final String name;

    public Rule(RuleKey key, String name) {
      this.key = requireNonNull(key, KEY_CANT_BE_NULL_MESSAGE);
      this.name = requireNonNull(name, "name can't be null");
    }

    public RuleKey getKey() {
      return key;
    }

    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Rule that = (Rule) o;
      return key.equals(that.key) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, name);
    }

    @Override
    public String toString() {
      return "Rule{" +
        "key=" + key +
        ", name='" + name + '\'' +
        '}';
    }
  }

  @Immutable
  public static final class Project {
    private final String uuid;
    private final String key;
    private final String projectName;
    @Nullable
    private final String branchName;

    public Project(Builder builder) {
      this.uuid = requireNonNull(builder.uuid, "uuid can't be null");
      this.key = requireNonNull(builder.key, KEY_CANT_BE_NULL_MESSAGE);
      this.projectName = requireNonNull(builder.projectName, "projectName can't be null");
      this.branchName = builder.branchName;
    }

    public String getUuid() {
      return uuid;
    }

    public String getKey() {
      return key;
    }

    public String getProjectName() {
      return projectName;
    }

    public Optional<String> getBranchName() {
      return ofNullable(branchName);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Project project = (Project) o;
      return uuid.equals(project.uuid) &&
        key.equals(project.key) &&
        projectName.equals(project.projectName) &&
        Objects.equals(branchName, project.branchName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(uuid, key, projectName, branchName);
    }

    @Override
    public String toString() {
      return "Project{" +
        "uuid='" + uuid + '\'' +
        ", key='" + key + '\'' +
        ", projectName='" + projectName + '\'' +
        ", branchName='" + branchName + '\'' +
        '}';
    }

    public static class Builder {
      private final String uuid;
      private String key;
      private String projectName;
      @CheckForNull
      private String branchName;

      public Builder(String uuid) {
        this.uuid = uuid;
      }

      public Builder setKey(String key) {
        this.key = key;
        return this;
      }

      public Builder setProjectName(String projectName) {
        this.projectName = projectName;
        return this;
      }

      public Builder setBranchName(@Nullable String branchName) {
        this.branchName = branchName;
        return this;
      }

      public Project build() {
        return new Project(this);
      }
    }
  }

  public abstract static class Change {
    protected final long date;

    private Change(long date) {
      this.date = requireNonNull(date, "date can't be null");
    }

    public long getDate() {
      return date;
    }

    public abstract boolean isAuthorLogin(String login);
  }

  @Immutable
  public static final class AnalysisChange extends Change {
    public AnalysisChange(long date) {
      super(date);
    }

    @Override
    public boolean isAuthorLogin(String login) {
      return false;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Change change = (Change) o;
      return date == change.date;
    }

    @Override
    public int hashCode() {
      return Objects.hash(date);
    }

    @Override
    public String toString() {
      return "AnalysisChange{" + date + '}';
    }
  }

  @Immutable
  public static final class UserChange extends Change {
    private final User user;

    public UserChange(long date, User user) {
      super(date);
      this.user = requireNonNull(user, "user can't be null");
    }

    public User getUser() {
      return user;
    }

    @Override
    public boolean isAuthorLogin(String login) {
      return this.user.login.equals(login);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      UserChange that = (UserChange) o;
      return date == that.date && user.equals(that.user);
    }

    @Override
    public int hashCode() {
      return Objects.hash(user, date);
    }

    @Override
    public String toString() {
      return "UserChange{" +
        "date=" + date +
        ", user=" + user +
        '}';
    }
  }
}
