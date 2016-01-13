/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.batch;

import org.junit.Test;
import org.sonar.api.resources.JavaFile;

public class SquidUtilsTest {

  @Test(expected = UnsupportedOperationException.class)
  public void convertJavaFileKeyFromSquidFormat() {
    SquidUtils.convertJavaFileKeyFromSquidFormat("java/lang/String");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldConvertJavaPackageKeyFromSquidFormat() {
    SquidUtils.convertJavaPackageKeyFromSquidFormat("java/lang");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldConvertToSquidKeyFormat() {
    SquidUtils.convertToSquidKeyFormat(new JavaFile("com.foo.Bar"));
  }
}
