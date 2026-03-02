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

import { diffLines } from 'diff';
import hljs from 'highlight.js/lib/core';
import * as React from 'react';
import { DiffSourceLine } from './FixDiffTypes';
import { escapeHtml, splitLines } from './FixDiffUtils';

interface UseFixDiffSourceLinesParams {
  originalSource: string | undefined;
  mergedSource: string | undefined;
  language: string;
}

export function UseFixDiffSourceLines({
  originalSource,
  mergedSource,
  language,
}: UseFixDiffSourceLinesParams): DiffSourceLine[] {
  return React.useMemo(() => {
    if (!originalSource || !mergedSource) {
      return [];
    }

    const diff = diffLines(originalSource, mergedSource);
    if (!diff || diff.length === 0) {
      return [];
    }

    const sourceLines: DiffSourceLine[] = [];
    let originalLineNumber = 1; // Line number in the original file
    let modifiedLineNumber = 1; // Line number in the modified file

    diff.forEach((part) => {
      const lines = splitLines(part.value);

      lines.forEach((line, lineIndex) => {
        // Skip trailing empty line from split
        if (line === '' && lineIndex === lines.length - 1 && lines.length > 1) {
          return;
        }

        let highlightedHtml: string;
        try {
          const actualLanguage = hljs.getLanguage(language) ? language : 'plaintext';
          const highlighted = hljs.highlight(line, {
            ignoreIllegals: true,
            language: actualLanguage,
          });
          highlightedHtml = highlighted.value;
        } catch {
          highlightedHtml = escapeHtml(line);
        }

        const uniqueId = `${part.added ? 'added' : part.removed ? 'removed' : 'unchanged'}-${originalLineNumber}-${modifiedLineNumber}-${sourceLines.length}`;

        if (part.removed) {
          // Removed line: show original line number, no modified line number
          sourceLines.push({
            line: originalLineNumber,
            code: highlightedHtml,
            isNew: false,
            isRemoved: true,
            isAdded: false,
            originalLineNumber: originalLineNumber,
            modifiedLineNumber: undefined,
            uniqueId,
          });
          originalLineNumber++;
        } else if (part.added) {
          // Added line: no original line number, show modified line number
          sourceLines.push({
            line: modifiedLineNumber,
            code: highlightedHtml,
            isNew: true,
            isRemoved: false,
            isAdded: true,
            originalLineNumber: undefined,
            modifiedLineNumber: modifiedLineNumber,
            uniqueId,
          });
          modifiedLineNumber++;
        } else {
          // Unchanged line: both line numbers advance
          sourceLines.push({
            line: modifiedLineNumber,
            code: highlightedHtml,
            isNew: false,
            isRemoved: false,
            isAdded: false,
            originalLineNumber: originalLineNumber,
            modifiedLineNumber: modifiedLineNumber,
            uniqueId,
          });
          originalLineNumber++;
          modifiedLineNumber++;
        }
      });
    });

    return sourceLines;
  }, [originalSource, mergedSource, language]);
}

