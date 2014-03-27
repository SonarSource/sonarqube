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

/**
 * @since 4.3
 */
public class RuleQuery {

  public static final int DEFAULT_PAGE_INDEX = 1;
  private static final int DEFAULT_PAGE_SIZE = 25;

  private String key;
  private String query;
  private String characteristicKey;
  private String subCharacteristicKey;

  private int pageSize;
  private int pageIndex;

  private RuleQuery(Builder builder) {
    this.key = builder.key;
    this.query = builder.query;
    this.subCharacteristicKey = builder.subCharacteristicKey;
    this.characteristicKey = builder.characteristicKey;
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
  public String characteristicKey() {
    return characteristicKey;
  }

  @CheckForNull
  public String subCharacteristicKey() {
    return subCharacteristicKey;
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
    private String characteristicKey;
    private String subCharacteristicKey;

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

    public Builder characteristicKey(@Nullable String characteristicKey) {
      this.characteristicKey = characteristicKey;
      return this;
    }

    public Builder subCharacteristicKey(@Nullable String subCharacteristicKey) {
      this.subCharacteristicKey = subCharacteristicKey;
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
}
