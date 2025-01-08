/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class PaginationInformationTest {

  @Test
  public void paginationInformation_whenPageIndexIsZero_shouldThrow() {
    assertThatIllegalArgumentException().isThrownBy(() -> PaginationInformation.forPageIndex(0).withPageSize(1).andTotal(1))
      .withMessage("Page index must be strictly positive. Got 0");
  }
  @Test
  public void paginationInformation_whenPageSizeIsNegative_shouldThrow() {
    assertThatIllegalArgumentException().isThrownBy(() -> PaginationInformation.forPageIndex(1).withPageSize(-1).andTotal(1))
      .withMessage("Page size must be positive. Got -1");
  }
  @Test
  public void paginationInformation_withPageSizeAndTotalSetTo0_shouldBeCreatedCorrectly() {
    PaginationInformation paginationInformation = PaginationInformation.forPageIndex(1).withPageSize(0).andTotal(0);

    assertThat(paginationInformation.pageIndex()).isOne();
    assertThat(paginationInformation.pageSize()).isZero();
    assertThat(paginationInformation.total()).isZero();
  }
  @Test
  public void paginationInformation_withHighPositiveValues_shouldBeCreatedCorrectly() {
    PaginationInformation paginationInformation = PaginationInformation.forPageIndex(1000).withPageSize(2344).andTotal(13213);

    assertThat(paginationInformation.pageIndex()).isEqualTo(1000);
    assertThat(paginationInformation.pageSize()).isEqualTo(2344);
    assertThat(paginationInformation.total()).isEqualTo(13213);
  }



}
