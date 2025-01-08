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
package org.sonar.telemetry.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DimensionTest {
  @Test
  void getValue() {
    assertEquals("installation", Dimension.INSTALLATION.getValue());
    assertEquals("user", Dimension.USER.getValue());
    assertEquals("project", Dimension.PROJECT.getValue());
    assertEquals("language", Dimension.LANGUAGE.getValue());
  }

  @Test
  void fromValue() {
    assertEquals(Dimension.INSTALLATION, Dimension.fromValue("installation"));
    assertEquals(Dimension.USER, Dimension.fromValue("user"));
    assertEquals(Dimension.PROJECT, Dimension.fromValue("project"));
    assertEquals(Dimension.LANGUAGE, Dimension.fromValue("language"));

    assertEquals(Dimension.INSTALLATION, Dimension.fromValue("INSTALLATION"));
    assertEquals(Dimension.USER, Dimension.fromValue("USER"));
    assertEquals(Dimension.PROJECT, Dimension.fromValue("PROJECT"));
    assertEquals(Dimension.LANGUAGE, Dimension.fromValue("LANGUAGE"));
  }

  @Test
  void fromValue_whenInvalid() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      Dimension.fromValue("invalid");
    });
    assertEquals("Unknown dimension value: invalid", exception.getMessage());
  }
}
