/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.api.impl.utils;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultTempFolderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private static StringBuilder tooLong = new StringBuilder("tooooolong");

  @BeforeClass
  public static void setUp() {
    for (int i = 0; i < 50; i++) {
      tooLong.append("tooooolong");
    }
  }

  @Test
  public void createTempFolderAndFile() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder underTest = new DefaultTempFolder(rootTempFolder);
    File dir = underTest.newDir();
    assertThat(dir).exists().isDirectory();
    File file = underTest.newFile();
    assertThat(file).exists().isFile();

    underTest.clean();
    assertThat(rootTempFolder).doesNotExist();
  }

  @Test
  public void createTempFolderWithName() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder underTest = new DefaultTempFolder(rootTempFolder);
    File dir = underTest.newDir("sample");
    assertThat(dir).exists().isDirectory();
    assertThat(new File(rootTempFolder, "sample")).isEqualTo(dir);

    underTest.clean();
    assertThat(rootTempFolder).doesNotExist();
  }

  @Test
  public void newDir_throws_ISE_if_name_is_not_valid() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder underTest = new DefaultTempFolder(rootTempFolder);

    assertThatThrownBy(() -> underTest.newDir(tooLong.toString()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to create temp directory");
  }

  @Test
  public void newFile_throws_ISE_if_name_is_not_valid() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder underTest = new DefaultTempFolder(rootTempFolder);

    assertThatThrownBy(() -> underTest.newFile(tooLong.toString(), ".txt"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to create temp file");
  }

  @Test
  public void clean_deletes_non_empty_directory() throws Exception {
    File dir = temp.newFolder();
    FileUtils.touch(new File(dir, "foo.txt"));

    DefaultTempFolder underTest = new DefaultTempFolder(dir);
    underTest.clean();

    assertThat(dir).doesNotExist();
  }

  @Test
  public void clean_does_not_fail_if_directory_has_already_been_deleted() throws Exception {
    File dir = temp.newFolder();

    DefaultTempFolder underTest = new DefaultTempFolder(dir);
    underTest.clean();
    assertThat(dir).doesNotExist();

    // second call does not fail, nor log ERROR logs
    underTest.clean();

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }
}
