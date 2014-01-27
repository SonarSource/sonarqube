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
package org.sonar.api.scan.filesystem.internal;

import org.sonar.api.scan.filesystem.InputFile;

import com.google.common.base.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.PathUtils;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultInputFileTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_attributes() throws IOException {
    File file = temp.newFile();
    InputFile input = new InputFileBuilder(file, Charsets.UTF_8, "src/main/java/Foo.java")
      .attribute("foo", "bar")
      .type(InputFile.TYPE_TEST)
      .hash("ABC")
      .status(InputFile.STATUS_ADDED)
      .language("java")
      .build();

    assertThat(input.attributes()).hasSize(5);
    assertThat(input.attribute("unknown")).isNull();
    assertThat(input.attribute("foo")).isEqualTo("bar");
    assertThat(input.attribute(InputFile.ATTRIBUTE_TYPE)).isEqualTo(InputFile.TYPE_TEST);
    assertThat(input.attribute(DefaultInputFile.ATTRIBUTE_HASH)).isEqualTo("ABC");
    assertThat(input.attribute(InputFile.ATTRIBUTE_LANGUAGE)).isEqualTo("java");
    assertThat(input.attribute(InputFile.ATTRIBUTE_STATUS)).isEqualTo(InputFile.STATUS_ADDED);

    assertThat(input.has(InputFile.ATTRIBUTE_LANGUAGE, "java")).isTrue();
    assertThat(input.has(InputFile.ATTRIBUTE_LANGUAGE, "php")).isFalse();
    assertThat(input.has("unknown", "xxx")).isFalse();
  }

  @Test
  public void test_file() throws Exception {
    File sourceDir = temp.newFolder();
    File file = temp.newFile("Foo.java");
    InputFile input = new InputFileBuilder(file, Charsets.UTF_8, "src/main/java/Foo.java")
      .sourceDir(sourceDir)
      .build();

    assertThat(input.name()).isEqualTo("Foo.java");
    assertThat(input.file()).isEqualTo(file);
    assertThat(input.attribute(DefaultInputFile.ATTRIBUTE_SOURCEDIR_PATH)).isEqualTo(FilenameUtils.separatorsToUnix(sourceDir.getAbsolutePath()));
    assertThat(input.path()).isEqualTo("src/main/java/Foo.java");
    assertThat(input.absolutePath()).isEqualTo(PathUtils.canonicalPath(file));
  }

  @Test
  public void test_equals_and_hashCode() throws Exception {
    File file1 = temp.newFile();
    InputFile input1 = new InputFileBuilder(file1, Charsets.UTF_8, "src/main/java/Foo.java").build();
    InputFile input1a = new InputFileBuilder(file1, Charsets.UTF_8, "src/main/java/Foo.java").build();
    InputFile input2 = new InputFileBuilder(temp.newFile(), Charsets.UTF_8, "src/main/java/Bar.java").build();

    assertThat(input1.equals(input1)).isTrue();
    assertThat(input1.equals(input1a)).isTrue();
    assertThat(input1.equals(input2)).isFalse();
    assertThat(input1.hashCode()).isEqualTo(input1.hashCode());
    assertThat(input1.hashCode()).isEqualTo(input1a.hashCode());
  }

  @Test
  public void test_toString() throws Exception {
    File file1 = temp.newFile();
    InputFile input = new InputFileBuilder(file1, Charsets.UTF_8, "src/main/java/Foo.java").type(InputFile.TYPE_TEST).build();
    assertThat(input.toString()).isEqualTo("[src/main/java/Foo.java,TEST]");
  }
}
