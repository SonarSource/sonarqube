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

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationRepository;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.ce.task.projectanalysis.source.linereader.CoverageLineReader;
import org.sonar.ce.task.projectanalysis.source.linereader.DuplicationLineReader;
import org.sonar.ce.task.projectanalysis.source.linereader.HighlightingLineReader;
import org.sonar.ce.task.projectanalysis.source.linereader.IsNewLineReader;
import org.sonar.ce.task.projectanalysis.source.linereader.LineReader;
import org.sonar.ce.task.projectanalysis.source.linereader.RangeOffsetConverter;
import org.sonar.ce.task.projectanalysis.source.linereader.ScmLineReader;
import org.sonar.ce.task.projectanalysis.source.linereader.SymbolsLineReader;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.scanner.protocol.output.ScannerReport;

public class SourceLineReadersFactory {
  private final BatchReportReader reportReader;
  private final ScmInfoRepository scmInfoRepository;
  private final DuplicationRepository duplicationRepository;
  private final NewLinesRepository newLinesRepository;

  public SourceLineReadersFactory(BatchReportReader reportReader, ScmInfoRepository scmInfoRepository, DuplicationRepository duplicationRepository,
    NewLinesRepository newLinesRepository) {
    this.reportReader = reportReader;
    this.scmInfoRepository = scmInfoRepository;
    this.duplicationRepository = duplicationRepository;
    this.newLinesRepository = newLinesRepository;
  }

  public LineReaders getLineReaders(Component component) {
    List<LineReader> readers = new ArrayList<>();
    List<CloseableIterator<?>> closeables = new ArrayList<>();
    ScmLineReader scmLineReader = null;

    int componentRef = component.getReportAttributes().getRef();
    CloseableIterator<ScannerReport.LineCoverage> coverageIt = reportReader.readComponentCoverage(componentRef);
    closeables.add(coverageIt);
    readers.add(new CoverageLineReader(coverageIt));

    Optional<ScmInfo> scmInfoOptional = scmInfoRepository.getScmInfo(component);
    if (scmInfoOptional.isPresent()) {
      scmLineReader = new ScmLineReader(scmInfoOptional.get());
      readers.add(scmLineReader);
    }

    RangeOffsetConverter rangeOffsetConverter = new RangeOffsetConverter();
    CloseableIterator<ScannerReport.SyntaxHighlightingRule> highlightingIt = reportReader.readComponentSyntaxHighlighting(componentRef);
    closeables.add(highlightingIt);
    readers.add(new HighlightingLineReader(component, highlightingIt, rangeOffsetConverter));

    CloseableIterator<ScannerReport.Symbol> symbolsIt = reportReader.readComponentSymbols(componentRef);
    closeables.add(symbolsIt);
    readers.add(new SymbolsLineReader(component, symbolsIt, rangeOffsetConverter));
    readers.add(new DuplicationLineReader(duplicationRepository.getDuplications(component)));
    readers.add(new IsNewLineReader(newLinesRepository, component));

    return new LineReadersImpl(readers, scmLineReader, closeables);
  }

  interface LineReaders extends AutoCloseable {
    void read(DbFileSources.Line.Builder lineBuilder, Consumer<LineReader.ReadError> readErrorConsumer);

    @CheckForNull
    Changeset getLatestChangeWithRevision();

    @Override
    void close();
  }

  @VisibleForTesting
  static final class LineReadersImpl implements LineReaders {
    final List<LineReader> readers;
    @Nullable
    final ScmLineReader scmLineReader;
    final List<CloseableIterator<?>> closeables;

    LineReadersImpl(List<LineReader> readers, @Nullable ScmLineReader scmLineReader, List<CloseableIterator<?>> closeables) {
      this.readers = readers;
      this.scmLineReader = scmLineReader;
      this.closeables = closeables;
    }

    @Override
    public void close() {
      for (CloseableIterator<?> reportIterator : closeables) {
        reportIterator.close();
      }
    }

    public void read(DbFileSources.Line.Builder lineBuilder, Consumer<LineReader.ReadError> readErrorConsumer) {
      for (LineReader r : readers) {
        r.read(lineBuilder)
          .ifPresent(readErrorConsumer);
      }
    }

    @Override
    @CheckForNull
    public Changeset getLatestChangeWithRevision() {
      return scmLineReader == null ? null : scmLineReader.getLatestChangeWithRevision();
    }
  }

}
