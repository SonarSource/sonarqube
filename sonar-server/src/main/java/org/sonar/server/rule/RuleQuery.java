/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.sonar.server.paging.Paging;

import javax.annotation.Nullable;

/**
 * @since 4.3
 */
public class RuleQuery {

  private static final int DEFAULT_PAGE_SIZE = 25;

  private String key;

  private String query;

  private Paging paging;

  private RuleQuery(@Nullable String key, @Nullable String query, Paging paging) {
    this.key = key;
    this.query = query;
    this.paging = paging;
  }

  public String key() {
    return this.key;
  }

  public String query() {
    return this.query;
  }

  public Paging paging() {
    return this.paging;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String key;

    private String query;

    private int pageSize;

    private int page;

    private Builder() {
      this.page = 0;
      this.pageSize = DEFAULT_PAGE_SIZE;
    }

    public Builder withKey(String key) {
      this.key = key;
      return this;
    }

    public Builder withSearchQuery(String query) {
      this.query = query;
      return this;
    }

    public Builder withPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder withPage(int page) {
      this.page = page;
      return this;
    }

    public RuleQuery build() {
      return new RuleQuery(key, query, Paging.create(pageSize, page));
    }
  }
}
