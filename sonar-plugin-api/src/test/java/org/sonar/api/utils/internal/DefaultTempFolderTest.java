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
package org.sonar.api.utils.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultTempFolderTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void createTempFolderAndFile() throws Exception {
    File tempFolder = temp.newFolder();
    DefaultTempFolder tempUtils = new DefaultTempFolder(tempFolder);
    File dir = tempUtils.newDir();
    assertThat(dir).exists().isDirectory();
    File file = tempUtils.newFile();
    assertThat(file).exists().isFile();

    tempUtils.stop();
    assertThat(tempFolder).doesNotExist();
  }

  @Test
  public void createTempFolderWithName() throws Exception {
    File tempFolder = temp.newFolder();
    DefaultTempFolder tempUtils = new DefaultTempFolder(tempFolder);
    File dir = tempUtils.newDir("sample");
    assertThat(dir).exists().isDirectory();
    assertThat(new File(tempFolder, "sample")).isEqualTo(dir);

    tempUtils.stop();
    assertThat(tempFolder).doesNotExist();
  }
}
