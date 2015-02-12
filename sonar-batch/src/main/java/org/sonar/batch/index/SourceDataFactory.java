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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.api.batch.sensor.symbol.Symbol;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.highlighting.SyntaxHighlightingData;
import org.sonar.batch.highlighting.SyntaxHighlightingRule;
import org.sonar.batch.scan.filesystem.InputFileMetadata;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.batch.source.CodeColorizers;
import org.sonar.batch.symbol.SymbolData;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.source.db.FileSourceDb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Consolidate different caches for the export of file sources to server.
 * @see org.sonar.server.source.db.FileSourceDb
 */
public class SourceDataFactory implements BatchComponent {

  private static final String BOM = "\uFEFF";

  private final MeasureCache measureCache;
  private final ComponentDataCache componentDataCache;
  private final DuplicationCache duplicationCache;
  private final CodeColorizers codeColorizers;

  public SourceDataFactory(MeasureCache measureCache, ComponentDataCache componentDataCache,
    DuplicationCache duplicationCache, CodeColorizers codeColorizers) {
    this.measureCache = measureCache;
    this.componentDataCache = componentDataCache;
    this.duplicationCache = duplicationCache;
    this.codeColorizers = codeColorizers;
  }

  public byte[] consolidateData(DefaultInputFile inputFile, InputFileMetadata metadata) throws IOException {
    FileSourceDb.Data.Builder dataBuilder = createForSource(inputFile);
    applyLineMeasures(inputFile, dataBuilder);
    applyDuplications(inputFile.key(), dataBuilder);
    applyHighlighting(inputFile, metadata, dataBuilder);
    applySymbolReferences(inputFile, metadata, dataBuilder);
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

  void applyLineMeasures(DefaultInputFile file, FileSourceDb.Data.Builder dataBuilder) {
    applyLineMeasure(file.key(), CoreMetrics.SCM_AUTHORS_BY_LINE_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setScmAuthor(value);
      }
    });
    applyLineMeasure(file.key(), CoreMetrics.SCM_REVISIONS_BY_LINE_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setScmRevision(value);
      }
    });
    applyLineMeasure(file.key(), CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY, dataBuilder, new MeasureOperation() {
      @Override
      public void apply(String value, FileSourceDb.Line.Builder lineBuilder) {
        lineBuilder.setScmDate(DateUtils.parseDateTimeQuietly(value).getTime());
      }
    });
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
    if (measures != null) {
      for (Measure measure : measures) {
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
  }

  static interface MeasureOperation {
    void apply(String value, FileSourceDb.Line.Builder lineBuilder);
  }

  void applyHighlighting(DefaultInputFile inputFile, InputFileMetadata metadata, FileSourceDb.Data.Builder to) {
    SyntaxHighlightingData highlighting = componentDataCache.getData(inputFile.key(), SnapshotDataTypes.SYNTAX_HIGHLIGHTING);
    String language = inputFile.language();
    if (highlighting == null && language != null) {
      highlighting = codeColorizers.toSyntaxHighlighting(inputFile.file(), inputFile.charset(), language);
    }
    if (highlighting == null) {
      return;
    }
    StringBuilder[] highlightingPerLine = new StringBuilder[inputFile.lines()];
    RuleItemWriter ruleItemWriter = new RuleItemWriter();
    int currentLineIdx = 1;
    for (SyntaxHighlightingRule rule : highlighting.syntaxHighlightingRuleSet()) {
      while (currentLineIdx < inputFile.lines() && rule.getStartPosition() >= metadata.originalLineOffsets()[currentLineIdx]) {
        // This rule starts on another line so advance
        currentLineIdx++;
      }
      // Now we know current rule starts on current line
      writeDataPerLine(metadata.originalLineOffsets(), rule, rule.getStartPosition(), rule.getEndPosition(), highlightingPerLine, currentLineIdx, ruleItemWriter);
    }
    for (int i = 0; i < highlightingPerLine.length; i++) {
      StringBuilder sb = highlightingPerLine[i];
      if (sb != null) {
        to.getLinesBuilder(i).setHighlighting(sb.toString());
      }
    }
  }

  void applySymbolReferences(DefaultInputFile file, InputFileMetadata metadata, FileSourceDb.Data.Builder to) {
    SymbolData symbolRefs = componentDataCache.getData(file.key(), SnapshotDataTypes.SYMBOL_HIGHLIGHTING);
    if (symbolRefs != null) {
      StringBuilder[] refsPerLine = new StringBuilder[file.lines()];
      int symbolId = 1;
      List<Symbol> symbols = new ArrayList<Symbol>(symbolRefs.referencesBySymbol().keySet());
      // Sort symbols to avoid false variation that would lead to an unnecessary update
      Collections.sort(symbols, new Comparator<Symbol>() {
        @Override
        public int compare(Symbol o1, Symbol o2) {
          return o1.getDeclarationStartOffset() - o2.getDeclarationStartOffset();
        }
      });
      for (Symbol symbol : symbols) {
        int declarationStartOffset = symbol.getDeclarationStartOffset();
        int declarationEndOffset = symbol.getDeclarationEndOffset();
        int length = declarationEndOffset - declarationStartOffset;
        addSymbol(symbolId, declarationStartOffset, declarationEndOffset, metadata.originalLineOffsets(), refsPerLine);
        for (Integer referenceStartOffset : symbolRefs.referencesBySymbol().get(symbol)) {
          if (referenceStartOffset == declarationStartOffset) {
            // Ignore old API that used to store reference as first declaration
            continue;
          }
          addSymbol(symbolId, referenceStartOffset, referenceStartOffset + length, metadata.originalLineOffsets(), refsPerLine);
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
  }

  private void addSymbol(int symbolId, int startOffset, int endOffset, int[] originalLineOffsets, StringBuilder[] result) {
    int startLine = binarySearchLine(startOffset, originalLineOffsets);
    writeDataPerLine(originalLineOffsets, symbolId, startOffset, endOffset, result, startLine, new SymbolItemWriter());
  }

  private int binarySearchLine(int declarationStartOffset, int[] originalLineOffsets) {
    int begin = 0;
    int end = originalLineOffsets.length - 1;
    while (begin < end) {
      int mid = (int) Math.round((begin + end) / 2D);
      if (declarationStartOffset < originalLineOffsets[mid]) {
        end = mid - 1;
      } else {
        begin = mid;
      }
    }
    return begin + 1;
  }

  private <G> void writeDataPerLine(int[] originalLineOffsets, G item, int globalStartOffset, int globalEndOffset, StringBuilder[] dataPerLine, int startLine,
    RangeItemWriter<G> writer) {
    int currentLineIdx = startLine;
    // We know current item starts on current line
    long ruleStartOffsetCurrentLine = globalStartOffset;
    while (currentLineIdx < originalLineOffsets.length && globalEndOffset >= originalLineOffsets[currentLineIdx]) {
      // item continue on next line so write current line and continue on next line with same item
      writeItem(item, dataPerLine, currentLineIdx, ruleStartOffsetCurrentLine - originalLineOffsets[currentLineIdx - 1], originalLineOffsets[currentLineIdx]
        - originalLineOffsets[currentLineIdx - 1], writer);
      currentLineIdx++;
      ruleStartOffsetCurrentLine = originalLineOffsets[currentLineIdx - 1];
    }
    // item ends on current line
    writeItem(item, dataPerLine, currentLineIdx, ruleStartOffsetCurrentLine - originalLineOffsets[currentLineIdx - 1], globalEndOffset
      - originalLineOffsets[currentLineIdx - 1], writer);
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

  private static class RuleItemWriter implements RangeItemWriter<SyntaxHighlightingRule> {
    @Override
    public void writeItem(StringBuilder currentLineSb, long startLineOffset, long endLineOffset, SyntaxHighlightingRule item) {
      if (currentLineSb.length() > 0) {
        currentLineSb.append(SyntaxHighlightingData.RULE_SEPARATOR);
      }
      currentLineSb.append(startLineOffset)
        .append(SyntaxHighlightingData.FIELD_SEPARATOR)
        .append(endLineOffset)
        .append(SyntaxHighlightingData.FIELD_SEPARATOR)
        .append(item.getTextType().cssClass());
    }

  }

  private static class SymbolItemWriter implements RangeItemWriter<Integer> {
    @Override
    public void writeItem(StringBuilder currentLineSb, long startLineOffset, long endLineOffset, Integer symbolId) {
      if (currentLineSb.length() > 0) {
        currentLineSb.append(SymbolData.SYMBOL_SEPARATOR);
      }
      currentLineSb.append(startLineOffset)
        .append(SymbolData.FIELD_SEPARATOR)
        .append(endLineOffset)
        .append(SymbolData.FIELD_SEPARATOR)
        .append(symbolId);
    }
  }

  void applyDuplications(String inputFileKey, FileSourceDb.Data.Builder to) {
    List<DuplicationGroup> groups = duplicationCache.byComponent(inputFileKey);
    if (groups != null) {
      int blockId = 1;
      for (Iterator<DuplicationGroup> it = groups.iterator(); it.hasNext();) {
        DuplicationGroup group = it.next();
        addBlock(blockId, group.originBlock(), to);
        blockId++;
        for (Iterator<DuplicationGroup.Block> dupsIt = group.duplicates().iterator(); dupsIt.hasNext();) {
          DuplicationGroup.Block dups = dupsIt.next();
          if (inputFileKey.equals(dups.resourceKey())) {
            addBlock(blockId, dups, to);
            blockId++;
          }
          // Save memory
          dupsIt.remove();
        }
        // Save memory
        it.remove();
      }
    }
  }

  private void addBlock(int blockId, DuplicationGroup.Block block, FileSourceDb.Data.Builder to) {
    int currentLine = block.startLine();
    for (int i = 0; i < block.length(); i++) {
      if (currentLine <= to.getLinesCount()) {
        to.getLinesBuilder(currentLine - 1).addDuplications(blockId);
        currentLine++;
      }
    }
  }
}
