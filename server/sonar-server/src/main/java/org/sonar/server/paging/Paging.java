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
 * @since 4.2
 * @deprecated use {@link org.sonar.server.search.Result}
 */
@Deprecated
public class Paging {

  private final int pageSize;
  private final int pageIndex;

  protected Paging(int pageSize, int pageIndex) {
    this.pageSize = pageSize;
    this.pageIndex = pageIndex;
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

  public int offset(){
    return (pageIndex - 1) * pageSize;
  }

  public static Paging create(int pageSize, int pageIndex) {
    checkPageSize(pageSize);
    checkPageIndex(pageIndex);
    return new Paging(pageSize, pageIndex);
  }

  protected static void checkPageIndex(int pageIndex) {
    if (pageIndex<1) {
      throw new IllegalArgumentException("Page index must be strictly positive. Got " + pageIndex);
    }
  }

  protected static void checkPageSize(int pageSize) {
    if (pageSize<1) {
      throw new IllegalArgumentException("Page size must be strictly positive. Got " + pageSize);
    }
  }
}
