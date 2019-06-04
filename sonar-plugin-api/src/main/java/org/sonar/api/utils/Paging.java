/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.utils;

import static org.sonar.api.utils.Preconditions.checkArgument;

/**
 * @since 3.6
 */
public class Paging {

  private final int pageSize;
  private final int pageIndex;
  private final int total;

  private Paging(int pageSize, int pageIndex, int total) {
    checkArgument(pageSize >= 1, "Page size must be strictly positive. Got %s", pageSize);
    checkArgument(pageIndex >= 1, "Page index must be strictly positive. Got %s", pageIndex);
    checkArgument(total >= 0, "Total items must be positive. Got %s", total);
    this.pageSize = pageSize;
    this.pageIndex = pageIndex;
    this.total = total;
  }

  /**
   * @deprecated since 5.2 please use the #forPgeIndex(...) builder method
   */
  @Deprecated
  public static Paging create(int pageSize, int pageIndex, int totalItems) {
    return new Paging(pageSize, pageIndex, totalItems);
  }

  public static Builder forPageIndex(int pageIndex) {
    return new Builder(pageIndex);
  }

  /**
   * Page index, starting with 1.
   */
  public int pageIndex() {
    return pageIndex;
  }

  /**
   * Maximum number of items per page. It is greater than 0.
   */
  public int pageSize() {
    return pageSize;
  }

  /**
   * Total number of items. It is greater than or equal 0.
   */
  public int total() {
    return total;
  }

  public int offset() {
    return (pageIndex - 1) * pageSize;
  }

  public static int offset(int pageIndex, int pageSize) {
    return (pageIndex - 1) * pageSize;
  }

  /**
   * Number of pages. It is greater than or equal 0.
   */
  public int pages() {
    int p = total / pageSize;
    if (total % pageSize > 0) {
      p++;
    }
    return p;
  }

  /**
   *
   * @since 4.1
   */
  public boolean hasNextPage() {
    return pageIndex() < pages();
  }

  public static class Builder {
    private int pageSize;
    private int pageIndex;

    private Builder(int pageIndex) {
      this.pageIndex = pageIndex;
    }

    public Builder withPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Paging andTotal(int total) {
      return new Paging(pageSize, pageIndex, total);
    }
  }
}
