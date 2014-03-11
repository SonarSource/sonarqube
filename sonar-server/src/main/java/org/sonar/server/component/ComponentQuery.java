/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.component;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @since 3.7
 */
public class ComponentQuery {

  public static final int DEFAULT_PAGE_INDEX = 1;
  public static final int DEFAULT_PAGE_SIZE = 100;
  public static final int NO_PAGINATION = -1;

  public static final String SORT_BY_NAME = "NAME";
  public static final Set<String> SORTS = ImmutableSet.of(SORT_BY_NAME);

  private final Collection<String> keys;
  private final Collection<String> names;
  private final Collection<String> qualifiers;
  private final String sort;
  private final Boolean asc;

  // max results per page
  private final int pageSize;

  // index of selected page. Start with 1.
  private final int pageIndex;

  private ComponentQuery(Builder builder) {
    this.keys = defaultCollection(builder.keys);
    this.names = defaultCollection(builder.names);
    this.qualifiers = defaultCollection(builder.qualifiers);

    this.sort = builder.sort;
    this.asc = builder.asc;
    this.pageSize = builder.pageSize;
    this.pageIndex = builder.pageIndex;
  }

  /**
   * Pattern of component keys to search. Can contain a sub part of keys.
   * Example : 'org.codehaus' will return 'org.codehaus.sonar', 'org.codehaus.tike', etc.
   */
  public Collection<String> keys() {
    return keys;
  }

  /**
   * Pattern of component name to search. Can contain a sub part of names.
   * Example : 'Sona' will return 'Sonar', 'SonarJ', etc.
   */
  public Collection<String> names() {
    return names;
  }

  /**
   * Qualifiers of components to search.
   */
  public Collection<String> qualifiers() {
    return qualifiers;
  }

  @CheckForNull
  public String sort() {
    return sort;
  }

  @CheckForNull
  public Boolean asc() {
    return asc;
  }

  public int pageSize() {
    return pageSize;
  }

  public int pageIndex() {
    return pageIndex;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Collection<String> keys;
    private Collection<String> names;
    private Collection<String> qualifiers;
    private String sort = SORT_BY_NAME;
    private Boolean asc = true;
    private Integer pageSize;
    private Integer pageIndex;

    private Builder() {
    }

    public Builder keys(@Nullable Collection<String> l) {
      this.keys = l;
      return this;
    }

    public Builder names(@Nullable Collection<String> l) {
      this.names = l;
      return this;
    }

    public Builder qualifiers(@Nullable Collection<String> l) {
      this.qualifiers = l;
      return this;
    }

    public Builder sort(@Nullable String s) {
      if (s != null && !SORTS.contains(s)) {
        throw new IllegalArgumentException("Bad sort field: " + s);
      }
      this.sort = s;
      return this;
    }

    public Builder asc(@Nullable Boolean asc) {
      this.asc = asc;
      return this;
    }

    public Builder pageSize(@Nullable Integer i) {
      this.pageSize = i;
      return this;
    }

    public Builder pageIndex(@Nullable Integer i) {
      this.pageIndex = i;
      return this;
    }

    public ComponentQuery build() {
      initPageIndex();
      initPageSize();
      return new ComponentQuery(this);
    }

    private void initPageSize() {
      if (pageSize == null) {
        pageSize = DEFAULT_PAGE_SIZE;
      }
    }

    private void initPageIndex() {
      if (pageIndex == null) {
        pageIndex = DEFAULT_PAGE_INDEX;
      }
      Preconditions.checkArgument(pageIndex > 0, "Page index must be greater than 0 (got " + pageIndex + ")");
    }
  }

  private static <T> Collection<T> defaultCollection(@Nullable Collection<T> c) {
    return c == null ? Collections.<T>emptyList() : Collections.unmodifiableCollection(c);
  }
}
