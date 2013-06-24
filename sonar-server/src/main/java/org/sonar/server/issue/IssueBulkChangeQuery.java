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

import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.7
 */
public class IssueBulkChangeQuery {

  private static final String ASSIGNEE = "assign";
  private static final String PLAN = "plan";
  private static final String SEVERITY = "set_severity";
  private static final String TRANSITION = "do_transition";
  private static final String COMMENT = "comment";

  private final Collection<String> actions;
  private final Collection<String> issueKeys;
  private final String assignee;
  private final String plan;
  private final String severity;
  private final String transition;
  private final String comment;

  private IssueBulkChangeQuery(Builder builder) {
    this.actions = defaultCollection(builder.actions);
    this.issueKeys = defaultCollection(builder.issueKeys);
    this.assignee = builder.assignee;
    this.plan = builder.plan;
    this.severity = builder.severity;
    this.transition = builder.transition;
    this.comment = builder.comment;
  }

  public Collection<String> issueKeys() {
    return issueKeys;
  }

  @CheckForNull
  public String assignee() {
    return assignee;
  }

  @CheckForNull
  public String plan() {
    return plan;
  }

  @CheckForNull
  public String severity() {
    return severity;
  }

  @CheckForNull
  public String transition() {
    return transition;
  }

  @CheckForNull
  public String comment() {
    return comment;
  }

  public boolean isOnAssignee() {
    return actions.contains(ASSIGNEE);
  }

  public boolean isOnActionPlan() {
    return actions.contains(PLAN);
  }

  public boolean isOnSeverity() {
    return actions.contains(SEVERITY);
  }

  public boolean isOnTransition() {
    return actions.contains(TRANSITION);
  }

  public boolean isOnComment() {
    return actions.contains(COMMENT);
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Collection<String> actions = newArrayList();
    private Collection<String> issueKeys;
    private String assignee;
    private String plan;
    private String severity;
    private String transition;
    private String comment;

    private Builder() {
    }

    public Builder issueKeys(@Nullable Collection<String> l) {
      this.issueKeys = l;
      return this;
    }

    public Builder assignee(@Nullable String assignee) {
      actions.add(IssueBulkChangeQuery.ASSIGNEE);
      this.assignee = assignee;
      return this;
    }

    public Builder plan(@Nullable String plan) {
      actions.add(IssueBulkChangeQuery.PLAN);
      this.plan = plan;
      return this;
    }

    public Builder severity(@Nullable String severity) {
      actions.add(IssueBulkChangeQuery.SEVERITY);
      this.severity = severity;
      return this;
    }

    public Builder transition(@Nullable String transition) {
      actions.add(IssueBulkChangeQuery.TRANSITION);
      this.transition = transition;
      return this;
    }

    public Builder comment(@Nullable String comment) {
      actions.add(IssueBulkChangeQuery.COMMENT);
      this.comment = comment;
      return this;
    }

    public IssueBulkChangeQuery build() {
      Preconditions.checkArgument(issueKeys != null && !issueKeys.isEmpty(), "Issues must not be empty");
      Preconditions.checkArgument(!actions.isEmpty(), "At least one action must be provided");

      return new IssueBulkChangeQuery(this);
    }
  }

  private static <T> Collection<T> defaultCollection(@Nullable Collection<T> c) {
    return c == null ? Collections.<T>emptyList() : Collections.unmodifiableCollection(c);
  }

}
