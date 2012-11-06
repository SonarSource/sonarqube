/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.bootstrap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class TempDirectoriesTest {

  private TempDirectories tempDirectories;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void before() throws IOException {
    ProjectDefinition project = ProjectDefinition.create().setKey("foo").setWorkDir(folder.newFolder());
    ProjectReactor reactor = new ProjectReactor(project);
    tempDirectories = new TempDirectories(reactor);
  }

  @Test
  public void should_create_root_temp_dir() {
    assertThat(tempDirectories.getRoot()).isNotNull();
    assertThat(tempDirectories.getRoot()).exists();
    assertThat(tempDirectories.getRoot()).isDirectory();
    assertThat(tempDirectories.getRoot().getName()).isEqualTo("_tmp");
  }

  @Test
  public void should_accept_empty_dirname() {
    assertThat(tempDirectories.getDir("")).isEqualTo(tempDirectories.getRoot());
  }

  @Test
  public void should_create_sub_directory() {
    File findbugsDir = tempDirectories.getDir("findbugs");
    assertThat(findbugsDir).isNotNull();
    assertThat(findbugsDir).exists();
    assertThat(findbugsDir.getParentFile()).isEqualTo(tempDirectories.getRoot());
    assertThat(findbugsDir.getName()).isEqualTo("findbugs");
  }

  @Test
  public void should_delete_temp_dir_on_shutdown() {
    File root = tempDirectories.getRoot();
    File findbugsDir = tempDirectories.getDir("findbugs");
    assertThat(findbugsDir).exists();

    tempDirectories.stop();

    assertThat(root).doesNotExist();
    assertThat(findbugsDir).doesNotExist();
  }

  @Test
  public void should_create_parent_directory() {
    File file = tempDirectories.getFile("findbugs", "bcel.jar");
    assertThat(file).isNotNull();
    assertThat(file.getParentFile().getName()).isEqualTo("findbugs");
  }
}
