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
package org.sonar.db.measure;

import java.util.Collection;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.WildcardPosition;
import org.sonar.db.component.ComponentDto;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.DaoUtils.buildLikeValue;
import static org.sonar.db.WildcardPosition.BEFORE_AND_AFTER;

public class MeasureTreeQuery {

  public enum Strategy {
    CHILDREN, LEAVES
  }

  @CheckForNull
  private final String nameOrKeyQuery;
  // SONAR-7681 a public implementation of List must be used in MyBatis - potential concurrency exceptions otherwise
  @CheckForNull
  private final Collection<String> qualifiers;
  private final Strategy strategy;

  @CheckForNull
  private final Collection<Integer> metricIds;

  private MeasureTreeQuery(Builder builder) {
    this.nameOrKeyQuery = builder.nameOrKeyQuery;
    this.qualifiers = builder.qualifiers == null ? null : newArrayList(builder.qualifiers);
    this.strategy = requireNonNull(builder.strategy);
    this.metricIds = builder.metricIds;
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

  @CheckForNull
  public Collection<String> getQualifiers() {
    return qualifiers;
  }

  public Strategy getStrategy() {
    return strategy;
  }

  @CheckForNull
  public Collection<Integer> getMetricIds() {
    return metricIds;
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

  public boolean returnsEmpty() {
    return (metricIds != null && metricIds.isEmpty()) || (qualifiers != null && qualifiers.isEmpty());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    @CheckForNull
    private String nameOrKeyQuery;
    @CheckForNull
    private Collection<String> qualifiers;
    private Strategy strategy;

    @CheckForNull
    private Collection<Integer> metricIds;

    private Builder() {
    }

    public Builder setNameOrKeyQuery(@Nullable String nameOrKeyQuery) {
      this.nameOrKeyQuery = nameOrKeyQuery;
      return this;
    }

    public Builder setQualifiers(Collection<String> qualifiers) {
      this.qualifiers = qualifiers;
      return this;
    }

    public Builder setStrategy(Strategy strategy) {
      this.strategy = requireNonNull(strategy);
      return this;
    }

    /**
     * All the measures are returned if parameter is {@code null}.
     */
    public Builder setMetricIds(@Nullable Collection<Integer> metricIds) {
      this.metricIds = metricIds;
      return this;
    }

    public MeasureTreeQuery build() {
      return new MeasureTreeQuery(this);
    }
  }
}
