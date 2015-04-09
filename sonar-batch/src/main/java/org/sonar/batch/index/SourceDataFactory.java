/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.index;

import com.google.common.base.CharMatcher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.duplication.Duplication;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Range;
import org.sonar.batch.protocol.output.BatchReport.Scm;
import org.sonar.batch.protocol.output.BatchReport.Scm.Changeset;
import org.sonar.batch.protocol.output.BatchReport.Symbols;
import org.sonar.batch.protocol.output.BatchReport.SyntaxHighlighting;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.report.BatchReportUtils;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.db.FileSourceDb.Data.Builder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Consolidate different caches for the export of file sources to server.
 * @see org.sonar.server.source.db.FileSourceDb
 */
public class SourceDataFactory implements BatchComponent {

  private static final String BOM = "\uFEFF";

  private final MeasureCache measureCache;
  private final DuplicationCache duplicationCache;
  private final ReportPublisher reportPublisher;

  private final ResourceCache resourceCache;

  public SourceDataFactory(MeasureCache measureCache, DuplicationCache duplicationCache, ReportPublisher reportPublisher, ResourceCache resourceCache) {
    this.measureCache = measureCache;
    this.duplicationCache = duplicationCache;
    this.reportPublisher = reportPublisher;
    this.resourceCache = resourceCache;
  }

  public byte[] consolidateData(DefaultInputFile inputFile) throws IOException {
    FileSourceDb.Data.Builder dataBuilder = createForSource(inputFile);
    applyLineMeasures(inputFile, dataBuilder);
    applyScm(inputFile, dataBuilder);
    applyDuplications(inputFile.key(), dataBuilder);
    applyHighlighting(inputFile, dataBuilder);
    applySymbolReferences(inputFile, dataBuilder);
    return FileSourceDto.encodeData(dataBuilder.build());
  }

  FileSourceDb.Data.Builder createForSource(DefaultInputFile inputFile) throws IOException {
    FileSourceDb.Data.Builder result = FileSourceDb.Data.newBuilder();
    List<String> lines = FileUtils.readLines(inputFile.file(), inputFile.charset());
    // Missing empty last line
    if (lines.size() == inputFile.lines() - 1) {
      lines.add("");
    }
    for (int lineIdx = 1; lineIdx <= lines.size(); lineIdx++) {
      String s = CharMatcher.anyOf(BOM).removeFrom(lines.get(lineIdx - 1));
      FileSourceDb.Line.Builder linesBuilder = result.addLinesBuilder();
      linesBuilder.setLine(lineIdx).setSource(s);
    }
    return result;
  }

  void applyScm(DefaultInputFile inputFile, Builder dataBuilder) {
    BatchReportReader reader = new BatchReportReader(reportPublisher.getReportDir());
    Scm componentScm = reader.readComponentScm(resourceCache.get(inputFile).batchId());
    if (componentScm != null) {
      for (int i = 0; i < componentScm.getChangesetIndexByLineCount(); i++) {
        int index = componentScm.getChangesetIndexByLine(i);
        Changeset changeset = componentScm.getChangeset(index);
        if (i < dataBuilder.getLinesCount()) {
          FileSourceDb.Line.Builder lineBuilder = dataBuilder.getLinesBuilder(i);
          if (changeset.hasAuthor()) {
            lineBuilder.setScmAuthor(changeset.getAuthor());
          }
          if (changeset.hasRevision()) {
            lineBuilder.setScmRevision(changeset.getRevision());
          }
          if (changeset.hasDate()) {
            lineBuilder.setScmDate(changeset.getDate());
          }
        }
      }
    }
  }

  void applyLineMeasures(DefaultInputFile file, FileSourceDb.Data.Builder dataBuilder) {
    applyLineMeasure(file.key(), CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setUtLineHits(Integer.parseInt(value));
      }
    });
    applyLineMeasure(file.key(), CoreMetrics.CONDITIONS_BY_LINE_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setUtConditions(Integer.parseInt(value));
      }
    });
    applyLineMeasure(file.key(), CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setUtCoveredConditions(Integer.parseInt(value));
      }
    });
    applyLineMeasure(file.key(), CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setItLineHits(Integer.parseInt(value));
      }
    });
    applyLineMeasure(file.key(), CoreMetrics.IT_CONDITIONS_BY_LINE_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setItConditions(Integer.parseInt(value));
      }
    });
    applyLineMeasure(file.key(), CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setItCoveredConditions(Integer.parseInt(value));
      }
    });
    applyLineMeasure(file.key(), CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setOverallLineHits(Integer.parseInt(value));
      }
    });
    applyLineMeasure(file.key(), CoreMetrics.OVERALL_CONDITIONS_BY_LINE_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setOverallConditions(Integer.parseInt(value));
      }
    });
    applyLineMeasure(file.key(), CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setOverallCoveredConditions(Integer.parseInt(value));
      }
    });
  }

  void applyLineMeasure(String inputFileKey, String metricKey, FileSourceDb.Data.Builder to, MeasureOperation op) {
    Iterable<Measure> measures = measureCache.byMetric(inputFileKey, metricKey);
    if (measures.iterator().hasNext()) {
      Measure measure = measures.iterator().next();
      Map<Integer, String> lineMeasures = KeyValueFormat.parseIntString((String) measure.value());
      for (Map.Entry<Integer, String> lineMeasure : lineMeasures.entrySet()) {
        int lineIdx = lineMeasure.getKey();
        if (lineIdx <= to.getLinesCount()) {
          String value = lineMeasure.getValue();
          if (StringUtils.isNotEmpty(value)) {
            FileSourceDb.Line.Builder lineBuilder = to.getLinesBuilder(lineIdx - 1);
            op.apply(value, lineBuilder);
          }
        }
      }
    }
  }

  static interface MeasureOperation {
    void apply(String value, FileSourceDb.Line.Builder lineBuilder);
  }

  void applyHighlighting(DefaultInputFile inputFile, FileSourceDb.Data.Builder to) {
    BatchReportReader reader = new BatchReportReader(reportPublisher.getReportDir());
    File highlightingFile = reader.readComponentSyntaxHighlighting(resourceCache.get(inputFile).batchId());
    if (highlightingFile == null) {
      return;
    }
    StringBuilder[] highlightingPerLine = new StringBuilder[inputFile.lines()];
    RuleItemWriter ruleItemWriter = new RuleItemWriter();
    int currentLineIdx = 1;

    InputStream inputStream = null;
    try {
      inputStream = FileUtils.openInputStream(highlightingFile);
      BatchReport.SyntaxHighlighting rule = BatchReport.SyntaxHighlighting.PARSER.parseDelimitedFrom(inputStream);
      while (rule != null) {
        while (currentLineIdx < inputFile.lines() && rule.getRange().getStartLine() > currentLineIdx) {
          // This rule starts on another line so advance
          currentLineIdx++;
        }
        // Now we know current rule starts on current line
        writeDataPerLine(inputFile.originalLineOffsets(), rule, rule.getRange(), highlightingPerLine, ruleItemWriter);

        // Get next element
        rule = BatchReport.SyntaxHighlighting.PARSER.parseDelimitedFrom(inputStream);
      }

    } catch (Exception e) {
      throw new IllegalStateException("Can't read syntax highlighting for " + inputFile.absolutePath(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
    for (int i = 0; i < highlightingPerLine.length; i++) {
      StringBuilder sb = highlightingPerLine[i];
      if (sb != null) {
        to.getLinesBuilder(i).setHighlighting(sb.toString());
      }
    }
  }

  void applySymbolReferences(DefaultInputFile inputFile, FileSourceDb.Data.Builder to) {
    BatchReportReader reader = new BatchReportReader(reportPublisher.getReportDir());
    List<Symbols.Symbol> symbols = new ArrayList<Symbols.Symbol>(reader.readComponentSymbols(resourceCache.get(inputFile).batchId()));
    if (symbols.isEmpty()) {
      return;
    }
    StringBuilder[] refsPerLine = new StringBuilder[inputFile.lines()];
    int symbolId = 1;
    // Sort symbols to avoid false variation that would lead to an unnecessary update
    Collections.sort(symbols, new Comparator<Symbols.Symbol>() {
      @Override
      public int compare(Symbols.Symbol o1, Symbols.Symbol o2) {
        if (o1.getDeclaration().getStartLine() == o2.getDeclaration().getStartLine()) {
          return Integer.compare(o1.getDeclaration().getStartOffset(), o2.getDeclaration().getStartOffset());
        } else {
          return Integer.compare(o1.getDeclaration().getStartLine(), o2.getDeclaration().getStartLine());
        }
      }
    });
    for (Symbols.Symbol symbol : symbols) {
      addSymbol(symbolId, symbol.getDeclaration(), inputFile.originalLineOffsets(), refsPerLine);
      for (Range reference : symbol.getReferenceList()) {
        addSymbol(symbolId, reference, inputFile.originalLineOffsets(), refsPerLine);
      }
      symbolId++;
    }
    for (int i = 0; i < refsPerLine.length; i++) {
      StringBuilder sb = refsPerLine[i];
      if (sb != null) {
        to.getLinesBuilder(i).setSymbols(sb.toString());
      }
    }
  }

  private void addSymbol(int symbolId, Range range, int[] originalLineOffsets, StringBuilder[] result) {
    writeDataPerLine(originalLineOffsets, symbolId, range, result, new SymbolItemWriter());
  }

  private <G> void writeDataPerLine(int[] originalLineOffsets, G item, Range range, StringBuilder[] dataPerLine, RangeItemWriter<G> writer) {
    int currentLineIdx = range.getStartLine();
    long ruleStartOffsetCurrentLine = range.getStartOffset();
    while (currentLineIdx < dataPerLine.length && range.getEndLine() > currentLineIdx) {
      // item continue on next line so write current line and continue on next line with same item
      writeItem(item, dataPerLine, currentLineIdx, ruleStartOffsetCurrentLine, lineLength(originalLineOffsets, currentLineIdx), writer);
      currentLineIdx++;
      ruleStartOffsetCurrentLine = 0;
    }
    // item ends on current line
    writeItem(item, dataPerLine, currentLineIdx, ruleStartOffsetCurrentLine, range.getEndOffset(), writer);
  }

  private int lineLength(int[] originalLineOffsets, int currentLineIdx) {
    return originalLineOffsets[currentLineIdx]
      - originalLineOffsets[currentLineIdx - 1];
  }

  private <G> void writeItem(G item, StringBuilder[] dataPerLine, int currentLineIdx, long startLineOffset, long endLineOffset, RangeItemWriter<G> writer) {
    if (startLineOffset == endLineOffset || currentLineIdx > dataPerLine.length) {
      // empty items or bad line index
      return;
    }
    if (dataPerLine[currentLineIdx - 1] == null) {
      dataPerLine[currentLineIdx - 1] = new StringBuilder();
    }
    StringBuilder currentLineSb = dataPerLine[currentLineIdx - 1];
    writer.writeItem(currentLineSb, startLineOffset, endLineOffset, item);
  }

  private static interface RangeItemWriter<G> {
    /**
     * Write item on a single line
     */
    void writeItem(StringBuilder currentLineSb, long startLineOffset, long endLineOffset, G item);
  }

  private static class RuleItemWriter implements RangeItemWriter<SyntaxHighlighting> {
    @Override
    public void writeItem(StringBuilder currentLineSb, long startLineOffset, long endLineOffset, SyntaxHighlighting item) {
      if (currentLineSb.length() > 0) {
        currentLineSb.append(';');
      }
      currentLineSb.append(startLineOffset)
        .append(',')
        .append(endLineOffset)
        .append(',')
        .append(BatchReportUtils.toCssClass(item.getType()));
    }

  }

  private static class SymbolItemWriter implements RangeItemWriter<Integer> {
    @Override
    public void writeItem(StringBuilder currentLineSb, long startLineOffset, long endLineOffset, Integer symbolId) {
      if (currentLineSb.length() > 0) {
        currentLineSb.append(";");
      }
      currentLineSb.append(startLineOffset)
        .append(",")
        .append(endLineOffset)
        .append(",")
        .append(symbolId);
    }
  }

  void applyDuplications(String inputFileKey, FileSourceDb.Data.Builder to) {
    Iterable<DefaultDuplication> groups = duplicationCache.byComponent(inputFileKey);
    int blockId = 1;
    for (Iterator<DefaultDuplication> it = groups.iterator(); it.hasNext();) {
      Duplication group = it.next();
      addBlock(blockId, group.originBlock(), to);
      blockId++;
      for (Iterator<Duplication.Block> dupsIt = group.duplicates().iterator(); dupsIt.hasNext();) {
        Duplication.Block dups = dupsIt.next();
        if (inputFileKey.equals(dups.resourceKey())) {
          addBlock(blockId, dups, to);
          blockId++;
        }
      }
    }
  }

  private void addBlock(int blockId, Duplication.Block block, FileSourceDb.Data.Builder to) {
    int currentLine = block.startLine();
    for (int i = 0; i < block.length(); i++) {
      if (currentLine <= to.getLinesCount()) {
        to.getLinesBuilder(currentLine - 1).addDuplication(blockId);
        currentLine++;
      }
    }
  }
}
