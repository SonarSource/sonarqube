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
package org.sonar.server.paging;

/**
 * Heavily inspired by {@link org.sonar.api.utils.Paging}
 *
 * @since 4.2
 * @deprecated use {@link org.sonar.server.search.Result}
 */
@Deprecated
public class PagingResult extends Paging {

  private final long total;

  private PagingResult(int pageSize, int pageIndex, long total) {
    super(pageSize, pageIndex);
    this.total = total;
  }

  /**
   * Number of pages. It is greater than or equal 0.
   */
  public long pages() {
    long p = total / pageSize();
    if (total % pageSize() > 0) {
      p++;
    }
    return p;
  }

  public boolean hasNextPage() {
    return pageIndex() < pages();
  }

  /**
   * Total number of items. It is greater than or equal 0.
   */
  public long total() {
    return total;
  }

  public static PagingResult create(int pageSize, int pageIndex, long totalItems) {
    checkPageSize(pageSize);
    checkPageIndex(pageIndex);
    checkTotalItems(totalItems);

    return new PagingResult(pageSize, pageIndex, totalItems);
  }

  protected static void checkTotalItems(long totalItems) {
    if (totalItems < 0) {
      throw new IllegalArgumentException("Total items must be positive. Got " + totalItems);
    }
  }
}
