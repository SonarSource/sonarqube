/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.common;

import static org.sonar.api.utils.Preconditions.checkArgument;

public class PaginationInformation {
  private final int pageSize;
  private final int pageIndex;
  private final int total;

  private PaginationInformation(int pageSize, int pageIndex, int total) {
    checkArgument(pageSize >= 0, "Page size must be positive. Got %s", pageSize);
    checkArgument(pageIndex >= 1, "Page index must be strictly positive. Got %s", pageIndex);
    checkArgument(total >= 0, "Total items must be positive. Got %s", total);
    this.pageSize = pageSize;
    this.pageIndex = pageIndex;
    this.total = total;
  }

  public static PaginationInformation.Builder forPageIndex(int pageIndex) {
    return new PaginationInformation.Builder(pageIndex);
  }

  /**
   * Page index, >= 1.
   */
  public int pageIndex() {
    return pageIndex;
  }

  /**
   * Maximum number of items per page. It is >= 0.
   */
  public int pageSize() {
    return pageSize;
  }

  /**
   * Total number of items. It is >= 0.
   */
  public int total() {
    return total;
  }

  public static int offset(int pageIndex, int pageSize) {
    return (pageIndex - 1) * pageSize;
  }

  public static class Builder {
    private int pageSize;
    private int pageIndex;

    private Builder(int pageIndex) {
      this.pageIndex = pageIndex;
    }

    public PaginationInformation.Builder withPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public PaginationInformation andTotal(int total) {
      return new PaginationInformation(pageSize, pageIndex, total);
    }
  }

}
