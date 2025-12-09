/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.source;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.core.hash.SourceHashComputer;
import org.sonar.core.util.CloseableIterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SourceHashRepositoryImplTest {
  private static final int FILE_REF = 112;
  private static final String FILE_KEY = "file key";
  private static final Component FILE_COMPONENT = ReportComponent.builder(Component.Type.FILE, FILE_REF).setKey(FILE_KEY).build();
  private static final String[] SOME_LINES = {"line 1", "line after line 1", "line 4 minus 1", "line 100 by 10"};

  @RegisterExtension
  private final SourceLinesRepositoryRule sourceLinesRepository = new SourceLinesRepositoryRule();

  private final SourceLinesRepository mockedSourceLinesRepository = mock(SourceLinesRepository.class);

  private final SourceHashRepositoryImpl underTest = new SourceHashRepositoryImpl(sourceLinesRepository);
  private final SourceHashRepositoryImpl mockedUnderTest = new SourceHashRepositoryImpl(mockedSourceLinesRepository);

  @Test
  void getRawSourceHash_throws_NPE_if_Component_argument_is_null() {
    assertThatThrownBy(() -> underTest.getRawSourceHash(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Specified component can not be null");
  }

  @ParameterizedTest
  @MethodSource("componentsOfAllTypesButFile")
  void getRawSourceHash_throws_IAE_if_Component_argument_is_not_FILE(Component component) {
    assertThatThrownBy(() -> underTest.getRawSourceHash(component))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("File source information can only be retrieved from FILE components (got " + component.getType() + ")");
  }

  private static Object[][] componentsOfAllTypesButFile() {
    return FluentIterable.from(Arrays.asList(Component.Type.values()))
      .filter(new Predicate<Component.Type>() {
        @Override
        public boolean apply(@Nullable Component.Type input) {
          return input != Component.Type.FILE;
        }
      })
      .transform(new Function<Component.Type, Component>() {
        @Nullable
        @Override
        public Component apply(Component.Type input) {
          if (input.isReportType()) {
            return ReportComponent.builder(input, input.hashCode())
              .setKey(input.name() + "_key")
              .build();
          } else if (input.isViewsType()) {
            return ViewsComponent.builder(input, input.name() + "_key")
              .build();
          } else {
            throw new IllegalArgumentException("Unsupported type " + input);
          }
        }
      }).transform(new Function<Component, Component[]>() {
        @Nullable
        @Override
        public Component[] apply(@Nullable Component input) {
          return new Component[] {input};
        }
      }).toArray(Component[].class);

  }

  @Test
  void getRawSourceHash_returns_hash_of_lines_from_SourceLinesRepository() {
    sourceLinesRepository.addLines(FILE_REF, SOME_LINES);

    String rawSourceHash = underTest.getRawSourceHash(FILE_COMPONENT);

    SourceHashComputer sourceHashComputer = new SourceHashComputer();
    for (int i = 0; i < SOME_LINES.length; i++) {
      sourceHashComputer.addLine(SOME_LINES[i], i < (SOME_LINES.length - 1));
    }

    assertThat(rawSourceHash).isEqualTo(sourceHashComputer.getHash());
  }

  @Test
  void getRawSourceHash_reads_lines_from_SourceLinesRepository_only_the_first_time() {
    when(mockedSourceLinesRepository.readLines(FILE_COMPONENT)).thenReturn(CloseableIterator.from(Arrays.asList(SOME_LINES).iterator()));

    String rawSourceHash = mockedUnderTest.getRawSourceHash(FILE_COMPONENT);
    String rawSourceHash1 = mockedUnderTest.getRawSourceHash(FILE_COMPONENT);

    assertThat(rawSourceHash).isSameAs(rawSourceHash1);
    verify(mockedSourceLinesRepository, times(1)).readLines(FILE_COMPONENT);
  }

  @Test
  void getRawSourceHash_let_exception_go_through() {
    IllegalArgumentException thrown = new IllegalArgumentException("this IAE will cause the hash computation to fail");
    when(mockedSourceLinesRepository.readLines(FILE_COMPONENT)).thenThrow(thrown);

    assertThatThrownBy(() -> mockedUnderTest.getRawSourceHash(FILE_COMPONENT))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(thrown.getMessage());
  }
}
