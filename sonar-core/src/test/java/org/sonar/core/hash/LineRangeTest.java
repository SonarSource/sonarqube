/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class LineRangeTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_throw_ISE_if_range_is_invalid() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Line range is not valid: 1 must be greater or equal than 2");
    new LineRange(2, 1);
  }
  
  @Test
  public void should_throw_ISE_if_startOffset_is_invalid() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Start offset not valid: -1");
    new LineRange(-1, 1);
  }

  @Test
  public void check_getters() {
    LineRange range = new LineRange(1, 2);
    assertThat(range.startOffset()).isEqualTo(1);
    assertThat(range.endOffset()).isEqualTo(2);
  }
}
