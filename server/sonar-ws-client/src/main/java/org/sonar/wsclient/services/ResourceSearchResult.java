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
package org.sonar.wsclient.services;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

public class ResourceSearchResult extends Model {

  public static class Resource {
    private String key, name, qualifier;

    @CheckForNull
    public String key() {
      return key;
    }

    @CheckForNull
    public String name() {
      return name;
    }

    @CheckForNull
    public String qualifier() {
      return qualifier;
    }

    public void setKey(@Nullable String key) {
      this.key = key;
    }

    public void setName(@Nullable String s) {
      this.name = s;
    }

    public void setQualifier(@Nullable String qualifier) {
      this.qualifier = qualifier;
    }
  }


  private Integer page, pageSize, total;
  private List<ResourceSearchResult.Resource> resources;

  @CheckForNull
  public Integer getPage() {
    return page;
  }

  @CheckForNull
  public Integer getTotal() {
    return total;
  }

  public List<ResourceSearchResult.Resource> getResources() {
    return resources;
  }

  public void setPage(@Nullable Integer page) {
    this.page = page;
  }

  public void setTotal(@Nullable Integer total) {
    this.total = total;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(@Nullable Integer pageSize) {
    this.pageSize = pageSize;
  }

  public void setResources(List<Resource> resources) {
    this.resources = resources;
  }
}
