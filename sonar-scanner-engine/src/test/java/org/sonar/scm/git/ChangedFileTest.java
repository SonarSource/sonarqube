/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scm.git;

import java.nio.file.Path;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.SensorStrategy;

import static org.apache.commons.lang.RandomStringUtils.random;
import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class ChangedFileTest {

  @Test
  public void test_unMovedFile() {
    Path absolutePath = Path.of("/absolutePath");
    ChangedFile changedFile = ChangedFile.of(absolutePath);

    assertThat(changedFile.getAbsolutFilePath()).isSameAs(absolutePath);
    assertThat(changedFile.getOldRelativeFilePathReference()).isNull();
    assertThat(changedFile.isMovedFile()).isFalse();
  }

  @Test
  public void test_movedFile() {
    Path absolutePath = Path.of("/absolutePath");
    ChangedFile changedFile = ChangedFile.of(absolutePath, "/oldRelativePath");

    assertThat(changedFile.getAbsolutFilePath()).isSameAs(absolutePath);
    assertThat(changedFile.getOldRelativeFilePathReference()).isEqualTo("/oldRelativePath");
    assertThat(changedFile.isMovedFile()).isTrue();
  }

  @Test
  public void test_equalsAndHashCode() {
    Path absolutePath = Path.of("/absolutePath");
    ChangedFile changedFile1 = ChangedFile.of(absolutePath, "/oldRelativePath");
    ChangedFile changedFile2 = ChangedFile.of(absolutePath, "/oldRelativePath");

    assertThat(changedFile1).isEqualTo(changedFile2).hasSameHashCodeAs(changedFile2);
  }

  @Test
  public void test_changed_file_is_created_properly_from_default_input_file() {
    String oldRelativeReference = "old/relative/path";
    Path path = Path.of("absolute/path");
    DefaultInputFile file = composeDefaultInputFile(path, oldRelativeReference);
    ChangedFile changedFile = ChangedFile.of(file);

    assertThat(changedFile.getAbsolutFilePath()).isEqualTo(path);
    assertThat(changedFile.isMovedFile()).isTrue();
    assertThat(changedFile.getOldRelativeFilePathReference()).isEqualTo(oldRelativeReference);
  }

  private DefaultInputFile composeDefaultInputFile(Path path, String oldRelativeReference) {
    DefaultIndexedFile indexedFile = composeDefaultIndexFile(path, oldRelativeReference);
    return new DefaultInputFile(indexedFile, f -> f.setPublished(true), f -> {
    });
  }

  private DefaultIndexedFile composeDefaultIndexFile(Path path, String oldRelativePath) {
    return new DefaultIndexedFile(
      path,
      random(5),
      random(5),
      random(5),
      InputFile.Type.MAIN,
      random(5),
      Integer.parseInt(randomNumeric(5)),
      new SensorStrategy(),
      oldRelativePath);
  }

}
