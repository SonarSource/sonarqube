/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTempFolderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void createTempFolderAndFile() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder underTest = new DefaultTempFolder(rootTempFolder);
    File dir = underTest.newDir();
    assertThat(dir).exists().isDirectory();
    File file = underTest.newFile();
    assertThat(file).exists().isFile();

    new TempFolderCleaner(underTest).stop();
    assertThat(rootTempFolder).doesNotExist();
  }

  @Test
  public void createTempFolderWithName() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder underTest = new DefaultTempFolder(rootTempFolder);
    File dir = underTest.newDir("sample");
    assertThat(dir).exists().isDirectory();
    assertThat(new File(rootTempFolder, "sample")).isEqualTo(dir);

    new TempFolderCleaner(underTest).stop();
    assertThat(rootTempFolder).doesNotExist();
  }

  @Test
  public void newDir_throws_ISE_if_name_is_not_valid() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder underTest = new DefaultTempFolder(rootTempFolder);
    String tooLong = "tooooolong";
    for (int i = 0; i < 50; i++) {
      tooLong += "tooooolong";
    }

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Failed to create temp directory");

    underTest.newDir(tooLong);
  }

  @Test
  public void newFile_throws_ISE_if_name_is_not_valid() throws Exception {
    File rootTempFolder = temp.newFolder();
    DefaultTempFolder underTest = new DefaultTempFolder(rootTempFolder);
    String tooLong = "tooooolong";
    for (int i = 0; i < 50; i++) {
      tooLong += "tooooolong";
    }

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Failed to create temp file");

    underTest.newFile(tooLong, ".txt");
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

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }
}
