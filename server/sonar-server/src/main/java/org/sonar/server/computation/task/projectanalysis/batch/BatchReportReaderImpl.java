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
package org.sonar.server.computation.task.projectanalysis.batch;

import com.google.common.base.Throwables;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.LineReaderIterator;
import org.sonar.scanner.protocol.output.ScannerReport;

public class BatchReportReaderImpl implements BatchReportReader {

  private final BatchReportDirectoryHolder batchReportDirectoryHolder;
  private org.sonar.scanner.protocol.output.ScannerReportReader delegate;
  // caching of metadata which are read often
  private ScannerReport.Metadata metadata;

  public BatchReportReaderImpl(BatchReportDirectoryHolder batchReportDirectoryHolder) {
    this.batchReportDirectoryHolder = batchReportDirectoryHolder;
  }

  private void ensureInitialized() {
    if (this.delegate == null) {
      this.delegate = new org.sonar.scanner.protocol.output.ScannerReportReader(batchReportDirectoryHolder.getDirectory());
    }
  }

  @Override
  public ScannerReport.Metadata readMetadata() {
    ensureInitialized();
    if (this.metadata == null) {
      this.metadata = delegate.readMetadata();
    }
    return this.metadata;
  }

  @Override
  public CloseableIterator<String> readScannerLogs() {
    ensureInitialized();
    File file = delegate.getFileStructure().analysisLog();
    if (!file.exists()) {
      return CloseableIterator.emptyCloseableIterator();
    }
    try {
      InputStreamReader reader = new InputStreamReader(FileUtils.openInputStream(file), StandardCharsets.UTF_8);
      return new LineReaderIterator(reader);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to open file " + file, e);
    }
  }

  @Override
  public CloseableIterator<ScannerReport.ActiveRule> readActiveRules() {
    ensureInitialized();
    return delegate.readActiveRules();
  }

  @Override
  public CloseableIterator<ScannerReport.Measure> readComponentMeasures(int componentRef) {
    ensureInitialized();
    return delegate.readComponentMeasures(componentRef);
  }

  @Override
  @CheckForNull
  public ScannerReport.Changesets readChangesets(int componentRef) {
    ensureInitialized();
    return delegate.readChangesets(componentRef);
  }

  @Override
  public ScannerReport.Component readComponent(int componentRef) {
    ensureInitialized();
    return delegate.readComponent(componentRef);
  }

  @Override
  public CloseableIterator<ScannerReport.Issue> readComponentIssues(int componentRef) {
    ensureInitialized();
    return delegate.readComponentIssues(componentRef);
  }

  @Override
  public CloseableIterator<ScannerReport.Duplication> readComponentDuplications(int componentRef) {
    ensureInitialized();
    return delegate.readComponentDuplications(componentRef);
  }

  @Override
  public CloseableIterator<ScannerReport.CpdTextBlock> readCpdTextBlocks(int componentRef) {
    ensureInitialized();
    return delegate.readCpdTextBlocks(componentRef);
  }

  @Override
  public CloseableIterator<ScannerReport.Symbol> readComponentSymbols(int componentRef) {
    ensureInitialized();
    return delegate.readComponentSymbols(componentRef);
  }

  @Override
  public CloseableIterator<ScannerReport.SyntaxHighlightingRule> readComponentSyntaxHighlighting(int fileRef) {
    ensureInitialized();
    return delegate.readComponentSyntaxHighlighting(fileRef);
  }

  @Override
  public CloseableIterator<ScannerReport.LineCoverage> readComponentCoverage(int fileRef) {
    ensureInitialized();
    return delegate.readComponentCoverage(fileRef);
  }

  @Override
  public Optional<CloseableIterator<String>> readFileSource(int fileRef) {
    ensureInitialized();
    File file = delegate.readFileSource(fileRef);
    if (file == null) {
      return Optional.empty();
    }

    try {
      return Optional.of(new CloseableLineIterator(IOUtils.lineIterator(FileUtils.openInputStream(file), StandardCharsets.UTF_8)));
    } catch (IOException e) {
      throw new IllegalStateException("Fail to traverse file: " + file, e);
    }
  }

  private static class CloseableLineIterator extends CloseableIterator<String> {
    private final LineIterator lineIterator;

    public CloseableLineIterator(LineIterator lineIterator) {
      this.lineIterator = lineIterator;
    }

    @Override
    public boolean hasNext() {
      return lineIterator.hasNext();
    }

    @Override
    public String next() {
      return lineIterator.next();
    }

    @Override
    protected String doNext() {
      // never called anyway
      throw new NoSuchElementException("Empty closeable Iterator has no element");
    }

    @Override
    protected void doClose() {
      lineIterator.close();
    }
  }

  @Override
  public CloseableIterator<ScannerReport.Test> readTests(int testFileRef) {
    ensureInitialized();
    File file = delegate.readTests(testFileRef);
    if (file == null) {
      return CloseableIterator.emptyCloseableIterator();
    }

    try {
      return new ParserCloseableIterator<>(ScannerReport.Test.parser(), FileUtils.openInputStream(file));
    } catch (IOException e) {
      Throwables.propagate(e);
      // actually never reached
      return CloseableIterator.emptyCloseableIterator();
    }
  }

  @Override
  public CloseableIterator<ScannerReport.CoverageDetail> readCoverageDetails(int testFileRef) {
    ensureInitialized();
    File file = delegate.readCoverageDetails(testFileRef);
    if (file == null) {
      return CloseableIterator.emptyCloseableIterator();
    }

    try {
      return new ParserCloseableIterator<>(ScannerReport.CoverageDetail.parser(), FileUtils.openInputStream(file));
    } catch (IOException e) {
      Throwables.propagate(e);
      // actually never reached
      return CloseableIterator.emptyCloseableIterator();
    }
  }

  @Override
  public CloseableIterator<ScannerReport.ContextProperty> readContextProperties() {
    ensureInitialized();
    return delegate.readContextProperties();
  }

  private static class ParserCloseableIterator<T> extends CloseableIterator<T> {
    private final Parser<T> parser;
    private final FileInputStream fileInputStream;

    public ParserCloseableIterator(Parser<T> parser, FileInputStream fileInputStream) {
      this.parser = parser;
      this.fileInputStream = fileInputStream;
    }

    @Override
    protected T doNext() {
      try {
        return parser.parseDelimitedFrom(fileInputStream);
      } catch (InvalidProtocolBufferException e) {
        Throwables.propagate(e);
        // actually never reached
        return null;
      }
    }

    @Override
    protected void doClose() throws Exception {
      fileInputStream.close();
    }
  }
}
