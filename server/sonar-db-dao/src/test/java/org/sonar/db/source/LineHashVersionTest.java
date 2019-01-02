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
package org.sonar.db.source;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class LineHashVersionTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_create_from_int() {
    assertThat(LineHashVersion.valueOf(0)).isEqualTo(LineHashVersion.WITHOUT_SIGNIFICANT_CODE);
    assertThat(LineHashVersion.valueOf(1)).isEqualTo(LineHashVersion.WITH_SIGNIFICANT_CODE);
  }

  @Test
  public void should_throw_exception_if_version_is_too_high() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Unknown line hash version: 2");
    LineHashVersion.valueOf(2);
  }

  @Test
  public void should_throw_exception_if_version_is_too_low() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Unknown line hash version: -1");
    LineHashVersion.valueOf(-1);
  }
}
