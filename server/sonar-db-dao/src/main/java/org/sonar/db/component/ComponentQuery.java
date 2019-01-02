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
package org.sonar.db.component;

import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.WildcardPosition;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.DaoUtils.buildLikeValue;

public class ComponentQuery {
  private final String nameOrKeyQuery;
  private final boolean partialMatchOnKey;
  private final String[] qualifiers;
  private final Boolean isPrivate;
  private final Set<Long> componentIds;
  private final Set<String> componentUuids;
  private final Set<String> componentKeys;
  private final Long analyzedBefore;
  private final Long anyBranchAnalyzedBefore;
  private final Long anyBranchAnalyzedAfter;
  private final Date createdAfter;
  private final boolean onProvisionedOnly;

  private ComponentQuery(Builder builder) {
    this.nameOrKeyQuery = builder.nameOrKeyQuery;
    this.partialMatchOnKey = builder.partialMatchOnKey == null ? false : builder.partialMatchOnKey;
    this.qualifiers = builder.qualifiers;
    this.componentIds = builder.componentIds;
    this.componentUuids = builder.componentUuids;
    this.componentKeys = builder.componentKeys;
    this.isPrivate = builder.isPrivate;
    this.analyzedBefore = builder.analyzedBefore;
    this.anyBranchAnalyzedBefore = builder.anyBranchAnalyzedBefore;
    this.anyBranchAnalyzedAfter = builder.anyBranchAnalyzedAfter;
    this.createdAfter = builder.createdAfter;
    this.onProvisionedOnly = builder.onProvisionedOnly;
  }

  public String[] getQualifiers() {
    return qualifiers;
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
  public Set<Long> getComponentIds() {
    return componentIds;
  }

  @CheckForNull
  public Set<String> getComponentUuids() {
    return componentUuids;
  }

  @CheckForNull
  public Set<String> getComponentKeys() {
    return componentKeys;
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

  boolean hasEmptySetOfComponents() {
    return Stream.of(componentIds, componentKeys, componentUuids)
      .anyMatch(list -> list != null && list.isEmpty());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String nameOrKeyQuery;
    private Boolean partialMatchOnKey;
    private String[] qualifiers;
    private Boolean isPrivate;
    private Set<Long> componentIds;
    private Set<String> componentUuids;
    private Set<String> componentKeys;
    private Long analyzedBefore;
    private Long anyBranchAnalyzedBefore;
    private Long anyBranchAnalyzedAfter;
    private Date createdAfter;
    private boolean onProvisionedOnly = false;

    public Builder setNameOrKeyQuery(@Nullable String nameOrKeyQuery) {
      this.nameOrKeyQuery = nameOrKeyQuery;
      return this;
    }

    /**
     * Beware, can be resource intensive! Should be used with precautions.
     */
    public Builder setPartialMatchOnKey(@Nullable Boolean partialMatchOnKey) {
      this.partialMatchOnKey = partialMatchOnKey;
      return this;
    }

    public Builder setQualifiers(String... qualifiers) {
      this.qualifiers = qualifiers;
      return this;
    }

    public Builder setComponentIds(@Nullable Set<Long> componentIds) {
      this.componentIds = componentIds;
      return this;
    }

    public Builder setComponentUuids(@Nullable Set<String> componentUuids) {
      this.componentUuids = componentUuids;
      return this;
    }

    public Builder setComponentKeys(@Nullable Set<String> componentKeys) {
      this.componentKeys = componentKeys;
      return this;
    }

    public Builder setPrivate(@Nullable Boolean isPrivate) {
      this.isPrivate = isPrivate;
      return this;
    }

    public Builder setAnalyzedBefore(@Nullable Long l) {
      this.analyzedBefore = l;
      return this;
    }

    public Builder setAnyBranchAnalyzedBefore(@Nullable Long l) {
      this.anyBranchAnalyzedBefore = l;
      return this;
    }

    /**
     * Filter on date of last analysis. On projects, all branches and pull requests are taken into
     * account. For example the analysis of a short-lived branch is included in the filter
     * even if the main branch has never been analyzed.
     */
    public Builder setAnyBranchAnalyzedAfter(@Nullable Long l) {
      this.anyBranchAnalyzedAfter = l;
      return this;
    }

    public Builder setCreatedAfter(@Nullable Date l) {
      this.createdAfter = l;
      return this;
    }

    public Builder setOnProvisionedOnly(boolean onProvisionedOnly) {
      this.onProvisionedOnly = onProvisionedOnly;
      return this;
    }

    public ComponentQuery build() {
      checkArgument(qualifiers != null && qualifiers.length > 0, "At least one qualifier must be provided");
      checkArgument(nameOrKeyQuery != null || partialMatchOnKey == null, "A query must be provided if a partial match on key is specified.");

      return new ComponentQuery(this);
    }
  }
}
