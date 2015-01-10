/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeprecatedKeyUtilsTest {

  @Test
  public void testGetJavaFileParentDeprecatedKey() {
    assertThat(DeprecatedKeyUtils.getJavaFileParentDeprecatedKey("org.foo.bar.Hello")).isEqualTo("org/foo/bar");
    assertThat(DeprecatedKeyUtils.getJavaFileParentDeprecatedKey("[default].Hello")).isEqualTo("[root]");
    assertThat(DeprecatedKeyUtils.getJavaFileParentDeprecatedKey("Hello")).isEqualTo("[root]");
  }

  @Test
  public void testGetJavaFileDeprecatedKey() {
    assertThat(DeprecatedKeyUtils.getJavaFileDeprecatedKey("org/foo/bar/Hello.java")).isEqualTo("org.foo.bar.Hello");
    assertThat(DeprecatedKeyUtils.getJavaFileDeprecatedKey("Hello.java")).isEqualTo("[default].Hello");
  }
}
