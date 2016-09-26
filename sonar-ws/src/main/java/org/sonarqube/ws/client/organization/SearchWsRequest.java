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
package org.sonarqube.ws.client.organization;

public class SearchWsRequest {
  private final Integer page;
  private final Integer pageSize;

  public SearchWsRequest(Builder builder) {
    this.page = builder.page;
    this.pageSize = builder.pageSize;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public Integer getPage() {
    return page;
  }

  public static final class Builder {
    private Integer page;
    private Integer pageSize;

    public Builder setPage(Integer page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public SearchWsRequest build() {
      return new SearchWsRequest(this);
    }
  }
}
