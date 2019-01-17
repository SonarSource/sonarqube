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
package org.sonar.ce.task.projectanalysis.batch;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.LineReaderIterator;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.LineSgnificantCode;

import static java.nio.charset.StandardCharsets.UTF_8;

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
      InputStreamReader reader = new InputStreamReader(FileUtils.openInputStream(file), UTF_8);
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
  public CloseableIterator<ScannerReport.AdHocRule> readAdHocRules() {
    ensureInitialized();
    return delegate.readAdHocRules();
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
  public CloseableIterator<ScannerReport.ExternalIssue> readComponentExternalIssues(int componentRef) {
    ensureInitialized();
    return delegate.readComponentExternalIssues(componentRef);
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
      return Optional.of(new CloseableLineIterator(IOUtils.lineIterator(FileUtils.openInputStream(file), UTF_8)));
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
    protected void doClose() throws IOException {
      lineIterator.close();
    }
  }

  @Override
  public CloseableIterator<ScannerReport.ContextProperty> readContextProperties() {
    ensureInitialized();
    return delegate.readContextProperties();
  }

  @Override
  public Optional<CloseableIterator<LineSgnificantCode>> readComponentSignificantCode(int fileRef) {
    ensureInitialized();
    return Optional.ofNullable(delegate.readComponentSignificantCode(fileRef));
  }

  @Override
  public Optional<ScannerReport.ChangedLines> readComponentChangedLines(int fileRef) {
    ensureInitialized();
    return Optional.ofNullable(delegate.readComponentChangedLines(fileRef));
  }

  @Override
  public CloseableIterator<ScannerReport.AnalysisWarning> readAnalysisWarnings() {
    ensureInitialized();
    return delegate.readAnalysisWarnings();
  }
}
