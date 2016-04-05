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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.db.WildcardPosition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.DatabaseUtils.buildLikeValue;
import static org.sonar.db.WildcardPosition.AFTER;

public class ComponentTreeQuery {
  @CheckForNull
  private final String nameOrKeyQuery;
  @CheckForNull
  private final Collection<String> qualifiers;
  @CheckForNull
  private final Integer page;
  @CheckForNull
  private final Integer pageSize;
  private final SnapshotDto baseSnapshot;
  private final String baseSnapshotPath;
  private final String sqlSort;
  private final String direction;

  private ComponentTreeQuery(Builder builder) {
    this.nameOrKeyQuery = builder.nameOrKeyQuery;
    this.qualifiers = builder.qualifiers;
    this.page = builder.page;
    this.pageSize = builder.pageSize;
    this.baseSnapshot = builder.baseSnapshot;
    this.baseSnapshotPath = buildBaseSnapshotPath(baseSnapshot);
    this.direction = builder.asc ? "ASC" : "DESC";
    this.sqlSort = sortFieldsToSqlSort(builder.sortFields, direction);
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

  public Integer getPage() {
    return page;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public SnapshotDto getBaseSnapshot() {
    return baseSnapshot;
  }

  public String getBaseSnapshotPath() {
    return baseSnapshotPath;
  }

  public String getSqlSort() {
    return sqlSort;
  }

  public String getDirection() {
    return direction;
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String sortFieldsToSqlSort(List<String> sortFields, String direction) {
    List<String> sqlSortFields = from(sortFields)
      .transform(new SortFieldToSqlSortFieldFunction(direction)).toList();

    return Joiner.on(", ").join(sqlSortFields);
  }

  private static String buildBaseSnapshotPath(SnapshotDto baseSnapshot) {
    String existingSnapshotPath = baseSnapshot.getPath() == null ? "" : baseSnapshot.getPath();
    return buildLikeValue(existingSnapshotPath + baseSnapshot.getId() + ".", WildcardPosition.AFTER);
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
    private SnapshotDto baseSnapshot;
    private List<String> sortFields;
    private boolean asc = true;

    private Builder() {
      // private constructor
    }

    public ComponentTreeQuery build() {
      requireNonNull(baseSnapshot);
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

    public Builder setPage(int page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder setBaseSnapshot(SnapshotDto baseSnapshot) {
      this.baseSnapshot = baseSnapshot;
      return this;
    }

    public Builder setSortFields(@Nullable List<String> sorts) {
      checkArgument(sorts != null && !sorts.isEmpty());
      this.sortFields = sorts;
      return this;
    }

    public Builder setAsc(boolean asc) {
      this.asc = asc;
      return this;
    }
  }

  private static class SortFieldToSqlSortFieldFunction implements Function<String, String> {
    private static final String PATTERN = "LOWER(p.%1$s) %2$s, p.%1$s %2$s";

    private final String direction;

    private SortFieldToSqlSortFieldFunction(String direction) {
      this.direction = direction;
    }

    @Nonnull
    @Override
    public String apply(@Nonnull String input) {
      return String.format(PATTERN, input, direction);
    }
  }
}
