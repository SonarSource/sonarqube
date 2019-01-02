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

import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class JUnitTempFolderTest {

  @Test
  public void apply() throws Throwable {
    JUnitTempFolder temp = new JUnitTempFolder();
    temp.before();
    File dir1 = temp.newDir();
    assertThat(dir1).isDirectory().exists();

    File dir2 = temp.newDir("foo");
    assertThat(dir2).isDirectory().exists();

    File file1 = temp.newFile();
    assertThat(file1).isFile().exists();

    File file2 = temp.newFile("foo", "txt");
    assertThat(file2).isFile().exists();

    temp.after();
    assertThat(dir1).doesNotExist();
    assertThat(dir2).doesNotExist();
    assertThat(file1).doesNotExist();
    assertThat(file2).doesNotExist();
  }

}
