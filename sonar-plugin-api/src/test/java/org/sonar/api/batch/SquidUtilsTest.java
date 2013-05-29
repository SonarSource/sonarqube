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

import static org.fest.assertions.Assertions.assertThat;

public class SquidUtilsTest {

  @Test
  public void convertJavaFileKeyFromSquidFormat() {
    assertThat(new JavaFile("java.lang.String")).isEqualTo(SquidUtils.convertJavaFileKeyFromSquidFormat("java/lang/String"));
    assertThat(new JavaFile("java.lang.String")).isEqualTo(SquidUtils.convertJavaFileKeyFromSquidFormat("java/lang/String.java"));
    assertThat(new JavaFile("String")).isEqualTo(SquidUtils.convertJavaFileKeyFromSquidFormat("String.java"));
    assertThat(new JavaFile("String")).isEqualTo(SquidUtils.convertJavaFileKeyFromSquidFormat("String"));
  }

  @Test
  public void shouldConvertJavaPackageKeyFromSquidFormat() {
    assertThat(new JavaPackage("java.lang")).isEqualTo(SquidUtils.convertJavaPackageKeyFromSquidFormat("java/lang"));
    assertThat(new JavaPackage("")).isEqualTo(SquidUtils.convertJavaPackageKeyFromSquidFormat(""));
    assertThat(new JavaPackage("singlepackage")).isEqualTo(SquidUtils.convertJavaPackageKeyFromSquidFormat("singlepackage"));
  }

  @Test
  public void shouldConvertToSquidKeyFormat() {
    assertThat(SquidUtils.convertToSquidKeyFormat(new JavaFile("com.foo.Bar"))).isEqualTo(("com/foo/Bar.java"));
    assertThat(SquidUtils.convertToSquidKeyFormat(new JavaFile("Bar"))).isEqualTo(("Bar.java"));
  }
}