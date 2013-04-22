/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.batch;

import org.junit.Test;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SquidUtilsTest {

  @Test
  public void convertJavaFileKeyFromSquidFormat() {
    assertEquals(new JavaFile("java.lang.String"), SquidUtils.convertJavaFileKeyFromSquidFormat("java/lang/String"));
    assertEquals(new JavaFile("java.lang.String"), SquidUtils.convertJavaFileKeyFromSquidFormat("java/lang/String.java"));
    assertEquals(new JavaFile("String"), SquidUtils.convertJavaFileKeyFromSquidFormat("String.java"));
    assertEquals(new JavaFile("String"), SquidUtils.convertJavaFileKeyFromSquidFormat("String"));
  }

  @Test
  public void shouldConvertJavaPackageKeyFromSquidFormat() {
    assertEquals(new JavaPackage("java.lang"), SquidUtils.convertJavaPackageKeyFromSquidFormat("java/lang"));
    assertEquals(new JavaPackage(""), SquidUtils.convertJavaPackageKeyFromSquidFormat(""));
    assertEquals(new JavaPackage("singlepackage"), SquidUtils.convertJavaPackageKeyFromSquidFormat("singlepackage"));
  }

  @Test
  public void shouldConvertToSquidKeyFormat() {
    assertThat(SquidUtils.convertToSquidKeyFormat(new JavaFile("com.foo.Bar")), is("com/foo/Bar.java"));
    assertThat(SquidUtils.convertToSquidKeyFormat(new JavaFile("Bar")), is("Bar.java"));
  }
}