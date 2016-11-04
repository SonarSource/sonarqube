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
package org.sonar.db.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.db.WildcardPosition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.DatabaseUtils.buildLikeValue;
import static org.sonar.db.WildcardPosition.AFTER;

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
  private final Integer page;
  @CheckForNull
  private final Integer pageSize;
  private final String baseUuid;
  private final String sqlSort;
  private final String direction;
  private final Strategy strategy;

  private ComponentTreeQuery(Builder builder) {
    this.nameOrKeyQuery = builder.nameOrKeyQuery;
    this.qualifiers = builder.qualifiers == null ? null : newArrayList(builder.qualifiers);
    this.page = builder.page;
    this.pageSize = builder.pageSize;
    this.baseUuid = builder.baseUuid;
    this.strategy = requireNonNull(builder.strategy);
    this.direction = builder.asc ? "ASC" : "DESC";
    this.sqlSort = builder.sortFields != null ? sortFieldsToSqlSort(builder.sortFields, direction) : null;
  }

  public Collection<String> getQualifiers() {
    return qualifiers;
  }

  @CheckForNull
  public String getNameOrKeyQuery() {
    return nameOrKeyQuery;
  }

  @CheckForNull
  public String getNameOrKeyQueryToSqlForResourceIndex() {
    return nameOrKeyQuery == null ? null : buildLikeValue(nameOrKeyQuery, AFTER).toLowerCase(Locale.ENGLISH);
  }

  @Deprecated
  public Integer getPage() {
    return page;
  }

  @Deprecated
  public Integer getPageSize() {
    return pageSize;
  }

  public String getBaseUuid() {
    return baseUuid;
  }

  public Strategy getStrategy() {
    return strategy;
  }

  @Deprecated
  public String getSqlSort() {
    return sqlSort;
  }

  @Deprecated
  public String getDirection() {
    return direction;
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

  @Deprecated
  private static String sortFieldsToSqlSort(List<String> sortFields, String direction) {
    return sortFields
      .stream()
      .map(new SortFieldToSqlSortFieldFunction(direction)::apply)
      .collect(Collectors.joining(", "));
  }

  public static class Builder {
    @CheckForNull
    private String nameOrKeyQuery;
    @CheckForNull
    private Collection<String> qualifiers;
    @CheckForNull
    private Integer page;
    @CheckForNull
    private Integer pageSize;
    private String baseUuid;
    private List<String> sortFields;
    private boolean asc = true;
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

    @Deprecated
    public Builder setPage(int page) {
      this.page = page;
      return this;
    }

    @Deprecated
    public Builder setPageSize(int pageSize) {
      this.pageSize = pageSize;
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

    @Deprecated
    public Builder setSortFields(List<String> sorts) {
      checkArgument(sorts != null && !sorts.isEmpty());
      this.sortFields = sorts;
      return this;
    }

    @Deprecated
    public Builder setAsc(boolean asc) {
      this.asc = asc;
      return this;
    }
  }

  @Deprecated
  private static class SortFieldToSqlSortFieldFunction implements Function<String, String> {
    private static final String PATTERN = "LOWER(p.%1$s) %2$s, p.%1$s %2$s";

    private final String direction;

    private SortFieldToSqlSortFieldFunction(String direction) {
      this.direction = direction;
    }

    @Nonnull
    @Override
    public String apply(@Nonnull String input) {
      return format(PATTERN, input, direction);
    }
  }
}
