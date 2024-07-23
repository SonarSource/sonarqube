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
import { ICell, INotebookContent } from '@jupyterlab/nbformat';
import { Spinner } from '@sonarsource/echoes-react';
import {
  FlagMessage,
  hljsUnderlinePlugin,
  IssueMessageHighlighting,
  LineFinding,
} from 'design-system';
import React from 'react';
import { JupyterCell } from '~sonar-aligned/components/SourceViewer/JupyterNotebookViewer';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { JsonIssueMapper } from '~sonar-aligned/helpers/json-issue-mapper';
import { translate } from '../../../helpers/l10n';
import { useRawSourceQuery } from '../../../queries/sources';
import { BranchLike } from '../../../types/branch-like';
import { Issue } from '../../../types/types';
import { pathToCursorInCell } from './utils';

export interface JupyterNotebookIssueViewerProps {
  branchLike?: BranchLike;
  issue: Issue;
}

export function JupyterNotebookIssueViewer(props: Readonly<JupyterNotebookIssueViewerProps>) {
  const { issue, branchLike } = props;
  const { data, isLoading } = useRawSourceQuery({
    key: issue.component,
    ...getBranchLikeQuery(branchLike),
  });
  const [renderedCells, setRenderedCells] = React.useState<ICell[] | null>(null);

  React.useEffect(() => {
    if (!issue.textRange || typeof data !== 'string') {
      return;
    }

    let jupyterNotebook: INotebookContent;
    try {
      jupyterNotebook = JSON.parse(data);
    } catch (error) {
      setRenderedCells(null);
      return;
    }

    const mapper = new JsonIssueMapper(data);
    const start = mapper.lineOffsetToCursorPosition(
      issue.textRange.startLine,
      issue.textRange.startOffset,
    );
    const end = mapper.lineOffsetToCursorPosition(
      issue.textRange.endLine,
      issue.textRange.endOffset,
    );
    const startOffset = pathToCursorInCell(mapper.get(start));
    const endOffset = pathToCursorInCell(mapper.get(end));
    if (!startOffset || !endOffset) {
      setRenderedCells(null);
      return;
    }

    if (startOffset.cell === endOffset.cell) {
      const startCell = jupyterNotebook.cells[startOffset.cell];
      startCell.source = Array.isArray(startCell.source) ? startCell.source : [startCell.source];
      startCell.source = hljsUnderlinePlugin.tokenize(startCell.source, [
        {
          start: startOffset,
          end: endOffset,
        },
      ]);
    } else {
      // Each cell is a separate code block, so we have to underline them separately
      // We underilne the first cell from the start offset to the end of the cell, and the last cell from the start of the cell to the end offset
      const startCell = jupyterNotebook.cells[startOffset.cell];
      startCell.source = Array.isArray(startCell.source) ? startCell.source : [startCell.source];
      startCell.source = hljsUnderlinePlugin.tokenize(startCell.source, [
        {
          start: startOffset,
          end: {
            line: startCell.source.length - 1,
            cursorOffset: startCell.source[startCell.source.length - 1].length,
          },
        },
      ]);
      const endCell = jupyterNotebook.cells[endOffset.cell];
      endCell.source = Array.isArray(endCell.source) ? endCell.source : [endCell.source];
      endCell.source = hljsUnderlinePlugin.tokenize(endCell.source, [
        {
          start: { line: 0, cursorOffset: 0 },
          end: endOffset,
        },
      ]);
    }

    const cells = Array.from(new Set([startOffset.cell, endOffset.cell])).map(
      (cellIndex) => jupyterNotebook.cells[cellIndex],
    );

    setRenderedCells(cells);
  }, [issue, data]);

  if (isLoading) {
    return <Spinner />;
  }

  if (!renderedCells) {
    return (
      <FlagMessage className="sw-mt-2" variant="warning">
        {translate('issue.preview.jupyter_notebook.error')}
      </FlagMessage>
    );
  }

  return (
    <>
      <LineFinding
        issueKey={issue.key}
        message={
          <IssueMessageHighlighting
            message={issue.message}
            messageFormattings={issue.messageFormattings}
          />
        }
        selected
      />
      {renderedCells.map((cell, index) => (
        <JupyterCell key={'cell-' + index} cell={cell} />
      ))}
    </>
  );
}
