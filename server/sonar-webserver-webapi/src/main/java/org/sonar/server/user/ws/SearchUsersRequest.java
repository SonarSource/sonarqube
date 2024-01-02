/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.user.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public abstract class SearchUsersRequest {
  protected String selected;
  protected String query;
  protected Integer page;
  protected Integer pageSize;

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public String getSelected() {
    return selected;
  }

  public Integer getPage() {
    return page;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public abstract static class Builder<T extends Builder<T>> {
    private String selected;
    private String query;
    private Integer page;
    private Integer pageSize;

    public String getSelected() {
      return selected;
    }

    public T setSelected(String selected) {
      this.selected = selected;
      return self();
    }

    public String getQuery() {
      return query;
    }

    public T setQuery(@Nullable String query) {
      this.query = query;
      return self();
    }

    public Integer getPage() {
      return page;
    }

    public T setPage(Integer page) {
      this.page = page;
      return self();
    }

    public Integer getPageSize() {
      return pageSize;
    }

    public T setPageSize(Integer pageSize) {
      this.pageSize = pageSize;
      return self();
    }

    @SuppressWarnings("unchecked")
    final T self() {
      return (T) this;
    }
  }
}
