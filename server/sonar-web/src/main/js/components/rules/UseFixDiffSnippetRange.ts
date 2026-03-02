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

import * as React from 'react';
import { DiffSourceLine, SnippetRange } from './FixDiffTypes';
import { determineSnippetLineCount } from './FixDiffUtils';

interface IssueContext {
  sourceSnippetStartLine?: number;
  sourceSnippetEndLine?: number;
  snippetViolationLine?: number;
  codesnippet?: string;
}

interface UseFixDiffSnippetRangeParams {
  sourceLines: DiffSourceLine[];
  issueContext: IssueContext | undefined;
  linesAbove?: number;
  linesBelow?: number;
}

const DEFAULT_LINES_ABOVE = 5;
const DEFAULT_LINES_BELOW = 5;
const DEFAULT_SNIPPET_SIZE = 20; // Show 20 lines if no issue context

export function UseFixDiffSnippetRange({
  sourceLines,
  issueContext,
  linesAbove = DEFAULT_LINES_ABOVE,
  linesBelow = DEFAULT_LINES_BELOW,
}: UseFixDiffSnippetRangeParams) {
  // Calculate min and max line numbers
  const minLineNumber = React.useMemo(() => {
    if (sourceLines.length === 0) return 1;
    const lineNumbers = sourceLines.map((line) => {
      return line.modifiedLineNumber ?? line.originalLineNumber ?? line.line ?? 1;
    });
    return Math.min(...lineNumbers, 1);
  }, [sourceLines]);

  const maxLineNumber = React.useMemo(() => {
    if (sourceLines.length === 0) return 1;
    const lineNumbers = sourceLines.map((line) => {
      return line.modifiedLineNumber ?? line.originalLineNumber ?? line.line ?? 1;
    });
    return Math.max(...lineNumbers, 1);
  }, [sourceLines]);

  // Find the first and last changed lines (added or removed)
  // This is similar to how "where is the issue?" tab finds the issue location
  // We need to find the range that encompasses all changes
  const changedLinesRange = React.useMemo(() => {
    if (sourceLines.length === 0) {
      return { firstChangedLineNum: null, lastChangedLineNum: null };
    }

    let firstChangedLineNum: number | null = null;
    let lastChangedLineNum: number | null = null;

    sourceLines.forEach((line) => {
      const diffLine = line as DiffSourceLine;
      if (diffLine.isAdded || diffLine.isRemoved) {
        // For removed lines, use originalLineNumber (they exist in original file)
        // For added lines, use modifiedLineNumber (they exist in modified file)
        // We want to find the range that covers all changes
        const lineNum = diffLine.isRemoved
          ? diffLine.originalLineNumber
          : diffLine.modifiedLineNumber;

        if (lineNum !== undefined) {
          if (firstChangedLineNum === null || lineNum < firstChangedLineNum) {
            firstChangedLineNum = lineNum;
          }
          if (lastChangedLineNum === null || lineNum > lastChangedLineNum) {
            lastChangedLineNum = lineNum;
          }
        }
      }
    });

    return { firstChangedLineNum, lastChangedLineNum };
  }, [sourceLines]);

  // Calculate initial snippet range based on changed lines
  // This mimics how "where is the issue?" tab shows lines around the issue location
  const initialSnippetRange = React.useMemo(() => {
    if (sourceLines.length === 0) {
      return { start: 1, end: 1 };
    }

    // Priority 1: If we have changed lines (added/removed), show them with buffer
    // This is the main use case - show the lines that were fixed/changed
    if (changedLinesRange.firstChangedLineNum !== null && changedLinesRange.lastChangedLineNum !== null) {
      // Show 5 lines above and below the changed lines (like "where is the issue?" tab)
      const start = Math.max(minLineNumber, changedLinesRange.firstChangedLineNum - linesAbove);
      const end = Math.min(maxLineNumber, changedLinesRange.lastChangedLineNum + linesBelow);
      return { start, end };
    }

    // Priority 2: If we have issue context, use it
    if (issueContext) {
      const snippetStart = issueContext.sourceSnippetStartLine ?? issueContext.snippetViolationLine ?? minLineNumber;
      const snippetEnd =
        issueContext.sourceSnippetEndLine ??
        snippetStart + determineSnippetLineCount(issueContext.codesnippet) - 1;

      // Add buffer lines above and below
      const start = Math.max(minLineNumber, snippetStart - linesAbove);
      const end = Math.min(maxLineNumber, snippetEnd + linesBelow);
      return { start, end };
    }

    // Priority 3: Fallback - show first DEFAULT_SNIPPET_SIZE lines
    return {
      start: minLineNumber,
      end: Math.min(minLineNumber + DEFAULT_SNIPPET_SIZE - 1, maxLineNumber),
    };
  }, [changedLinesRange, issueContext, sourceLines.length, minLineNumber, maxLineNumber, linesAbove, linesBelow]);

  // State for displayed snippet range
  const [snippetRange, setSnippetRange] = React.useState<SnippetRange>({ start: 1, end: 1 });

  // Update snippet range when initial range changes
  React.useEffect(() => {
    if (sourceLines.length > 0 && initialSnippetRange.end >= initialSnippetRange.start && initialSnippetRange.end > 0) {
      setSnippetRange(initialSnippetRange);
    }
  }, [initialSnippetRange, sourceLines.length]);

  return {
    snippetRange,
    setSnippetRange,
    minLineNumber,
    maxLineNumber,
  };
}

