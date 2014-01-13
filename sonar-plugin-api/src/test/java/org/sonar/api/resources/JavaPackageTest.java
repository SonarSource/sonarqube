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
package org.sonar.api.resources;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class JavaPackageTest {
  @Test
  public void defaultPackage() {
    assertEquals(new JavaPackage(), new JavaPackage());
    assertEquals(JavaPackage.DEFAULT_PACKAGE_NAME, new JavaPackage(null).getDeprecatedKey());
    assertEquals(JavaPackage.DEFAULT_PACKAGE_NAME, new JavaPackage(null).getName());
    assertEquals(JavaPackage.DEFAULT_PACKAGE_NAME, new JavaPackage("").getDeprecatedKey());
    assertThat(new JavaPackage(null).isDefault(), is(true));
  }

  @Test
  public void testNewPackage() {
    assertEquals(JavaPackage.create("src/foo/bar", " foo.bar   "), JavaPackage.create("src/foo/bar", "foo.bar"));
    JavaPackage pac = JavaPackage.create("src/foo/bar", "foo.bar");
    assertEquals("/src/foo/bar", pac.getKey());
    assertEquals("foo.bar", pac.getDeprecatedKey());
    assertEquals("foo.bar", pac.getName());
  }

  @Test
  public void testNewPackageDeprecatedConstructor() {
    assertEquals(new JavaPackage(" foo.bar   "), new JavaPackage("foo.bar"));
    JavaPackage pac = new JavaPackage("foo.bar");
    assertEquals("foo.bar", pac.getDeprecatedKey());
    assertEquals("foo.bar", pac.getName());
  }

  @Test
  public void singleLevelPackage() {
    assertEquals(new JavaPackage("foo"), new JavaPackage("foo"));
    JavaPackage pac = new JavaPackage("foo");
    assertEquals("foo", pac.getDeprecatedKey());
    assertEquals("foo", pac.getName());
  }

  @Test
  public void shouldNotMatchFilePatterns() {
    JavaPackage pac = new JavaPackage("org.sonar.commons");
    assertFalse(pac.matchFilePattern("**"));
  }

}
