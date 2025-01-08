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
package org.sonar.duplications.index;

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CloneGroupTest {

  @Test
  public void test_equals() {
    ClonePart clone = new ClonePart("id", 1, 1, 2);
    CloneGroup group = composeGroup(1, 1, clone, List.of(clone));

    assertThat(group)
      .isEqualTo(group)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .isNotEqualTo(composeGroup(1, 1, clone, List.of()))
      .isNotEqualTo(composeGroup(1, 1, new ClonePart("", 1, 1, 2), List.of(clone)))
      .isNotEqualTo(composeGroup(0, 1, clone, List.of(clone)))
      .isEqualTo(composeGroup(1, 1, clone, List.of(new ClonePart("id", 1, 1, 2))));
  }

  private static CloneGroup composeGroup(int length, int lengthInUnits, ClonePart origin, List<ClonePart> parts) {
    return  CloneGroup.builder()
      .setLength(length)
      .setLengthInUnits(lengthInUnits)
      .setOrigin(origin)
      .setParts(parts)
      .build();
  }
}
