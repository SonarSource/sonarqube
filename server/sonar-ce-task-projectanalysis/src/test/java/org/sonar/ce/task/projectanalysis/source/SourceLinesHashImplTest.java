/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.List;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.hash.SourceLineHashesComputer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class SourceLinesHashImplTest {
  private static final String FILE_UUID = "FILE_UUID";
  private static final String FILE_KEY = "FILE_KEY";

  @Rule
  public SourceLinesRepositoryRule sourceLinesRepository = new SourceLinesRepositoryRule();
  public SignificantCodeRepository significantCodeRepository = mock(SignificantCodeRepository.class);
  public SourceLinesHashCache cache = mock(SourceLinesHashCache.class);
  public DbLineHashVersion dbLineHashVersion = mock(DbLineHashVersion.class);

  private SourceLinesHashRepositoryImpl underTest = new SourceLinesHashRepositoryImpl(sourceLinesRepository, significantCodeRepository, cache, dbLineHashVersion);

  @Test
  public void should_generate_correct_version_of_line_hashes() {
    Component component = createComponent(1);
    when(cache.computeIfAbsent(eq(component), any()))
      .thenAnswer(invocation -> invocation.<Function<Component, List<String>>>getArgument(1).apply(component));
    when(dbLineHashVersion.hasLineHashesWithoutSignificantCode(component)).thenReturn(true);
    sourceLinesRepository.addLines(1, "line1", "line2");

    List<String> lineHashes = underTest.getLineHashesMatchingDBVersion(component);

    SourceLineHashesComputer expectedHashesComputer = new SourceLineHashesComputer();
    expectedHashesComputer.addLine("line1");
    expectedHashesComputer.addLine("line2");
    assertThat(lineHashes).containsExactlyElementsOf(expectedHashesComputer.getLineHashes());
    verify(cache).computeIfAbsent(eq(component), any());
    verify(significantCodeRepository, never()).getRangesPerLine(component);
  }

  private static Component createComponent(int ref) {
    return builder(Component.Type.FILE, ref)
      .setKey(FILE_KEY)
      .setUuid(FILE_UUID)
      .build();
  }
}
