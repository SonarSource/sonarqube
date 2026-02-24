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

import { useTheme } from '@emotion/react';
import {
  CodeViewerExpander,
  SonarCodeColorizer,
  UnfoldDownIcon,
  UnfoldUpIcon,
  themeColor,
} from '~design-system';
import { translate } from '../../helpers/l10n';
import { BranchLike } from '../../types/branch-like';
import { SourceViewerFile } from '../../types/types';
import { SourceViewerContext } from '../SourceViewer/SourceViewerContext';
import {
  DiffCodeCell,
  DiffCodeContent,
  DiffLineNumberCell,
  DiffLineRow,
  DiffMarker,
  DiffMarkerCell,
} from './FixDiffStyles';
import { DiffSourceLine } from './FixDiffTypes';

interface FixDiffTableProps {
  branchLike?: BranchLike;
  displayedLines: DiffSourceLine[];
  file: SourceViewerFile;
  firstDisplayedLineNumber: number;
  lastDisplayedLineNumber: number;
  minLineNumber: number;
  maxLineNumber: number;
  onExpandUp: () => void;
  onExpandDown: () => void;
}

export function FixDiffTable({
  branchLike,
  displayedLines,
  file,
  firstDisplayedLineNumber,
  lastDisplayedLineNumber,
  minLineNumber,
  maxLineNumber,
  onExpandUp,
  onExpandDown,
}: Readonly<FixDiffTableProps>) {
  const theme = useTheme();
  const borderColor = themeColor('codeLineBorder')({ theme });

  return (
    <div
      className="it__source-viewer-code"
      style={{ border: `1px solid ${borderColor}` }}
    >
      <SonarCodeColorizer>
        <SourceViewerContext.Provider value={{ branchLike, file }}>
          {displayedLines.length > 0 && firstDisplayedLineNumber > minLineNumber && (
            <CodeViewerExpander
              direction="UP"
              className="sw-flex sw-justify-start sw-items-center sw-py-1 sw-px-2"
              onClick={onExpandUp}
            >
              <UnfoldUpIcon aria-label={translate('source_viewer.expand_above')} />
            </CodeViewerExpander>
          )}
          {displayedLines.length > 0 ? (
            <table className="sw-w-full" style={{ borderCollapse: 'collapse', borderSpacing: 0 }}>
              <tbody data-fix-diff-tbody="true">
                {displayedLines.map((line, displayedIndex) => {
                  const diffLine = line as DiffSourceLine;
                  const isAdded = diffLine.isAdded === true;
                  const isRemoved = diffLine.isRemoved === true;
                  const originalLineNum = diffLine.originalLineNumber;
                  const modifiedLineNum = diffLine.modifiedLineNumber;

                  // Determine background color
                  const bgColor = isRemoved
                    ? themeColor('codeLineUncoveredUnderline')({ theme }) // Red
                    : isAdded
                    ? themeColor('codeLineCoveredUnderline')({ theme }) // Green
                    : 'transparent';

                  return (
                    <DiffLineRow
                      key={diffLine.uniqueId ?? `${line.line}-${displayedIndex}`}
                      $bgColor={bgColor}
                      data-fix-diff-id={diffLine.uniqueId ?? `${line.line}-${displayedIndex}`}
                    >
                      <DiffLineNumberCell $align="right" $bgColor={bgColor}>
                        {originalLineNum !== undefined ? String(originalLineNum) : ''}
                      </DiffLineNumberCell>
                      <DiffLineNumberCell $align="right" $bgColor={bgColor}>
                        {modifiedLineNum !== undefined ? String(modifiedLineNum) : ''}
                      </DiffLineNumberCell>
                      <DiffMarkerCell $bgColor={bgColor}>
                        {isRemoved && <DiffMarker $type="removed">-</DiffMarker>}
                        {isAdded && <DiffMarker $type="added">+</DiffMarker>}
                      </DiffMarkerCell>
                      <DiffCodeCell $bgColor={bgColor}>
                        <DiffCodeContent
                          dangerouslySetInnerHTML={{ __html: line.code ?? '' }}
                        />
                      </DiffCodeCell>
                    </DiffLineRow>
                  );
                })}
              </tbody>
            </table>
          ) : (
            <div className="sw-p-4 sw-text-center sw-text-textSecondary">
              {translate('issue.tabs.fix_diff.empty')}
            </div>
          )}
          {displayedLines.length > 0 && lastDisplayedLineNumber < maxLineNumber && (
            <CodeViewerExpander
              className="sw-flex sw-justify-start sw-items-center sw-py-1 sw-px-2"
              onClick={onExpandDown}
              direction="DOWN"
            >
              <UnfoldDownIcon aria-label={translate('source_viewer.expand_below')} />
            </CodeViewerExpander>
          )}
        </SourceViewerContext.Provider>
      </SonarCodeColorizer>
    </div>
  );
}

