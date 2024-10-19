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
package org.sonar.api.impl.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScannerUtilsTest {
  @Test
  public void test_pluralize() {
    assertThat(ScannerUtils.pluralize("string", 0)).isEqualTo("strings");
    assertThat(ScannerUtils.pluralize("string", 1)).isEqualTo("string");
    assertThat(ScannerUtils.pluralize("string", 2)).isEqualTo("strings");
  }

  @Test
  public void cleanKeyForFilename() {
    assertThat(ScannerUtils.cleanKeyForFilename("project 1")).isEqualTo("project1");
    assertThat(ScannerUtils.cleanKeyForFilename("project:1")).isEqualTo("project_1");
  }

  @Test
  public void describe() {
    assertThat(ScannerUtils.describe(new Object())).isEqualTo("java.lang.Object");
    assertThat(ScannerUtils.describe(new TestClass())).isEqualTo("overridden");
  }

  static class TestClass {
    @Override
    public String toString() {
      return "overridden";
    }
  }
}
