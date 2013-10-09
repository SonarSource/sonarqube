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
package org.sonar.api.scan.filesystem;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.scan.filesystem.internal.InputFileBuilder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class InputFilesTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_toFiles() throws Exception {
    File file1 = temp.newFile();
    File file2 = temp.newFile();
    InputFile input1 = new InputFileBuilder(file1, "src/main/java/Foo.java").build();
    InputFile input2 = new InputFileBuilder(file2, "src/main/java/Bar.java").build();

    assertThat(InputFiles.toFiles(Lists.newArrayList(input1, input2))).containsOnly(file1, file2);
  }
}
