/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.batch.fs.internal;

import java.io.IOException;
import java.util.Collections;
import org.junit.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FilenamePredicateTest {
  @Test
  public void should_match_file_by_filename() throws IOException {
    String filename = "some name";
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.filename()).thenReturn(filename);

    assertThat(new FilenamePredicate(filename).apply(inputFile)).isTrue();
  }

  @Test
  public void should_not_match_file_by_different_filename() throws IOException {
    String filename = "some name";
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.filename()).thenReturn(filename + "x");

    assertThat(new FilenamePredicate(filename).apply(inputFile)).isFalse();
  }

  @Test
  public void should_find_matching_file_in_index() throws IOException {
    String filename = "some name";
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.filename()).thenReturn(filename);

    FileSystem.Index index = mock(FileSystem.Index.class);
    when(index.getFilesByName(filename)).thenReturn(Collections.singleton(inputFile));

    assertThat(new FilenamePredicate(filename).get(index)).containsOnly(inputFile);
  }

}
