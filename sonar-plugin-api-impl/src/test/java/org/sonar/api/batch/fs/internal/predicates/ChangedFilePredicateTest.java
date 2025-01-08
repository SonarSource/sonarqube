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
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChangedFilePredicateTest {

  private final FilePredicate predicate = mock(FilePredicate.class);
  private final InputFile inputFile = mock(InputFile.class);

  private final ChangedFilePredicate underTest = new ChangedFilePredicate(predicate);

  @Test
  public void apply_when_file_is_changed_and_predicate_is_true() {
    when(inputFile.status()).thenReturn(InputFile.Status.CHANGED);
    when(predicate.apply(inputFile)).thenReturn(true);

    Assertions.assertThat(underTest.apply(inputFile)).isTrue();

    verify(predicate, times(1)).apply(any());
    verify(inputFile, times(1)).status();
  }

  @Test
  public void apply_when_file_is_added_and_predicate_is_true() {
    when(inputFile.status()).thenReturn(InputFile.Status.ADDED);
    when(predicate.apply(inputFile)).thenReturn(true);

    Assertions.assertThat(underTest.apply(inputFile)).isTrue();

    verify(predicate, times(1)).apply(any());
    verify(inputFile, times(1)).status();
  }

  @Test
  public void do_not_apply_when_file_is_same_and_predicate_is_true() {
    when(inputFile.status()).thenReturn(InputFile.Status.SAME);
    when(predicate.apply(inputFile)).thenReturn(true);

    Assertions.assertThat(underTest.apply(inputFile)).isFalse();

    verify(predicate, times(1)).apply(any());
    verify(inputFile, times(1)).status();
  }

  @Test
  public void predicate_is_evaluated_before_file_status() {
    when(predicate.apply(inputFile)).thenReturn(false);

    Assertions.assertThat(underTest.apply(inputFile)).isFalse();

    verify(inputFile, never()).status();
  }

  @Test
  public void do_not_apply_when_file_is_same_and_predicate_is_false() {
    when(inputFile.status()).thenReturn(InputFile.Status.SAME);
    when(predicate.apply(inputFile)).thenReturn(true);

    Assertions.assertThat(underTest.apply(inputFile)).isFalse();

    verify(predicate, times(1)).apply(any());
    verify(inputFile, times(1)).status();
  }

}
