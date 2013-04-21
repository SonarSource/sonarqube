/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import org.junit.Test;

public class JavaPackageTest {
  @Test
  public void defaultPackage() {
    assertEquals(new JavaPackage(), new JavaPackage());
    assertEquals(JavaPackage.DEFAULT_PACKAGE_NAME, new JavaPackage(null).getKey());
    assertEquals(JavaPackage.DEFAULT_PACKAGE_NAME, new JavaPackage(null).getName());
    assertEquals(JavaPackage.DEFAULT_PACKAGE_NAME, new JavaPackage("").getKey());
    assertThat(new JavaPackage(null).isDefault(), is(true));
  }

  @Test
  public void testNewPackage() {
    assertEquals(new JavaPackage(" foo.bar   "), new JavaPackage("foo.bar"));
    JavaPackage pac = new JavaPackage("foo.bar");
    assertEquals("foo.bar", pac.getKey());
    assertEquals("foo.bar", pac.getName());
  }

  @Test
  public void singleLevelPackage() {
    assertEquals(new JavaPackage("foo"), new JavaPackage("foo"));
    JavaPackage pac = new JavaPackage("foo");
    assertEquals("foo", pac.getKey());
    assertEquals("foo", pac.getName());
  }

  @Test
  public void shouldNotMatchFilePatterns() {
    JavaPackage pac = new JavaPackage("org.sonar.commons");
    assertFalse(pac.matchFilePattern("**"));
  }

}
