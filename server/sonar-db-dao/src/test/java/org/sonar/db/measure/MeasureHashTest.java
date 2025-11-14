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
package org.sonar.db.measure;

import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeasureHashTest {

  @Test
  void hashCode_depends_on_both_fields() {
    MeasureHash measureHash1 = new MeasureHash("component1", 123L);
    MeasureHash measureHash2 = new MeasureHash("component", 123L);
    MeasureHash measureHash3 = new MeasureHash("component", 124L);

    assertThat(measureHash1)
      .doesNotHaveSameHashCodeAs(measureHash2)
      .doesNotHaveSameHashCodeAs(measureHash3)
      .isNotEqualTo(measureHash2)
      .isNotEqualTo(measureHash3);
  }

  @Test
  void sort_by_component_and_hash() {
    MeasureHash measureHash1 = new MeasureHash("A", 1L);
    MeasureHash measureHash2 = new MeasureHash("A", 2L);
    MeasureHash measureHash3 = new MeasureHash("B", 1L);
    MeasureHash measureHash4 = new MeasureHash("B", 2L);

    TreeSet<MeasureHash> set = new TreeSet<>(List.of(measureHash1, measureHash2, measureHash3, measureHash4));

    assertThat(set).containsExactly(measureHash1, measureHash2, measureHash3, measureHash4);
  }
}
