/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.resources;

import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class ScopesTest {

  @Test
  public void testProject() {
    Project resource = new Project(ProjectDefinition.create());
    assertThat(Scopes.isProject(resource)).isTrue();
    assertThat(Scopes.isDirectory(resource)).isFalse();
    assertThat(Scopes.isFile(resource)).isFalse();
    assertThat(Scopes.isBlockUnit(resource)).isFalse();
    assertThat(Scopes.isProgramUnit(resource)).isFalse();
  }

  @Test
  public void testDirectory() {
    Resource resource = Directory.create("org/foo");
    assertThat(Scopes.isProject(resource)).isFalse();
    assertThat(Scopes.isDirectory(resource)).isTrue();
    assertThat(Scopes.isFile(resource)).isFalse();
    assertThat(Scopes.isBlockUnit(resource)).isFalse();
    assertThat(Scopes.isProgramUnit(resource)).isFalse();
  }

  @Test
  public void testFile() {
    Resource resource = File.create("org/foo/Bar.java");
    assertThat(Scopes.isProject(resource)).isFalse();
    assertThat(Scopes.isDirectory(resource)).isFalse();
    assertThat(Scopes.isFile(resource)).isTrue();
    assertThat(Scopes.isBlockUnit(resource)).isFalse();
    assertThat(Scopes.isProgramUnit(resource)).isFalse();
  }

  @Test
  public void shouldBeHigherThan() {
    assertThat(Scopes.isHigherThan(Scopes.PROJECT, Scopes.PROJECT)).isFalse();
    assertThat(Scopes.isHigherThan(Scopes.PROJECT, Scopes.DIRECTORY)).isTrue();
    assertThat(Scopes.isHigherThan(Scopes.PROJECT, Scopes.BLOCK_UNIT)).isTrue();

    assertThat(Scopes.isHigherThan(Scopes.FILE, Scopes.FILE)).isFalse();
    assertThat(Scopes.isHigherThan(Scopes.FILE, Scopes.DIRECTORY)).isFalse();
    assertThat(Scopes.isHigherThan(Scopes.FILE, Scopes.BLOCK_UNIT)).isTrue();
  }

  @Test
  public void shouldBeHigherThanOrEquals() {
    assertThat(Scopes.isHigherThanOrEquals(Scopes.PROJECT, Scopes.PROJECT)).isTrue();
    assertThat(Scopes.isHigherThanOrEquals(Scopes.PROJECT, Scopes.DIRECTORY)).isTrue();
    assertThat(Scopes.isHigherThanOrEquals(Scopes.PROJECT, Scopes.BLOCK_UNIT)).isTrue();

    assertThat(Scopes.isHigherThanOrEquals(Scopes.FILE, Scopes.FILE)).isTrue();
    assertThat(Scopes.isHigherThanOrEquals(Scopes.FILE, Scopes.DIRECTORY)).isFalse();
    assertThat(Scopes.isHigherThanOrEquals(Scopes.FILE, Scopes.BLOCK_UNIT)).isTrue();
  }
}
