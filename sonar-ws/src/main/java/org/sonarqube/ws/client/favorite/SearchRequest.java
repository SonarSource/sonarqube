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
package org.sonarqube.ws.client.favorite;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class SearchRequest {
  public static final int MAX_PAGE_SIZE = 500;

  private Integer page;
  private Integer pageSize;

  @CheckForNull
  public Integer getPage() {
    return page;
  }

  public SearchRequest setPage(@Nullable Integer page) {
    this.page = page;
    return this;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  public SearchRequest setPageSize(@Nullable Integer pageSize) {
    if (pageSize != null && pageSize > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("Page size must be lower than or equals to " + MAX_PAGE_SIZE);
    }
    this.pageSize = pageSize;
    return this;
  }
}
