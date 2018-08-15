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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationRepositoryRule;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepositoryRule;
import org.sonar.ce.task.projectanalysis.source.linereader.LineReader;
import org.sonar.ce.task.projectanalysis.source.linereader.ScmLineReader;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SourceLineReadersFactoryTest {
  private static final int FILE1_REF = 3;
  private static final String PROJECT_UUID = "PROJECT";
  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String FILE1_UUID = "FILE1";
  private static final long NOW = 123456789L;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public ScmInfoRepositoryRule scmInfoRepository = new ScmInfoRepositoryRule();
  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);
  private NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);

  private SourceLineReadersFactory underTest;

  @Before
  public void setUp() {
    underTest = new SourceLineReadersFactory(reportReader, scmInfoRepository, duplicationRepository, newLinesRepository);
  }

  @Test
  public void should_create_readers() {
    initBasicReport(10);
    SourceLineReadersFactory.LineReaders lineReaders = underTest.getLineReaders(fileComponent());

    assertThat(lineReaders).isNotNull();
    assertThat(lineReaders.closeables).hasSize(3);
    assertThat(lineReaders.readers).hasSize(5);
  }

  @Test
  public void line_readers_should_close_all_closeables() {
    LineReader r1 = mock(LineReader.class);
    LineReader r2 = mock(LineReader.class);
    CloseableIterator c1 = mock(CloseableIterator.class);
    CloseableIterator c2 = mock(CloseableIterator.class);

    SourceLineReadersFactory.LineReaders lineReaders = new SourceLineReadersFactory.LineReaders(Arrays.asList(r1, r2), null, Arrays.asList(c1, c2));
    lineReaders.close();

    verify(c1).close();
    verify(c2).close();
    verifyNoMoreInteractions(c1, c2);
    verifyZeroInteractions(r1, r2);
  }

  @Test
  public void line_readers_should_call_all_readers() {
    LineReader r1 = mock(LineReader.class);
    LineReader r2 = mock(LineReader.class);
    CloseableIterator c1 = mock(CloseableIterator.class);
    CloseableIterator c2 = mock(CloseableIterator.class);

    SourceLineReadersFactory.LineReaders lineReaders = new SourceLineReadersFactory.LineReaders(Arrays.asList(r1, r2), null, Arrays.asList(c1, c2));
    DbFileSources.Line.Builder builder = DbFileSources.Line.newBuilder();
    lineReaders.read(builder);

    verify(r1).read(builder);
    verify(r2).read(builder);
    verifyNoMoreInteractions(r1, r2);
    verifyZeroInteractions(c1, c2);
  }

  @Test
  public void should_delegate_latest_changeset() {
    ScmLineReader scmLineReader = mock(ScmLineReader.class);
    Changeset changeset = Changeset.newChangesetBuilder().setDate(0L).build();
    when(scmLineReader.getLatestChangeWithRevision()).thenReturn(changeset);
    SourceLineReadersFactory.LineReaders lineReaders = new SourceLineReadersFactory.LineReaders(Collections.emptyList(), scmLineReader, Collections.emptyList());
    assertThat(lineReaders.getLatestChangeWithRevision()).isEqualTo(changeset);
  }

  @Test
  public void should_not_delegate_latest_changeset() {
    SourceLineReadersFactory.LineReaders lineReaders = new SourceLineReadersFactory.LineReaders(Collections.emptyList(), null, Collections.emptyList());
    assertThat(lineReaders.getLatestChangeWithRevision()).isNull();
  }

  private Component fileComponent() {
    return ReportComponent.builder(Component.Type.FILE, FILE1_REF).build();
  }

  private void initBasicReport(int numberOfLines) {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("MODULE").setKey("MODULE_KEY").addChildren(
        ReportComponent.builder(Component.Type.FILE, FILE1_REF).setUuid(FILE1_UUID).setKey("MODULE_KEY:src/Foo.java")
          .setFileAttributes(new FileAttributes(false, null, numberOfLines)).build())
        .build())
      .build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ScannerReport.Component.ComponentType.PROJECT)
      .addChildRef(2)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(2)
      .setType(ScannerReport.Component.ComponentType.MODULE)
      .addChildRef(FILE1_REF)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(FILE1_REF)
      .setType(ScannerReport.Component.ComponentType.FILE)
      .setLines(numberOfLines)
      .build());

    // for (int i = 1; i <= numberOfLines; i++) {
    // fileSourceRepository.addLine(FILE1_REF, "line" + i);
    // }
  }

}
