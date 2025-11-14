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
package org.sonar.core.hash;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LineRangeTest {

  @Test
  public void should_throw_ISE_if_range_is_invalid() {
    assertThatThrownBy(() -> new LineRange(2, 1))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Line range is not valid: 1 must be greater or equal than 2");
  }

  @Test
  public void should_throw_ISE_if_startOffset_is_invalid() {
    assertThatThrownBy(() -> new LineRange(-1, 1))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Start offset not valid: -1");
  }

  @Test
  public void check_getters() {
    LineRange range = new LineRange(1, 2);
    assertThat(range.startOffset()).isOne();
    assertThat(range.endOffset()).isEqualTo(2);
  }
}
