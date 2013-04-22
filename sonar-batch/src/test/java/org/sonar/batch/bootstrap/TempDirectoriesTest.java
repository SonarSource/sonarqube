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
package org.sonar.batch.bootstrap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class TempDirectoriesTest {

  private TempDirectories tempDirectories;

  @Before
  public void before() throws IOException {
    tempDirectories = new TempDirectories();
  }

  @After
  public void after() {
    if (tempDirectories != null) {
      tempDirectories.stop();
    }
  }

  @Test
  public void shouldCreateRoot() {
    assertThat(tempDirectories.getRoot()).isNotNull();
    assertThat(tempDirectories.getRoot()).exists();
    assertThat(tempDirectories.getRoot()).isDirectory();
    assertThat(tempDirectories.getDir("")).isEqualTo(tempDirectories.getRoot());
  }

  @Test
  public void shouldCreateDirectory() {
    File findbugsDir = tempDirectories.getDir("findbugs");
    assertThat(findbugsDir).isNotNull();
    assertThat(findbugsDir).exists();
    assertThat(findbugsDir.getParentFile()).isEqualTo(tempDirectories.getRoot());
    assertThat(findbugsDir.getName()).isEqualTo("findbugs");
  }

  @Test
  public void shouldStopAndDeleteDirectory() {
    File root = tempDirectories.getRoot();
    File findbugsDir = tempDirectories.getDir("findbugs");
    assertThat(findbugsDir).exists();

    tempDirectories.stop();

    assertThat(root).doesNotExist();
    assertThat(findbugsDir).doesNotExist();
  }

  @Test
  public void shouldCreateDirectoryWhenGettingFile() {
    File file = tempDirectories.getFile("findbugs", "bcel.jar");
    assertThat(file).isNotNull();
    assertThat(file.getParentFile().getName()).isEqualTo("findbugs");
  }
}
