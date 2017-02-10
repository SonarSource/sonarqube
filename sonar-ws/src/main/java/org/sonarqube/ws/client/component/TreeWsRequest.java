/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.component;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class TreeWsRequest {
  @CheckForNull
  private String baseComponentId;
  @CheckForNull
  private String baseComponentKey;
  @CheckForNull
  private String strategy;
  @CheckForNull
  private List<String> qualifiers;
  @CheckForNull
  private String query;
  @CheckForNull
  private List<String> sort;
  @CheckForNull
  private Boolean asc;
  @CheckForNull
  private Integer page;
  @CheckForNull
  private Integer pageSize;

  public String getBaseComponentId() {
    return baseComponentId;
  }

  public TreeWsRequest setBaseComponentId(@Nullable String baseComponentId) {
    this.baseComponentId = baseComponentId;
    return this;
  }

  public String getBaseComponentKey() {
    return baseComponentKey;
  }

  public TreeWsRequest setBaseComponentKey(@Nullable String baseComponentKey) {
    this.baseComponentKey = baseComponentKey;
    return this;
  }

  public String getStrategy() {
    return strategy;
  }

  public TreeWsRequest setStrategy(@Nullable String strategy) {
    this.strategy = strategy;
    return this;
  }

  public List<String> getQualifiers() {
    return qualifiers;
  }

  public TreeWsRequest setQualifiers(@Nullable List<String> qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public String getQuery() {
    return query;
  }

  public TreeWsRequest setQuery(@Nullable String query) {
    this.query = query;
    return this;
  }

  public List<String> getSort() {
    return sort;
  }

  public TreeWsRequest setSort(@Nullable List<String> sort) {
    this.sort = sort;
    return this;
  }

  public Boolean getAsc() {
    return asc;
  }

  public TreeWsRequest setAsc(boolean asc) {
    this.asc = asc;
    return this;
  }

  public Integer getPage() {
    return page;
  }

  public TreeWsRequest setPage(int page) {
    this.page = page;
    return this;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public TreeWsRequest setPageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }
}
