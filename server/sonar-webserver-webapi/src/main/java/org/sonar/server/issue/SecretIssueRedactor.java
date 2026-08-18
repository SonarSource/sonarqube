/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.issue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.protobuf.DbIssues;

/**
 * Redacts secret values detected by secret rules before source code is sent to clients.
 * <p>
 * Source redaction masks trusted secret ranges or fully redacts the returned source response while retaining source length and metadata.
 */
public final class SecretIssueRedactor {
  private SecretIssueRedactor() {
  }

  /**
   * Masks trusted secret ranges while retaining every line's original length and metadata.
   * Source responses are fully redacted when a rule does not provide enough trusted location data.
   */
  public static List<DbFileSources.Line> redactSourceLines(List<DbFileSources.Line> lines, List<IssueDto> issues) {
    Map<Integer, List<DbCommons.TextRange>> secretRangesByLine = new HashMap<>();
    Set<Integer> fullyRedactedLines = new HashSet<>();
    return redactSourceLines(
      lines,
      issues.stream().anyMatch(issue -> requiresWholeSourceRedactionForIssue(issue, secretRangesByLine, fullyRedactedLines)),
      secretRangesByLine,
      fullyRedactedLines
    );
  }

  private static List<DbFileSources.Line> redactSourceLines(
    List<DbFileSources.Line> lines,
    boolean redactWholeSource,
    Map<Integer, List<DbCommons.TextRange>> secretRangesByLine,
    Set<Integer> fullyRedactedLines
  ) {
    return lines.stream()
      .map(line -> redactWholeSource || fullyRedactedLines.contains(line.getLine())
        ? redactLine(line)
        : redactLine(line, secretRangesByLine.get(line.getLine())))
      .toList();
  }

  /**
   * Adds a trusted secret range and returns whether all source lines in the response must be redacted.
   */
  private static boolean addSecretRedaction(IssueDto issue, Map<Integer, List<DbCommons.TextRange>> secretRangesByLine) {
    if (!SecretIssueRedactionRules.hasTrustedSourceRange(issue)) {
      return true;
    }

    DbIssues.Locations locations = issue.parseLocations();
    if (locations == null || !locations.hasTextRange()) {
      return true;
    }

    DbCommons.TextRange textRange = locations.getTextRange();
    if (!textRange.hasStartLine() || !textRange.hasStartOffset() || !textRange.hasEndOffset()) {
      return true;
    }

    int startLine = textRange.getStartLine();
    int endLine = textRange.hasEndLine() ? textRange.getEndLine() : startLine;
    if (startLine < 1 || endLine < startLine) {
      return true;
    }

    for (int line = startLine; line <= endLine; line++) {
      secretRangesByLine.computeIfAbsent(line, ignored -> new ArrayList<>()).add(textRange);
    }
    return false;
  }

  private static boolean requiresWholeSourceRedactionForIssue(IssueDto issue, Map<Integer, List<DbCommons.TextRange>> secretRangesByLine, Set<Integer> fullyRedactedLines) {
    if (SecretIssueRedactionRules.isFlowBasedSourceRule(issue)) {
      return addS6437Redaction(issue, fullyRedactedLines);
    }
    return isSecretIssue(issue) && addSecretRedaction(issue, secretRangesByLine);
  }

  private static boolean addS6437Redaction(IssueDto issue, Set<Integer> fullyRedactedLines) {
    DbIssues.Locations locations = issue.parseLocations();
    if (locations == null) {
      return true;
    }

    FlowRedactionResult flowRedactionResult = addRelevantFlowRedactions(locations, issue, fullyRedactedLines);
    if (flowRedactionResult.requiresWholeSourceRedaction()) {
      return true;
    }
    return requiresWholeSourceRedactionForS6437PrimaryLocation(locations, flowRedactionResult.hasRelevantLocation(), fullyRedactedLines);
  }

  private static FlowRedactionResult addRelevantFlowRedactions(DbIssues.Locations locations, IssueDto issue, Set<Integer> fullyRedactedLines) {
    boolean hasRelevantLocation = false;
    for (DbIssues.Flow flow : locations.getFlowList()) {
      for (DbIssues.Location location : flow.getLocationList()) {
        if (isLocationInIssueComponent(issue, location)) {
          hasRelevantLocation = true;
          if (!location.hasTextRange() || hasInvalidLineRange(location.getTextRange())) {
            return new FlowRedactionResult(hasRelevantLocation, true);
          }
          addFullyRedactedLines(location.getTextRange(), fullyRedactedLines);
        }
      }
    }
    return new FlowRedactionResult(hasRelevantLocation, false);
  }

  private static boolean requiresWholeSourceRedactionForS6437PrimaryLocation(DbIssues.Locations locations, boolean hasRelevantFlowLocation, Set<Integer> fullyRedactedLines) {
    if (!hasRelevantFlowLocation || !locations.hasTextRange() || hasInvalidLineRange(locations.getTextRange())) {
      return true;
    }
    addFullyRedactedLines(locations.getTextRange(), fullyRedactedLines);
    return false;
  }

  private static boolean isLocationInIssueComponent(IssueDto issue, DbIssues.Location location) {
    return !location.hasComponentId() || Objects.equals(issue.getComponentUuid(), location.getComponentId());
  }

  private static boolean isSecretIssue(IssueDto issue) {
    return SecretIssueRedactionRules.isSecretIssue(issue);
  }

  private static boolean hasInvalidLineRange(DbCommons.TextRange textRange) {
    if (!textRange.hasStartLine()) {
      return true;
    }
    int startLine = textRange.getStartLine();
    int endLine = textRange.hasEndLine() ? textRange.getEndLine() : startLine;
    return startLine < 1 || endLine < startLine;
  }

  private static void addFullyRedactedLines(DbCommons.TextRange textRange, Set<Integer> fullyRedactedLines) {
    int startLine = textRange.getStartLine();
    int endLine = textRange.hasEndLine() ? textRange.getEndLine() : startLine;
    for (int line = startLine; line <= endLine; line++) {
      fullyRedactedLines.add(line);
    }
  }

  private record FlowRedactionResult(boolean hasRelevantLocation, boolean requiresWholeSourceRedaction) {}

  private static DbFileSources.Line redactLine(DbFileSources.Line line) {
    return line.toBuilder().setSource(redactWholeLine(line.getSource())).build();
  }

  private static DbFileSources.Line redactLine(DbFileSources.Line line, @Nullable List<DbCommons.TextRange> ranges) {
    if (ranges == null || ranges.isEmpty()) {
      return line;
    }

    String redactedSource = redactRanges(line.getSource(), line.getLine(), ranges);
    return redactedSource.equals(line.getSource()) ? line : line.toBuilder().setSource(redactedSource).build();
  }

  private static String redactWholeLine(String source) {
    int indentationLength = 0;
    while (indentationLength < source.length() && (source.charAt(indentationLength) == ' ' || source.charAt(indentationLength) == '\t')) {
      indentationLength++;
    }
    return source.substring(0, indentationLength) + "*".repeat(source.length() - indentationLength);
  }

  private static String redactRanges(String source, int lineNumber, List<DbCommons.TextRange> ranges) {
    char[] redactedCharacters = null;
    for (DbCommons.TextRange range : ranges) {
      int startLine = range.getStartLine();
      int endLine = range.hasEndLine() ? range.getEndLine() : startLine;
      int startOffset = lineNumber == startLine ? range.getStartOffset() : 0;
      int endOffset = lineNumber == endLine ? range.getEndOffset() : source.length();
      if (startOffset < 0 || endOffset <= startOffset || endOffset > source.length()) {
        return redactWholeLine(source);
      }
      if (redactedCharacters == null) {
        redactedCharacters = source.toCharArray();
      }
      Arrays.fill(redactedCharacters, startOffset, endOffset, '*');
    }
    return redactedCharacters == null ? source : new String(redactedCharacters);
  }
}
