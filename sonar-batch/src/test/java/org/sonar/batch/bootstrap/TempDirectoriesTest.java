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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

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
    assertNotNull(tempDirectories.getRoot());
    assertThat(tempDirectories.getRoot().exists(), is(true));
    assertThat(tempDirectories.getRoot().isDirectory(), is(true));
  }

  @Test
  public void shouldCreateDirectory() {
    File findbugsDir = tempDirectories.getDir("findbugs");
    assertNotNull(findbugsDir);
    assertThat(findbugsDir.exists(), is(true));
    assertThat(findbugsDir.getParentFile(), is(tempDirectories.getRoot()));
    assertThat(findbugsDir.getName(), is("findbugs"));
  }

  @Test
  public void shouldStopAndDeleteDirectory() {
    File root = tempDirectories.getRoot();
    File findbugsDir = tempDirectories.getDir("findbugs");
    assertThat(findbugsDir.exists(), is(true));

    tempDirectories.stop();

    assertThat(root.exists(), is(false));
    assertThat(findbugsDir.exists(), is(false));
  }

  @Test
  public void shouldCreateDirectoryWhenGettingFile() {
    File file = tempDirectories.getFile("findbugs", "bcel.jar");
    assertNotNull(file);
    assertThat(file.getParentFile().getName(), is("findbugs"));
  }
}
