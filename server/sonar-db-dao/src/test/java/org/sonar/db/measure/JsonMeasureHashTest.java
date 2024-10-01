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
package org.sonar.db.measure;

import java.util.List;
import java.util.TreeSet;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonMeasureHashTest {

  @Test
  public void hashCode_depends_on_both_fields() {
    JsonMeasureHash measureHash1 = new JsonMeasureHash("component1", 123L);
    JsonMeasureHash measureHash2 = new JsonMeasureHash("component", 123L);
    JsonMeasureHash measureHash3 = new JsonMeasureHash("component", 124L);

    assertThat(measureHash1)
      .doesNotHaveSameHashCodeAs(measureHash2)
      .doesNotHaveSameHashCodeAs(measureHash3)
      .isNotEqualTo(measureHash2)
      .isNotEqualTo(measureHash3);
  }

  @Test
  public void sort_by_component_and_hash() {
    JsonMeasureHash measureHash1 = new JsonMeasureHash("A", 1L);
    JsonMeasureHash measureHash2 = new JsonMeasureHash("A", 2L);
    JsonMeasureHash measureHash3 = new JsonMeasureHash("B", 1L);
    JsonMeasureHash measureHash4 = new JsonMeasureHash("B", 2L);

    TreeSet<JsonMeasureHash> set = new TreeSet<>(List.of(measureHash1, measureHash2, measureHash3, measureHash4));

    assertThat(set).containsExactly(measureHash1, measureHash2, measureHash3, measureHash4);
  }
}
