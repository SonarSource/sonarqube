/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.api;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JavaUtilsTest {

  @Test
  public void shouldAbbreviatePackage() {
    assertThat(JavaUtils.abbreviatePackage(""), is(""));
    assertThat(JavaUtils.abbreviatePackage("com"), is("com"));
    assertThat(JavaUtils.abbreviatePackage("com.foo"), is("com.f"));
    assertThat(JavaUtils.abbreviatePackage("com.foo.bar.buz"), is("com.f.b.b"));
    assertThat(JavaUtils.abbreviatePackage("..."), is(""));
    assertThat(JavaUtils.abbreviatePackage("com.foo."), is("com.f"));
    assertThat(JavaUtils.abbreviatePackage("com.foo..bar"), is("com.f.b"));
  }
}
