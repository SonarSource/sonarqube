/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.issue;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class IssueListQuery {

  private final String project;
  private final String branch;
  private final String pullRequest;
  private final String component;
  private final Boolean resolved;
  private final Long createdAfter;
  private final boolean newCodeOnReference;
  private final Collection<Integer> types;
  private final Collection<String> statuses;
  private final Collection<String> resolutions;
  private final Collection<String> softwareQualities;

  private IssueListQuery(IssueListQueryBuilder issueListQueryBuilder) {
    this.project = issueListQueryBuilder.project;
    this.branch = issueListQueryBuilder.branch;
    this.pullRequest = issueListQueryBuilder.pullRequest;
    this.component = issueListQueryBuilder.component;
    this.resolved = issueListQueryBuilder.resolved;
    this.createdAfter = issueListQueryBuilder.createdAfter;
    this.newCodeOnReference = issueListQueryBuilder.newCodeOnReference;
    this.types = ofNullable(issueListQueryBuilder.types)
      .map(Collections::unmodifiableCollection)
      .orElse(emptyList());
    this.statuses = ofNullable(issueListQueryBuilder.statuses)
      .map(Collections::unmodifiableCollection)
      .orElse(emptyList());
    this.resolutions = ofNullable(issueListQueryBuilder.resolutions)
      .map(Collections::unmodifiableCollection)
      .orElse(emptyList());
    this.softwareQualities = ofNullable(issueListQueryBuilder.softwareQualities)
      .map(Collections::unmodifiableCollection)
      .orElse(emptyList());
  }

  public String getProject() {
    return project;
  }

  public String getBranch() {
    return branch;
  }

  public String getPullRequest() {
    return pullRequest;
  }

  public String getComponent() {
    return component;
  }

  public Boolean getResolved() {
    return resolved;
  }

  public Long getCreatedAfter() {
    return createdAfter;
  }

  public boolean getNewCodeOnReference() {
    return newCodeOnReference;
  }

  public Collection<Integer> getTypes() {
    return types;
  }

  public Collection<String> getStatuses() {
    return statuses;
  }

  public Collection<String> getResolutions() {
    return resolutions;
  }

  public Collection<String> getSoftwareQualities() {
    return softwareQualities;
  }

  public static final class IssueListQueryBuilder {
    private String organization;
    private String project;
    private String branch;
    private String pullRequest;
    private String component;
    private Boolean resolved;
    private Long createdAfter;
    private boolean newCodeOnReference;
    private Collection<Integer> types;
    private Collection<String> statuses;
    private Collection<String> resolutions;
    private Collection<String> softwareQualities;

    private IssueListQueryBuilder() {
    }

    public static IssueListQueryBuilder newIssueListQueryBuilder() {
      return new IssueListQueryBuilder();
    }

    public IssueListQueryBuilder organization(String organization) {
      this.organization = organization;
      return this;
    }

    public IssueListQueryBuilder project(String project) {
      this.project = project;
      return this;
    }

    public IssueListQueryBuilder branch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    public IssueListQueryBuilder pullRequest(@Nullable String pullRequest) {
      this.pullRequest = pullRequest;
      return this;
    }

    public IssueListQueryBuilder component(String component) {
      this.component = component;
      return this;
    }

    public IssueListQueryBuilder resolved(Boolean resolved) {
      this.resolved = resolved;
      return this;
    }

    public IssueListQueryBuilder createdAfter(Long createdAfter) {
      this.createdAfter = createdAfter;
      return this;
    }

    public IssueListQueryBuilder newCodeOnReference(boolean newCodeOnReference) {
      this.newCodeOnReference = newCodeOnReference;
      return this;
    }

    public IssueListQueryBuilder types(Collection<Integer> types) {
      this.types = types;
      return this;
    }

    public IssueListQueryBuilder statuses(Collection<String> statuses) {
      this.statuses = statuses;
      return this;
    }

    public IssueListQueryBuilder resolutions(Collection<String> resolutions) {
      this.resolutions = resolutions;
      return this;
    }

    public IssueListQueryBuilder softwareQualities(Collection<String> softwareQualities) {
      this.softwareQualities = softwareQualities;
      return this;
    }

    public IssueListQuery build() {
      return new IssueListQuery(this);
    }
  }
}
