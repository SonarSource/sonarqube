/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.source.linereader;

import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.source.linereader.RangeOffsetConverter.RangeOffsetConverterException;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.sonar.ce.task.projectanalysis.source.linereader.LineReader.Data.HIGHLIGHTING;
import static org.sonar.ce.task.projectanalysis.source.linereader.RangeOffsetConverter.OFFSET_SEPARATOR;
import static org.sonar.ce.task.projectanalysis.source.linereader.RangeOffsetConverter.SYMBOLS_SEPARATOR;

public class HighlightingLineReader implements LineReader {

  private static final Logger LOG = LoggerFactory.getLogger(HighlightingLineReader.class);

  private static final Map<HighlightingType, String> cssClassByType = ImmutableMap.<HighlightingType, String>builder()
    .put(HighlightingType.ANNOTATION, "a")
    .put(HighlightingType.CONSTANT, "c")
    .put(HighlightingType.COMMENT, "cd")
    .put(HighlightingType.STRUCTURED_COMMENT, "j")
    .put(HighlightingType.KEYWORD, "k")
    .put(HighlightingType.KEYWORD_LIGHT, "h")
    .put(HighlightingType.HIGHLIGHTING_STRING, "s")
    .put(HighlightingType.PREPROCESS_DIRECTIVE, "p")
    .build();

  private final Component file;
  private final Iterator<ScannerReport.SyntaxHighlightingRule> lineHighlightingIterator;
  private final RangeOffsetConverter rangeOffsetConverter;
  private final List<ScannerReport.SyntaxHighlightingRule> highlightingList;
  private ReadError readError = null;
  private ScannerReport.SyntaxHighlightingRule currentItem;

  public HighlightingLineReader(Component file, Iterator<ScannerReport.SyntaxHighlightingRule> lineHighlightingIterator, RangeOffsetConverter rangeOffsetConverter) {
    this.file = file;
    this.lineHighlightingIterator = lineHighlightingIterator;
    this.rangeOffsetConverter = rangeOffsetConverter;
    this.highlightingList = newArrayList();
  }

  /**
   * Stops reading at first encountered error, which implies the same
   * {@link org.sonar.ce.task.projectanalysis.source.linereader.LineReader.ReadError} will be returned once an error
   * has been encountered and for any then provided {@link DbFileSources.Line.Builder lineBuilder}.
   */
  @Override
  public Optional<ReadError> read(DbFileSources.Line.Builder lineBuilder) {
    if (readError == null) {
      try {
        processHighlightings(lineBuilder);
      } catch (RangeOffsetConverterException e) {
        readError = new ReadError(HIGHLIGHTING, lineBuilder.getLine());
        LOG.debug(format("Inconsistency detected in Highlighting data. Highlighting will be ignored for file '%s'", file.getKey()), e);
      }
    }
    return Optional.ofNullable(readError);
  }

  private void processHighlightings(DbFileSources.Line.Builder lineBuilder) {
    int line = lineBuilder.getLine();
    StringBuilder highlighting = new StringBuilder();

    incrementHighlightingListMatchingLine(line);
    for (Iterator<ScannerReport.SyntaxHighlightingRule> syntaxHighlightingIterator = highlightingList.iterator(); syntaxHighlightingIterator.hasNext();) {
      processHighlighting(syntaxHighlightingIterator, highlighting, lineBuilder);
    }
    if (highlighting.length() > 0) {
      lineBuilder.setHighlighting(highlighting.toString());
    }
  }

  private void processHighlighting(Iterator<ScannerReport.SyntaxHighlightingRule> syntaxHighlightingIterator, StringBuilder highlighting,
    DbFileSources.Line.Builder lineBuilder) {
    ScannerReport.SyntaxHighlightingRule syntaxHighlighting = syntaxHighlightingIterator.next();
    int line = lineBuilder.getLine();
    ScannerReport.TextRange range = syntaxHighlighting.getRange();
    if (range.getStartLine() <= line) {
      String offsets = rangeOffsetConverter.offsetToString(syntaxHighlighting.getRange(), line, lineBuilder.getSource().length());
      if (offsets.isEmpty()) {
        if (range.getEndLine() == line) {
          syntaxHighlightingIterator.remove();
        }
      } else {
        if (highlighting.length() > 0) {
          highlighting.append(SYMBOLS_SEPARATOR);
        }
        highlighting.append(offsets)
          .append(OFFSET_SEPARATOR)
          .append(getCssClass(syntaxHighlighting.getType()));
        if (range.getEndLine() == line) {
          syntaxHighlightingIterator.remove();
        }
      }
    }
  }

  private static String getCssClass(HighlightingType type) {
    String cssClass = cssClassByType.get(type);
    if (cssClass != null) {
      return cssClass;
    } else {
      throw new IllegalArgumentException(format("Unknown type %s ", type.toString()));
    }
  }

  private void incrementHighlightingListMatchingLine(int line) {
    ScannerReport.SyntaxHighlightingRule syntaxHighlighting = getNextHighlightingMatchingLine(line);
    while (syntaxHighlighting != null) {
      highlightingList.add(syntaxHighlighting);
      this.currentItem = null;
      syntaxHighlighting = getNextHighlightingMatchingLine(line);
    }
  }

  @CheckForNull
  private ScannerReport.SyntaxHighlightingRule getNextHighlightingMatchingLine(int line) {
    // Get next element (if exists)
    if (currentItem == null && lineHighlightingIterator.hasNext()) {
      currentItem = lineHighlightingIterator.next();
    }
    // Return current element if lines match
    if (currentItem != null && currentItem.getRange().getStartLine() == line) {
      return currentItem;
    }
    return null;
  }

}
