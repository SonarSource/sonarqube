/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db;

import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;

@Immutable
public final class Pagination {
  private static final Pagination ALL = new Builder(1).andSize(Integer.MAX_VALUE);

  private final int page;
  private final int pageSize;

  private Pagination(Builder builder) {
    this.page = builder.page;
    this.pageSize = builder.pageSize;
  }

  public static Pagination all() {
    return ALL;
  }

  public static Builder forPage(int page) {
    return new Builder(page);
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getOffset() {
    return (page - 1) * pageSize;
  }

  public int getStartRowNumber() {
    return getOffset() + 1;
  }

  public int getEndRowNumber() {
    return page * pageSize;
  }

  public static final class Builder {
    private final int page;
    private int pageSize = 0;

    public Builder(int page) {
      checkArgument(page >= 1, "page index must be >= 1");
      this.page = page;
    }

    public Pagination andSize(int pageSize) {
      checkArgument(pageSize >= 1, "page size must be >= 1");
      this.pageSize = pageSize;
      return new Pagination(this);
    }
  }
}
