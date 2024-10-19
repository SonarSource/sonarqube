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
package org.sonar.duplications.index;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClonePartTest {

  @Test
  public void test_equals() {
    ClonePart part = new ClonePart("id_1", 1, 2, 3);

    assertThat(part)
      .isEqualTo(part)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .isNotEqualTo(new ClonePart("id_1", 1, 2, 0))
      .isNotEqualTo(new ClonePart("id_1", 1, 0, 3))
      .isNotEqualTo(new ClonePart("id_1", 0, 2, 3))
      .isNotEqualTo(new ClonePart("id_2", 1, 2, 3))
      .isEqualTo(new ClonePart("id_1", 1, 2, 3));
  }
}
