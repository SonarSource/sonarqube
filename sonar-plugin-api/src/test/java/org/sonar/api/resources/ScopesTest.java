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
package org.sonar.api.resources;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ScopesTest {

  @Test
  public void testProject() {
    Project resource = new Project("key");
    assertThat(Scopes.isProject(resource), is(true));
    assertThat(Scopes.isDirectory(resource), is(false));
    assertThat(Scopes.isFile(resource), is(false));
    assertThat(Scopes.isBlockUnit(resource), is(false));
    assertThat(Scopes.isProgramUnit(resource), is(false));
  }

  @Test
  public void testDirectory() {
    Resource resource = Directory.create("org/foo");
    assertThat(Scopes.isProject(resource), is(false));
    assertThat(Scopes.isDirectory(resource), is(true));
    assertThat(Scopes.isFile(resource), is(false));
    assertThat(Scopes.isBlockUnit(resource), is(false));
    assertThat(Scopes.isProgramUnit(resource), is(false));
  }

  @Test
  public void testFile() {
    Resource resource = File.create("org/foo/Bar.java");
    assertThat(Scopes.isProject(resource), is(false));
    assertThat(Scopes.isDirectory(resource), is(false));
    assertThat(Scopes.isFile(resource), is(true));
    assertThat(Scopes.isBlockUnit(resource), is(false));
    assertThat(Scopes.isProgramUnit(resource), is(false));
  }

  @Test
  public void shouldBeHigherThan() {
    assertThat(Scopes.isHigherThan(Scopes.PROJECT, Scopes.PROJECT), is(false));
    assertThat(Scopes.isHigherThan(Scopes.PROJECT, Scopes.DIRECTORY), is(true));
    assertThat(Scopes.isHigherThan(Scopes.PROJECT, Scopes.BLOCK_UNIT), is(true));

    assertThat(Scopes.isHigherThan(Scopes.FILE, Scopes.FILE), is(false));
    assertThat(Scopes.isHigherThan(Scopes.FILE, Scopes.DIRECTORY), is(false));
    assertThat(Scopes.isHigherThan(Scopes.FILE, Scopes.BLOCK_UNIT), is(true));
  }

  @Test
  public void shouldBeHigherThanOrEquals() {
    assertThat(Scopes.isHigherThanOrEquals(Scopes.PROJECT, Scopes.PROJECT), is(true));
    assertThat(Scopes.isHigherThanOrEquals(Scopes.PROJECT, Scopes.DIRECTORY), is(true));
    assertThat(Scopes.isHigherThanOrEquals(Scopes.PROJECT, Scopes.BLOCK_UNIT), is(true));

    assertThat(Scopes.isHigherThanOrEquals(Scopes.FILE, Scopes.FILE), is(true));
    assertThat(Scopes.isHigherThanOrEquals(Scopes.FILE, Scopes.DIRECTORY), is(false));
    assertThat(Scopes.isHigherThanOrEquals(Scopes.FILE, Scopes.BLOCK_UNIT), is(true));
  }
}
