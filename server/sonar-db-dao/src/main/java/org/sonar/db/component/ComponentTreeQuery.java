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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.WildcardPosition;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.DaoDatabaseUtils.buildLikeValue;
import static org.sonar.db.WildcardPosition.BEFORE_AND_AFTER;

public class ComponentTreeQuery {

  public enum Strategy {
    CHILDREN, LEAVES
  }

  @CheckForNull
  private final String nameOrKeyQuery;
  // SONAR-7681 a public implementation of List must be used in MyBatis - potential concurrency exceptions otherwise
  @CheckForNull
  private final ArrayList<String> qualifiers;
  @CheckForNull
  private final ArrayList<String> scopes;
  private final String baseUuid;
  private final Strategy strategy;

  private ComponentTreeQuery(Builder builder) {
    this.nameOrKeyQuery = builder.nameOrKeyQuery;
    this.qualifiers = builder.qualifiers == null ? null : newArrayList(builder.qualifiers);
    this.scopes = builder.scopes == null ? null : newArrayList(builder.scopes);
    this.baseUuid = builder.baseUuid;
    this.strategy = requireNonNull(builder.strategy);
  }

  @CheckForNull
  public Collection<String> getQualifiers() {
    return qualifiers;
  }

  @CheckForNull
  public Collection<String> getScopes() {
    return scopes;
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
    return nameOrKeyQuery == null ? null : buildLikeValue(nameOrKeyQuery, BEFORE_AND_AFTER).toUpperCase(Locale.ENGLISH);
  }

  public String getBaseUuid() {
    return baseUuid;
  }

  public Strategy getStrategy() {
    return strategy;
  }

  public String getUuidPath(ComponentDto component) {
    switch (strategy) {
      case CHILDREN:
        return component.getUuidPath() + component.uuid() + ".";
      case LEAVES:
        return buildLikeValue(component.getUuidPath() + component.uuid() + ".", WildcardPosition.AFTER);
      default:
        throw new IllegalArgumentException("Unknown strategy : " + strategy);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    @CheckForNull
    private String nameOrKeyQuery;
    @CheckForNull
    private Collection<String> qualifiers;
    @CheckForNull
    private Collection<String> scopes;
    private String baseUuid;
    private Strategy strategy;

    private Builder() {
      // private constructor
    }

    public ComponentTreeQuery build() {
      requireNonNull(baseUuid);
      return new ComponentTreeQuery(this);
    }

    public Builder setNameOrKeyQuery(@Nullable String nameOrKeyQuery) {
      this.nameOrKeyQuery = nameOrKeyQuery;
      return this;
    }

    public Builder setQualifiers(Collection<String> qualifiers) {
      this.qualifiers = qualifiers;
      return this;
    }

    public Builder setScopes(Collection<String> scopes) {
      this.scopes = scopes;
      return this;
    }

    public Builder setBaseUuid(String uuid) {
      this.baseUuid = uuid;
      return this;
    }

    public Builder setStrategy(Strategy strategy) {
      this.strategy = requireNonNull(strategy);
      return this;
    }
  }
}
