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
package org.sonar.api.batch.fs.internal.predicates;

import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RelativePathPredicateTest {
  @Test
  public void returns_false_when_path_is_invalid() {
    RelativePathPredicate predicate = new RelativePathPredicate("..");
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.relativePath()).thenReturn("path");
    assertThat(predicate.apply(inputFile)).isFalse();
  }

  @Test
  public void returns_true_if_matches() {
    RelativePathPredicate predicate = new RelativePathPredicate("path");
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.relativePath()).thenReturn("path");
    assertThat(predicate.apply(inputFile)).isTrue();
  }

  @Test
  public void returns_false_if_doesnt_match() {
    RelativePathPredicate predicate = new RelativePathPredicate("path1");
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.relativePath()).thenReturn("path2");
    assertThat(predicate.apply(inputFile)).isFalse();
  }
}
