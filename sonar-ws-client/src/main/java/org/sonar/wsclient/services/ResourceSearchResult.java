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
package org.sonar.wsclient.services;

import java.util.List;

public class ResourceSearchResult extends Model {

  public static class Resource {
    private String key, name, qualifier;

    public String key() {
      return key;
    }

    public String name() {
      return name;
    }

    public String qualifier() {
      return qualifier;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public void setName(String s) {
      this.name = s;
    }

    public void setQualifier(String qualifier) {
      this.qualifier = qualifier;
    }
  }


  private int page, pageSize, total;
  private List<ResourceSearchResult.Resource> resources;

  public int getPage() {
    return page;
  }

  public int getTotal() {
    return total;
  }

  public List<ResourceSearchResult.Resource> getResources() {
    return resources;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public void setResources(List<Resource> resources) {
    this.resources = resources;
  }
}
