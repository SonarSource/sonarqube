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

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class JavaPackageTest {
  @Test
  public void defaultPackageDeprecatedConstructor() {
    assertEquals(new JavaPackage(), new JavaPackage());
    assertEquals(Directory.ROOT, new JavaPackage(null).getDeprecatedKey());
    assertEquals(Directory.ROOT, new JavaPackage("").getDeprecatedKey());
    assertThat(new JavaPackage(null).isDefault(), is(true));
  }

  @Test
  public void testNewPackageDeprecatedConstructor() {
    assertEquals(new JavaPackage(" foo.bar   "), new JavaPackage("foo.bar"));
    JavaPackage pac = new JavaPackage("foo.bar");
    assertEquals("foo/bar", pac.getDeprecatedKey());
  }

  @Test
  public void singleLevelPackageDeprecatedConstructor() {
    assertEquals(new JavaPackage("foo"), new JavaPackage("foo"));
    JavaPackage pac = new JavaPackage("foo");
    assertEquals("foo", pac.getDeprecatedKey());
  }

  @Test
  public void shouldNotMatchFilePatterns() {
    JavaPackage pac = new JavaPackage("org.sonar.commons");
    assertFalse(pac.matchFilePattern("**"));
  }

  @Test
  public void packagesAreEquivalentToDirectories() {
    JavaPackage pac = new JavaPackage();
    pac.setKey("someKey");
    Directory dir = new Directory();
    dir.setKey("someKey");
    assertThat(pac).isEqualTo(dir);
  }

}
