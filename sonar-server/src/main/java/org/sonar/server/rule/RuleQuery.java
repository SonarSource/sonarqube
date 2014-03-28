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
package org.sonar.server.rule;

import com.google.common.base.Preconditions;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @since 4.3
 */
public class RuleQuery {

  public static final int DEFAULT_PAGE_INDEX = 1;
  private static final int DEFAULT_PAGE_SIZE = 25;

  private String key;
  private String query;
  private String characteristic;
  private String language;
  private Collection<String> repositories;

  private int pageSize;
  private int pageIndex;

  private RuleQuery(Builder builder) {
    this.key = builder.key;
    this.query = builder.query;
    this.language = builder.language;
    this.repositories = defaultCollection(builder.repositories);
    this.characteristic = builder.characteristic;
    this.pageSize = builder.pageSize;
    this.pageIndex = builder.pageIndex;
  }

  @CheckForNull
  public String key() {
    return key;
  }

  @CheckForNull
  public String query() {
    return query;
  }

  @CheckForNull
  public String language() {
    return language;
  }

  public Collection<String> repositories() {
    return repositories;
  }

  @CheckForNull
  public String characteristic() {
    return characteristic;
  }

  public int pageSize() {
    return pageSize;
  }

  public int pageIndex() {
    return pageIndex;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String key;
    private String query;
    private String characteristic;
    private String language;
    private Collection<String> repositories;

    private Integer pageSize;
    private Integer pageIndex;

    public Builder key(@Nullable String key) {
      this.key = key;
      return this;
    }

    public Builder searchQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder language(@Nullable String language) {
      this.language = language;
      return this;
    }

    public Builder repositories(Collection<String> repositories) {
      this.repositories = repositories;
      return this;
    }

    public Builder characteristic(@Nullable String characteristic) {
      this.characteristic = characteristic;
      return this;
    }

    public Builder pageSize(@Nullable Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder pageIndex(@Nullable Integer page) {
      this.pageIndex = page;
      return this;
    }

    public RuleQuery build() {
      initPageSize();
      initPageIndex();
      return new RuleQuery(this);
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
