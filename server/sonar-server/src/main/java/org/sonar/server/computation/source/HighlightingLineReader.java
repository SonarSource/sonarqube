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

package org.sonar.server.computation.source;

import com.google.common.collect.ImmutableMap;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.source.db.FileSourceDb;

import javax.annotation.CheckForNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class HighlightingLineReader implements LineReader {

  private static final Map<Constants.HighlightingType, String> cssClassByType = ImmutableMap.<Constants.HighlightingType, String>builder()
    .put(Constants.HighlightingType.ANNOTATION, "a")
    .put(Constants.HighlightingType.CONSTANT, "c")
    .put(Constants.HighlightingType.COMMENT, "cd")
    .put(Constants.HighlightingType.CPP_DOC, "cppd")
    .put(Constants.HighlightingType.STRUCTURED_COMMENT, "j")
    .put(Constants.HighlightingType.KEYWORD, "k")
    .put(Constants.HighlightingType.KEYWORD_LIGHT, "h")
    .put(Constants.HighlightingType.HIGHLIGHTING_STRING, "s")
    .put(Constants.HighlightingType.PREPROCESS_DIRECTIVE, "p")
    .build();

  private final Iterator<BatchReport.SyntaxHighlighting> lineHighlightingIterator;

  private BatchReport.SyntaxHighlighting currentItem;
  private List<BatchReport.SyntaxHighlighting> highlightingList;

  public HighlightingLineReader(Iterator<BatchReport.SyntaxHighlighting> lineHighlightingIterator) {
    this.lineHighlightingIterator = lineHighlightingIterator;
    this.highlightingList = newArrayList();
  }

  @Override
  public void read(FileSourceDb.Line.Builder lineBuilder) {
    int line = lineBuilder.getLine();
    StringBuilder highlighting = new StringBuilder();

    incrementHighlightingListMatchingLine(line);
    for (Iterator<BatchReport.SyntaxHighlighting> syntaxHighlightingIterator = highlightingList.iterator(); syntaxHighlightingIterator.hasNext();) {
      BatchReport.SyntaxHighlighting syntaxHighlighting = syntaxHighlightingIterator.next();
      BatchReport.Range range = syntaxHighlighting.getRange();
      if (range.getStartLine() <= line) {
        String offsets = RangeOffsetHelper.offsetToString(syntaxHighlighting.getRange(), line, lineBuilder.getSource().length());
        if (!offsets.isEmpty()) {
          if (highlighting.length() > 0) {
            highlighting.append(RangeOffsetHelper.SYMBOLS_SEPARATOR);
          }
          highlighting.append(offsets)
            .append(RangeOffsetHelper.OFFSET_SEPARATOR)
            .append(getCssClass(syntaxHighlighting.getType()));
          if (range.getEndLine() == line) {
            syntaxHighlightingIterator.remove();
          }
        } else {
          syntaxHighlightingIterator.remove();
        }
      }
    }
    if (highlighting.length() > 0) {
      lineBuilder.setHighlighting(highlighting.toString());
    }
  }

  private static String getCssClass(Constants.HighlightingType type) {
    String cssClass = cssClassByType.get(type);
    if (cssClass != null) {
      return cssClass;
    } else {
      throw new IllegalArgumentException(String.format("Unknown type %s ", type.toString()));
    }
  }

  private void incrementHighlightingListMatchingLine(int line) {
    BatchReport.SyntaxHighlighting syntaxHighlighting = getNextHighlightingMatchingLine(line);
    while (syntaxHighlighting != null) {
      highlightingList.add(syntaxHighlighting);
      this.currentItem = null;
      syntaxHighlighting = getNextHighlightingMatchingLine(line);
    }
  }

  @CheckForNull
  private BatchReport.SyntaxHighlighting getNextHighlightingMatchingLine(int line) {
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
