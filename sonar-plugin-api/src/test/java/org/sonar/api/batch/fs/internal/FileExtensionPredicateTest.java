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
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.batch.fs.internal.FileExtensionPredicate.getExtension;

public class FileExtensionPredicateTest {

  @Test
  public void should_match_correct_extension() throws IOException {
    FileExtensionPredicate predicate = new FileExtensionPredicate("bat");
    assertThat(predicate.apply(mockWithName("prog.bat"))).isTrue();
    assertThat(predicate.apply(mockWithName("prog.bat.bat"))).isTrue();
  }

  @Test
  public void should_not_match_incorrect_extension() throws IOException {
    FileExtensionPredicate predicate = new FileExtensionPredicate("bat");
    assertThat(predicate.apply(mockWithName("prog.batt"))).isFalse();
    assertThat(predicate.apply(mockWithName("prog.abat"))).isFalse();
    assertThat(predicate.apply(mockWithName("prog."))).isFalse();
    assertThat(predicate.apply(mockWithName("prog.bat."))).isFalse();
    assertThat(predicate.apply(mockWithName("prog.bat.batt"))).isFalse();
    assertThat(predicate.apply(mockWithName("prog"))).isFalse();
  }

  @Test
  public void should_match_correct_extension_case_insensitively() throws IOException {
    FileExtensionPredicate predicate = new FileExtensionPredicate("jAVa");
    assertThat(predicate.apply(mockWithName("Program.java"))).isTrue();
    assertThat(predicate.apply(mockWithName("Program.JAVA"))).isTrue();
    assertThat(predicate.apply(mockWithName("Program.Java"))).isTrue();
    assertThat(predicate.apply(mockWithName("Program.JaVa"))).isTrue();
  }

  @Test
  public void test_empty_extension() {
    assertThat(getExtension("prog")).isEmpty();
    assertThat(getExtension("prog.")).isEmpty();
    assertThat(getExtension(".")).isEmpty();
  }

  private InputFile mockWithName(String filename) throws IOException {
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.filename()).thenReturn(filename);
    return inputFile;
  }
}
