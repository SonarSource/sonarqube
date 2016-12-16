/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonarqube.ws.client.issue;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;

public class BulkChangeRequest {

  private final List<String> issues;
  private final String assign;
  private final String setSeverity;
  private final String setType;
  private final String doTransition;
  private final List<String> addTags;
  private final List<String> removeTags;
  private final String comment;
  private final Boolean sendNotifications;

  private BulkChangeRequest(Builder builder) {
    this.issues = builder.issues;
    this.assign = builder.assign;
    this.setSeverity = builder.setSeverity;
    this.setType = builder.setType;
    this.doTransition = builder.doTransition;
    this.addTags = builder.addTags;
    this.removeTags = builder.removeTags;
    this.comment = builder.comment;
    this.sendNotifications = builder.sendNotifications;
  }

  public List<String> getIssues() {
    return issues;
  }

  @CheckForNull
  public String getAssign() {
    return assign;
  }

  @CheckForNull
  public String getSetSeverity() {
    return setSeverity;
  }

  @CheckForNull
  public String getSetType() {
    return setType;
  }

  @CheckForNull
  public String getDoTransition() {
    return doTransition;
  }

  public List<String> getAddTags() {
    return addTags;
  }

  public List<String> getRemoveTags() {
    return removeTags;
  }

  @CheckForNull
  public String getComment() {
    return comment;
  }

  @CheckForNull
  public Boolean getSendNotifications() {
    return sendNotifications;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<String> issues;
    private String assign;
    private String setSeverity;
    private String setType;
    private String doTransition;
    private List<String> addTags = newArrayList();
    private List<String> removeTags = newArrayList();
    private String comment;
    private Boolean sendNotifications;

    public Builder setIssues(List<String> issues) {
      this.issues = issues;
      return this;
    }

    public Builder setAssign(@Nullable String assign) {
      this.assign = assign;
      return this;
    }

    public Builder setSetSeverity(@Nullable String setSeverity) {
      this.setSeverity = setSeverity;
      return this;
    }

    public Builder setSetType(@Nullable String setType) {
      this.setType = setType;
      return this;
    }

    public Builder setDoTransition(@Nullable String doTransition) {
      this.doTransition = doTransition;
      return this;
    }

    public Builder setAddTags(List<String> addTags) {
      this.addTags = requireNonNull(addTags);
      return this;
    }

    public Builder setRemoveTags(List<String> removeTags) {
      this.removeTags = requireNonNull(removeTags);
      return this;
    }

    public Builder setComment(@Nullable String comment) {
      this.comment = comment;
      return this;
    }

    public Builder setSendNotifications(@Nullable Boolean sendNotifications) {
      this.sendNotifications = sendNotifications;
      return this;
    }

    public BulkChangeRequest build() {
      checkArgument(issues != null && !issues.isEmpty(), "Issue keys must be provided");
      return new BulkChangeRequest(this);
    }
  }
}
