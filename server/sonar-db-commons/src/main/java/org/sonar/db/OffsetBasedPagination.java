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
package org.sonar.db;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

public class OffsetBasedPagination implements Pagineable {

  private final int offset;
  private final int pageSize;

  private OffsetBasedPagination(int offset, int pageSize) {
    this.offset = offset;
    this.pageSize = pageSize;
  }

  /**
   * @param offset as meant by database sql offset: how many rows will be skipped before selecting the first result. offset=0 means no element would be skipped
   * @param pageSize how many rows should be returned
   * @return
   */
  public static OffsetBasedPagination forOffset(int offset, int pageSize) {
    checkArgument(offset >= 0, "offset must be >= 0");
    checkArgument(pageSize >= 1, "page size must be >= 1");
    return new OffsetBasedPagination(offset, pageSize);
  }

  /**
   * @param startRowNumber index of the first element to be returned. startRowNumber = 1 means no element would be skipped
   * @param pageSize how many rows should be returned
   * @return
   */
  public static OffsetBasedPagination forStartRowNumber(int startRowNumber, int pageSize) {
    checkArgument(startRowNumber >= 1, "startRowNumber must be >= 1");
    checkArgument(pageSize >= 1, "page size must be >= 1");
    return new OffsetBasedPagination(startRowNumber - 1, pageSize);
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public int getPageSize() {
    return pageSize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OffsetBasedPagination that = (OffsetBasedPagination) o;
    return offset == that.offset && pageSize == that.pageSize;
  }

  @Override
  public int hashCode() {
    return Objects.hash(offset, pageSize);
  }
}
