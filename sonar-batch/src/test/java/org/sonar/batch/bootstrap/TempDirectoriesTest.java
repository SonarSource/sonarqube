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

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
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

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  TempDirectories tempDirectories;
  File workDir;
  ProjectReactor reactor;

  @Before
  public void before() throws IOException {
    workDir = folder.newFolder();
    ProjectDefinition project = ProjectDefinition.create().setKey("foo").setWorkDir(workDir);
    reactor = new ProjectReactor(project);
  }

  @Test
  public void should_create_root_temp_dir() throws IOException {
    tempDirectories = new TempDirectories(reactor);
    assertThat(tempDirectories.getRoot()).isNotNull();
    assertThat(tempDirectories.getRoot()).exists();
    assertThat(tempDirectories.getRoot()).isDirectory();
    assertThat(tempDirectories.getRoot().getName()).isEqualTo("_tmp");
  }

  @Test
  public void should_clean_root_temp_dir_at_startup() throws IOException {
    File tempFile = new File(workDir, "_tmp/foo.txt");
    FileUtils.touch(tempFile);
    assertThat(tempFile).exists();

    tempDirectories = new TempDirectories(reactor);
    assertThat(tempDirectories.getRoot().getParentFile()).isEqualTo(workDir);
    assertThat(tempDirectories.getRoot()).exists();
    assertThat(tempFile).doesNotExist();
    assertThat(FileUtils.listFiles(tempDirectories.getRoot(), new String[]{"txt"}, true)).isEmpty();
  }

  @Test
  public void should_accept_empty_dirname() throws IOException {
    tempDirectories = new TempDirectories(reactor);
    assertThat(tempDirectories.getDir("")).isEqualTo(tempDirectories.getRoot());
  }

  @Test
  public void should_create_sub_directory() throws IOException {
    tempDirectories = new TempDirectories(reactor);
    File findbugsDir = tempDirectories.getDir("findbugs");
    assertThat(findbugsDir).isNotNull();
    assertThat(findbugsDir).exists();
    assertThat(findbugsDir.getParentFile()).isEqualTo(tempDirectories.getRoot());
    assertThat(findbugsDir.getName()).isEqualTo("findbugs");
  }

  @Test
  public void should_delete_temp_dir_on_shutdown() throws IOException {
    tempDirectories = new TempDirectories(reactor);
    File root = tempDirectories.getRoot();
    File findbugsDir = tempDirectories.getDir("findbugs");
    assertThat(findbugsDir).exists();

    tempDirectories.stop();

    assertThat(root).doesNotExist();
    assertThat(findbugsDir).doesNotExist();
  }

  @Test
  public void should_create_parent_directory() throws IOException {
    tempDirectories = new TempDirectories(reactor);
    File file = tempDirectories.getFile("findbugs", "bcel.jar");
    assertThat(file).isNotNull();
    assertThat(file.getParentFile().getName()).isEqualTo("findbugs");
  }
}
