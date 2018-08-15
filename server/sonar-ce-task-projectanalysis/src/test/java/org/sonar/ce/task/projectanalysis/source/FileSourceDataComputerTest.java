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
package org.sonar.ce.task.projectanalysis.source;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepositoryImpl.LineHashesComputer;
import org.sonar.ce.task.projectanalysis.source.linereader.LineReader;
import org.sonar.ce.task.projectanalysis.source.linereader.ScmLineReader;
import org.sonar.core.util.CloseableIterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FileSourceDataComputerTest {
  private static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1).build();

  private SourceLinesRepository sourceLinesRepository = mock(SourceLinesRepository.class);
  private LineHashesComputer lineHashesComputer = mock(LineHashesComputer.class);
  private SourceLineReadersFactory sourceLineReadersFactory = mock(SourceLineReadersFactory.class);
  private SourceLinesHashRepository sourceLinesHashRepository = mock(SourceLinesHashRepository.class);
  private ScmLineReader scmLineReader = mock(ScmLineReader.class);
  private CloseableIterator closeableIterator = mock(CloseableIterator.class);

  private FileSourceDataComputer fileSourceDataComputer;

  @Before
  public void before() {
    when(sourceLinesHashRepository.getLineHashesComputerToPersist(FILE)).thenReturn(lineHashesComputer);
    LineReader reader = line -> line.setHighlighting("h-" + line.getLine());
    SourceLineReadersFactory.LineReaders lineReaders = new SourceLineReadersFactory.LineReaders(Collections.singletonList(reader), scmLineReader,
      Collections.singletonList(closeableIterator));
    when(sourceLineReadersFactory.getLineReaders(FILE)).thenReturn(lineReaders);

    fileSourceDataComputer = new FileSourceDataComputer(sourceLinesRepository, sourceLineReadersFactory, sourceLinesHashRepository);
  }

  @Test
  public void compute_one_line() {
    List<String> lineHashes = Collections.singletonList("lineHash");
    when(sourceLinesRepository.readLines(FILE)).thenReturn(CloseableIterator.from(Collections.singletonList("line1").iterator()));
    when(lineHashesComputer.getResult()).thenReturn(lineHashes);

    FileSourceDataComputer.Data data = fileSourceDataComputer.compute(FILE);

    assertThat(data.getLineHashes()).isEqualTo(lineHashes);
    assertThat(data.getSrcHash()).isEqualTo("137f72c3708c6bd0de00a0e5a69c699b");
    assertThat(data.getLineData().getLinesList()).hasSize(1);
    assertThat(data.getLineData().getLines(0).getHighlighting()).isEqualTo("h-1");

    verify(lineHashesComputer).addLine("line1");
    verify(lineHashesComputer).getResult();
    verify(closeableIterator).close();
    verifyNoMoreInteractions(lineHashesComputer);
  }

  @Test
  public void compute_two_lines() {
    List<String> lineHashes = Arrays.asList("137f72c3708c6bd0de00a0e5a69c699b", "e6251bcf1a7dc3ba5e7933e325bbe605");
    when(sourceLinesRepository.readLines(FILE)).thenReturn(CloseableIterator.from(Arrays.asList("line1", "line2").iterator()));
    when(lineHashesComputer.getResult()).thenReturn(lineHashes);

    FileSourceDataComputer.Data data = fileSourceDataComputer.compute(FILE);

    assertThat(data.getLineHashes()).isEqualTo(lineHashes);
    assertThat(data.getSrcHash()).isEqualTo("ee5a58024a155466b43bc559d953e018");
    assertThat(data.getLineData().getLinesList()).hasSize(2);
    assertThat(data.getLineData().getLines(0).getHighlighting()).isEqualTo("h-1");
    assertThat(data.getLineData().getLines(1).getHighlighting()).isEqualTo("h-2");

    verify(lineHashesComputer).addLine("line1");
    verify(lineHashesComputer).addLine("line2");
    verify(lineHashesComputer).getResult();
    verify(closeableIterator).close();
    verifyNoMoreInteractions(lineHashesComputer);
  }

}
