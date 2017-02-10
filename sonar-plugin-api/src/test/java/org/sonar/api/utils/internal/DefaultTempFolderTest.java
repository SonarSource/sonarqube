/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.api.utils.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTempFolderTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void createTempFolderAndFile() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder tempFolder = new DefaultTempFolder(rootTempFolder);
    File dir = tempFolder.newDir();
    assertThat(dir).exists().isDirectory();
    File file = tempFolder.newFile();
    assertThat(file).exists().isFile();

    new TempFolderCleaner(tempFolder).stop();
    assertThat(rootTempFolder).doesNotExist();
  }

  @Test
  public void createTempFolderWithName() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder tempFolder = new DefaultTempFolder(rootTempFolder);
    File dir = tempFolder.newDir("sample");
    assertThat(dir).exists().isDirectory();
    assertThat(new File(rootTempFolder, "sample")).isEqualTo(dir);

    new TempFolderCleaner(tempFolder).stop();
    assertThat(rootTempFolder).doesNotExist();
  }

  @Test
  public void createTempFolderWithInvalidName() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder tempFolder = new DefaultTempFolder(rootTempFolder);
    String tooLong = "tooooolong";
    for (int i = 0; i < 50; i++) {
      tooLong += "tooooolong";
    }
    throwable.expect(IllegalStateException.class);
    throwable.expectMessage("Failed to create temp directory");
    tempFolder.newDir(tooLong);
  }

  @Test
  public void createNewFileWithInvalidName() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder tempFolder = new DefaultTempFolder(rootTempFolder);
    String tooLong = "tooooolong";
    for (int i = 0; i < 50; i++) {
      tooLong += "tooooolong";
    }
    throwable.expect(IllegalStateException.class);
    throwable.expectMessage("Failed to create temp file");
    tempFolder.newFile(tooLong, ".txt");
  }
}
