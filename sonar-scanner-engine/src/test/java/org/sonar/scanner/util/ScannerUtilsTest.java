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
package org.sonar.scanner.util;

import org.junit.Test;
import org.sonar.api.impl.utils.ScannerUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ScannerUtilsTest {

  @Test
  public void encodeForUrl() throws Exception {
    assertThat(ScannerUtils.encodeForUrl(null)).isEqualTo("");
    assertThat(ScannerUtils.encodeForUrl("")).isEqualTo("");
    assertThat(ScannerUtils.encodeForUrl("foo")).isEqualTo("foo");
    assertThat(ScannerUtils.encodeForUrl("foo&bar")).isEqualTo("foo%26bar");
  }

  private class MyClass {
    @Override
    public String toString() {
      return null;
    }
  }
  @Test
  public void testDescribe() {
    Object withToString = new Object() {
      @Override
      public String toString() {
        return "desc";
      }
    };

    Object withoutToString = new Object();

    assertThat(ScannerUtils.describe(withToString)).isEqualTo(("desc"));
    assertThat(ScannerUtils.describe(withoutToString)).isEqualTo("java.lang.Object");
    assertThat(ScannerUtils.describe(new MyClass())).endsWith("MyClass");
  }
}
