/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NonHiddenFilesPredicateTest {

  private final InputFile inputFile = mock(InputFile.class);

  private final NonHiddenFilesPredicate underTest = new NonHiddenFilesPredicate();

  @Test
  public void apply_when_file_is_not_hidden() {
    when(inputFile.isHidden()).thenReturn(false);

    Assertions.assertThat(underTest.apply(inputFile)).isTrue();

    verify(inputFile).isHidden();
  }

  @Test
  public void do_not_apply_when_file_is_hidden() {
    when(inputFile.isHidden()).thenReturn(true);

    Assertions.assertThat(underTest.apply(inputFile)).isFalse();

    verify(inputFile).isHidden();
  }
}
