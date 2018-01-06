/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.WildcardPosition;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.DaoDatabaseUtils.buildLikeValue;

public class ComponentQuery {
  private final String nameOrKeyQuery;
  private final boolean partialMatchOnKey;
  private final String[] qualifiers;
  private final String language;
  private final Boolean isPrivate;
  private final Set<Long> componentIds;
  private final Set<String> componentUuids;
  private final Set<String> componentKeys;
  private final Long analyzedBefore;
  private final boolean onProvisionedOnly;

  private ComponentQuery(Builder builder) {
    this.nameOrKeyQuery = builder.nameOrKeyQuery;
    this.partialMatchOnKey = builder.partialMatchOnKey == null ? false : builder.partialMatchOnKey;
    this.qualifiers = builder.qualifiers;
    this.language = builder.language;
    this.componentIds = builder.componentIds;
    this.componentUuids = builder.componentUuids;
    this.componentKeys = builder.componentKeys;
    this.isPrivate = builder.isPrivate;
    this.analyzedBefore = builder.analyzedBefore;
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
  public String getLanguage() {
    return language;
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
    private String language;
    private Boolean isPrivate;
    private Set<Long> componentIds;
    private Set<String> componentUuids;
    private Set<String> componentKeys;
    private Long analyzedBefore;
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

    public Builder setLanguage(@Nullable String language) {
      this.language = language;
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

    public Builder setAnalyzedBefore(@Nullable Long analyzedBefore) {
      this.analyzedBefore = analyzedBefore;
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
