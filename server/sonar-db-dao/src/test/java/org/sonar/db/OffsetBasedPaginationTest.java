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
package org.sonar.db;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OffsetBasedPaginationTest {

  @Test
  void forOffset_whenNegativeOffset_shouldfailsWithIAE() {
    assertThatThrownBy(() -> OffsetBasedPagination.forOffset(-1, 10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("offset must be >= 0");
  }

  @Test
  void forOffset_whenPageSizeIsZero_shouldfailsWithIAE() {
    assertThatThrownBy(() -> OffsetBasedPagination.forOffset(1, 0))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("page size must be >= 1");
  }

  @Test
  void forOffset_whenNegativePageSize_shouldfailsWithIAE() {
    assertThatThrownBy(() -> OffsetBasedPagination.forOffset(1, -1))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("page size must be >= 1");
  }

  @Test
  void forOffset_whenZeroOffset_shouldStartOffsetAtZero() {
    assertThat(OffsetBasedPagination.forOffset(0, 100))
      .extracting(OffsetBasedPagination::getOffset, OffsetBasedPagination::getPageSize)
      .containsExactly(0, 100);
  }

  @Test
  void forStartRowNumber_whenStartRowNumberLowerThanOne_shouldfailsWithIAE() {
    assertThatThrownBy(() -> OffsetBasedPagination.forStartRowNumber(0, 10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("startRowNumber must be >= 1");
  }

  @Test
  void forStartRowNumber_whenPageSizeIsZero_shouldfailsWithIAE() {
    assertThatThrownBy(() -> OffsetBasedPagination.forStartRowNumber(1, 0))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("page size must be >= 1");
  }

  @Test
  void forStartRowNumber_whenNegativePageSize_shouldfailsWithIAE() {
    assertThatThrownBy(() -> OffsetBasedPagination.forStartRowNumber(1, -1))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("page size must be >= 1");
  }

  @Test
  void equals_whenSameParameters_shouldBeTrue() {
    Assertions.assertThat(OffsetBasedPagination.forStartRowNumber(15, 20))
      .isEqualTo(OffsetBasedPagination.forOffset(14, 20));
  }

  @Test
  void equals_whenSameObjects_shouldBeTrue() {
    OffsetBasedPagination offsetBasedPagination = OffsetBasedPagination.forStartRowNumber(15, 20);
    Assertions.assertThat(offsetBasedPagination).isEqualTo(offsetBasedPagination);
  }

  @Test
  void hashcode_whenSameObjects_shouldBeEquals() {
    OffsetBasedPagination offsetBasedPagination = OffsetBasedPagination.forStartRowNumber(15, 20);
    Assertions.assertThat(offsetBasedPagination).hasSameHashCodeAs(offsetBasedPagination);
  }

  @Test
  void equals_whenDifferentClasses_shouldBeFalse() {
    Assertions.assertThat(OffsetBasedPagination.forStartRowNumber(15, 20)).isNotEqualTo("not an OffsetBasedPagination object");
  }

  @Test
  void equals_whenDifferentPageSize_shouldBeFalse() {
    Assertions.assertThat(OffsetBasedPagination.forStartRowNumber(15, 21))
      .isNotEqualTo(OffsetBasedPagination.forOffset(14, 20));
  }

  @Test
  void equals_whenDifferentOffset_shouldBeFalse() {
    Assertions.assertThat(OffsetBasedPagination.forOffset(30, 20))
      .isNotEqualTo(OffsetBasedPagination.forOffset(15, 20));
  }

  @Test
  void hashcode_whenSameParameters_shouldBeEquals() {
    Assertions.assertThat(OffsetBasedPagination.forStartRowNumber(1, 20))
      .hasSameHashCodeAs(OffsetBasedPagination.forOffset(0, 20));
  }

  @Test
  void hashcode_whenDifferentOffset_shouldBeNotEquals() {
    Assertions.assertThat(OffsetBasedPagination.forOffset(10, 20))
      .doesNotHaveSameHashCodeAs(OffsetBasedPagination.forOffset(15, 20));
  }

  @Test
  void hashcode_whenDifferentPageSize_shouldBeNotEquals() {
    Assertions.assertThat(OffsetBasedPagination.forOffset(0, 20))
      .doesNotHaveSameHashCodeAs(OffsetBasedPagination.forOffset(0, 40));
  }

}
