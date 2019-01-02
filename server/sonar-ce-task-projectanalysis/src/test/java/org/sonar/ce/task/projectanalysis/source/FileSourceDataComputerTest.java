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
package org.sonar.ce.task.projectanalysis.source;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepositoryImpl.LineHashesComputer;
import org.sonar.ce.task.projectanalysis.source.linereader.LineReader;
import org.sonar.core.hash.SourceHashComputer;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbFileSources;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileSourceDataComputerTest {
  private static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1).build();

  private SourceLinesRepository sourceLinesRepository = mock(SourceLinesRepository.class);
  private LineHashesComputer lineHashesComputer = mock(LineHashesComputer.class);
  private SourceLineReadersFactory sourceLineReadersFactory = mock(SourceLineReadersFactory.class);
  private SourceLinesHashRepository sourceLinesHashRepository = mock(SourceLinesHashRepository.class);

  private FileSourceDataComputer underTest = new FileSourceDataComputer(sourceLinesRepository, sourceLineReadersFactory, sourceLinesHashRepository);

  private FileSourceDataWarnings fileSourceDataWarnings = mock(FileSourceDataWarnings.class);
  private SourceLineReadersFactory.LineReaders lineReaders = mock(SourceLineReadersFactory.LineReaders.class);

  @Before
  public void before() {
    when(sourceLinesHashRepository.getLineHashesComputerToPersist(FILE)).thenReturn(lineHashesComputer);
    when(sourceLineReadersFactory.getLineReaders(FILE)).thenReturn(mock(SourceLineReadersFactory.LineReaders.class));
  }

  @Test
  public void compute_calls_read_for_each_line_and_passe_read_error_to_fileSourceDataWarnings() {
    int lineCount = 1 + new Random().nextInt(10);
    List<String> lines = IntStream.range(0, lineCount).mapToObj(i -> "line" + i).collect(toList());
    when(sourceLinesRepository.readLines(FILE)).thenReturn(CloseableIterator.from(lines.iterator()));
    when(sourceLineReadersFactory.getLineReaders(FILE)).thenReturn(lineReaders);
    when(sourceLinesHashRepository.getLineHashesComputerToPersist(FILE)).thenReturn(lineHashesComputer);
    // mock an implementation that will call the ReadErrorConsumer in order to verify that the provided consumer is
    // doing what we expect: pass readError to fileSourceDataWarnings
    int randomStartPoint = new Random().nextInt(500);
    doAnswer(new Answer() {
      int i = randomStartPoint;

      @Override
      public Object answer(InvocationOnMock invocation) {
        Consumer<LineReader.ReadError> readErrorConsumer = invocation.getArgument(1);
        readErrorConsumer.accept(new LineReader.ReadError(LineReader.Data.SYMBOLS, i++));
        return null;
      }
    }).when(lineReaders).read(any(), any());

    underTest.compute(FILE, fileSourceDataWarnings);

    ArgumentCaptor<DbFileSources.Line.Builder> lineBuilderCaptor = ArgumentCaptor.forClass(DbFileSources.Line.Builder.class);
    verify(lineReaders, times(lineCount)).read(lineBuilderCaptor.capture(), any());
    assertThat(lineBuilderCaptor.getAllValues())
      .extracting(DbFileSources.Line.Builder::getSource)
      .containsOnlyElementsOf(lines);
    assertThat(lineBuilderCaptor.getAllValues())
      .extracting(DbFileSources.Line.Builder::getLine)
      .containsExactly(IntStream.range(1, lineCount + 1).boxed().toArray(Integer[]::new));
    ArgumentCaptor<LineReader.ReadError> readErrorCaptor = ArgumentCaptor.forClass(LineReader.ReadError.class);
    verify(fileSourceDataWarnings, times(lineCount)).addWarning(same(FILE), readErrorCaptor.capture());
    assertThat(readErrorCaptor.getAllValues())
      .extracting(LineReader.ReadError::getLine)
      .containsExactly(IntStream.range(randomStartPoint, randomStartPoint + lineCount).boxed().toArray(Integer[]::new));
  }

  @Test
  public void compute_builds_data_object_from_lines() {
    int lineCount = 1 + new Random().nextInt(10);
    int randomStartPoint = new Random().nextInt(500);
    List<String> lines = IntStream.range(0, lineCount).mapToObj(i -> "line" + i).collect(toList());
    List<String> expectedLineHashes = IntStream.range(0, 1 + new Random().nextInt(12)).mapToObj(i -> "str_" + i).collect(toList());
    Changeset expectedChangeset = Changeset.newChangesetBuilder().setDate((long) new Random().nextInt(9_999)).build();
    String expectedSrcHash = computeSrcHash(lines);
    CloseableIterator<String> lineIterator = spy(CloseableIterator.from(lines.iterator()));
    DbFileSources.Data.Builder expectedLineDataBuilder = DbFileSources.Data.newBuilder();
    for (int i = 0; i < lines.size(); i++) {
      expectedLineDataBuilder.addLinesBuilder()
        .setSource(lines.get(i))
        .setLine(i + 1)
        // scmAuthor will be set with specific value by our mock implementation of LinesReaders.read()
        .setScmAuthor("reader_called_" + (randomStartPoint + i));
    }
    when(sourceLinesRepository.readLines(FILE)).thenReturn(lineIterator);
    when(sourceLineReadersFactory.getLineReaders(FILE)).thenReturn(lineReaders);
    when(sourceLinesHashRepository.getLineHashesComputerToPersist(FILE)).thenReturn(lineHashesComputer);
    when(lineHashesComputer.getResult()).thenReturn(expectedLineHashes);
    when(lineReaders.getLatestChangeWithRevision()).thenReturn(expectedChangeset);
    // mocked implementation of LineReader.read to ensure changes done by it to the lineBuilder argument actually end
    // up in the FileSourceDataComputer.Data object returned
    doAnswer(new Answer() {
      int i = 0;

      @Override
      public Object answer(InvocationOnMock invocation) {
        DbFileSources.Line.Builder lineBuilder = invocation.getArgument(0);
        lineBuilder.setScmAuthor("reader_called_" + (randomStartPoint + i++));
        return null;
      }
    }).when(lineReaders).read(any(), any());

    FileSourceDataComputer.Data data = underTest.compute(FILE, fileSourceDataWarnings);

    assertThat(data.getLineHashes()).isEqualTo(expectedLineHashes);
    assertThat(data.getSrcHash()).isEqualTo(expectedSrcHash);
    assertThat(data.getLatestChangeWithRevision()).isSameAs(expectedChangeset);
    assertThat(data.getLineData()).isEqualTo(expectedLineDataBuilder.build());

    verify(lineIterator).close();
    verify(lineReaders).close();
  }

  private static String computeSrcHash(List<String> lines) {
    SourceHashComputer computer = new SourceHashComputer();
    Iterator<String> iterator = lines.iterator();
    while (iterator.hasNext()) {
      computer.addLine(iterator.next(), iterator.hasNext());
    }
    return computer.getHash();
  }

}
