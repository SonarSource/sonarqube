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
package org.sonar.db.project;

import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.WildcardPosition;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.DaoUtils.buildLikeValue;

public class ProjectQuery {
  private final String nameOrKeyQuery;
  private final boolean partialMatchOnKey;
  private final Boolean isPrivate;
  private final Set<String> projectUuids;
  private final Set<String> projectKeys;
  private final Long analyzedBefore;
  private final Long anyBranchAnalyzedBefore;
  private final Long anyBranchAnalyzedAfter;
  private final Date createdAfter;
  private final boolean onProvisionedOnly;

  private ProjectQuery(ProjectQuery.Builder builder) {
    this.nameOrKeyQuery = builder.nameOrKeyQuery;
    this.partialMatchOnKey = builder.partialMatchOnKey != null && builder.partialMatchOnKey;
    this.projectUuids = builder.projectUuids;
    this.projectKeys = builder.projectKeys;
    this.isPrivate = builder.isPrivate;
    this.analyzedBefore = builder.analyzedBefore;
    this.anyBranchAnalyzedBefore = builder.anyBranchAnalyzedBefore;
    this.anyBranchAnalyzedAfter = builder.anyBranchAnalyzedAfter;
    this.createdAfter = builder.createdAfter;
    this.onProvisionedOnly = builder.onProvisionedOnly;
  }

  @CheckForNull
  public String getNameOrKeyQuery() {
    return nameOrKeyQuery;
  }

  /**
   * Used by MyBatis mapper
   */
  @CheckForNull
  public String getNameOrKeyUpperLikeQuery() {
    return buildLikeValue(nameOrKeyQuery, WildcardPosition.BEFORE_AND_AFTER).toUpperCase(Locale.ENGLISH);
  }

  /**
   * Used by MyBatis mapper
   */
  public boolean isPartialMatchOnKey() {
    return partialMatchOnKey;
  }

  @CheckForNull
  public Set<String> getProjectUuids() {
    return projectUuids;
  }

  @CheckForNull
  public Set<String> getProjectKeys() {
    return projectKeys;
  }

  @CheckForNull
  public Boolean getPrivate() {
    return isPrivate;
  }

  @CheckForNull
  public Long getAnalyzedBefore() {
    return analyzedBefore;
  }

  @CheckForNull
  public Long getAnyBranchAnalyzedBefore() {
    return anyBranchAnalyzedBefore;
  }

  @CheckForNull
  public Long getAnyBranchAnalyzedAfter() {
    return anyBranchAnalyzedAfter;
  }

  @CheckForNull
  public Date getCreatedAfter() {
    return createdAfter;
  }

  public boolean isOnProvisionedOnly() {
    return onProvisionedOnly;
  }

  boolean hasEmptySetOfProjects() {
    return Stream.of(projectKeys, projectUuids)
      .anyMatch(list -> list != null && list.isEmpty());
  }

  public static ProjectQuery.Builder builder() {
    return new ProjectQuery.Builder();
  }

  public static class Builder {
    private String nameOrKeyQuery;
    private Boolean partialMatchOnKey;
    private Boolean isPrivate;
    private Set<String> projectUuids;
    private Set<String> projectKeys;
    private Set<String> qualifiers;
    private Long analyzedBefore;
    private Long anyBranchAnalyzedBefore;
    private Long anyBranchAnalyzedAfter;
    private Date createdAfter;
    private boolean onProvisionedOnly = false;

    public ProjectQuery.Builder setNameOrKeyQuery(@Nullable String nameOrKeyQuery) {
      this.nameOrKeyQuery = nameOrKeyQuery;
      return this;
    }

    /**
     * Beware, can be resource intensive! Should be used with precautions.
     */
    public ProjectQuery.Builder setPartialMatchOnKey(@Nullable Boolean partialMatchOnKey) {
      this.partialMatchOnKey = partialMatchOnKey;
      return this;
    }

    public ProjectQuery.Builder setProjectUuids(@Nullable Set<String> projectUuids) {
      this.projectUuids = projectUuids;
      return this;
    }

    public ProjectQuery.Builder setProjectKeys(@Nullable Set<String> projectKeys) {
      this.projectKeys = projectKeys;
      return this;
    }

    public Set<String> getQualifiers() {
      return qualifiers;
    }

    public ProjectQuery.Builder setQualifiers(Set<String> qualifiers) {
      this.qualifiers = qualifiers;
      return this;
    }

    public ProjectQuery.Builder setPrivate(@Nullable Boolean isPrivate) {
      this.isPrivate = isPrivate;
      return this;
    }

    public ProjectQuery.Builder setAnalyzedBefore(@Nullable Long l) {
      this.analyzedBefore = l;
      return this;
    }

    public ProjectQuery.Builder setAnyBranchAnalyzedBefore(@Nullable Long l) {
      this.anyBranchAnalyzedBefore = l;
      return this;
    }

    /**
     * Filter on date of last analysis. On projects, all branches and pull requests are taken into
     * account. For example the analysis of a branch is included in the filter
     * even if the main branch has never been analyzed.
     */
    public ProjectQuery.Builder setAnyBranchAnalyzedAfter(@Nullable Long l) {
      this.anyBranchAnalyzedAfter = l;
      return this;
    }

    public ProjectQuery.Builder setCreatedAfter(@Nullable Date l) {
      this.createdAfter = l;
      return this;
    }

    public ProjectQuery.Builder setOnProvisionedOnly(boolean onProvisionedOnly) {
      this.onProvisionedOnly = onProvisionedOnly;
      return this;
    }

    public ProjectQuery build() {
      checkArgument(nameOrKeyQuery != null || partialMatchOnKey == null, "A query must be provided if a partial match on key is specified.");
      return new ProjectQuery(this);
    }
  }
}
