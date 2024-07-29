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
import { ICodeCell, INotebookContent, isCode } from '@jupyterlab/nbformat';
import { Spinner } from '@sonarsource/echoes-react';
import {
  FlagMessage,
  hljsUnderlinePlugin,
  IssueMessageHighlighting,
  LineFinding,
} from 'design-system';
import React from 'react';
import { JupyterCodeCell } from '~sonar-aligned/components/SourceViewer/JupyterNotebookViewer';
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
  const [renderedCells, setRenderedCells] = React.useState<{
    after: ICodeCell;
    before: ICodeCell[];
  } | null>(null);

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
    const startOffset = pathToCursorInCell(
      mapper.get(
        mapper.lineOffsetToCursorPosition(issue.textRange.startLine, issue.textRange.startOffset),
      ),
    );
    const endOffset = pathToCursorInCell(
      mapper.get(
        mapper.lineOffsetToCursorPosition(issue.textRange.endLine, issue.textRange.endOffset),
      ),
    );
    if (!startOffset || !endOffset) {
      setRenderedCells(null);
      return;
    }

    // When the primary location spans over multiple cells, we show all cells that are part of the range
    const cells: ICodeCell[] = jupyterNotebook.cells
      .slice(startOffset.cell, endOffset.cell + 1)
      .filter((cell) => isCode(cell));

    // Split the last cell because we want to show the issue message at the end of the primary location
    const sourceBefore = cells[cells.length - 1].source.slice(0, endOffset.line + 1);
    const sourceAfter = cells[cells.length - 1].source.slice(endOffset.line + 1);
    const lastCell = {
      ...cells[cells.length - 1],
      source: sourceAfter,
    };
    cells[cells.length - 1] = {
      cell_type: 'code',
      source: sourceBefore,
      execution_count: 0,
      outputs: [],
      metadata: {},
    };

    for (let i = 0; i < cells.length; i++) {
      const cell = cells[i];
      cell.source = Array.isArray(cell.source) ? cell.source : [cell.source];

      // Any cell between the first and last cell should be fully underlined
      const start = i === 0 ? startOffset : { line: 0, cursorOffset: 0 };
      const end =
        i === cells.length - 1
          ? endOffset
          : {
              line: cell.source.length - 1,
              cursorOffset: cell.source[cell.source.length - 1].length,
            };

      cell.source = hljsUnderlinePlugin.tokenize(cell.source, [
        {
          start,
          end,
        },
      ]);
    }

    setRenderedCells({
      before: cells,
      after: lastCell,
    });
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
      {renderedCells.before.map((cell, index) => (
        <JupyterCodeCell
          key={'cell-' + index}
          source={cell.source as string[]}
          className={index === renderedCells.before.length - 1 ? '-sw-mb-6 sw-relative' : undefined}
        />
      ))}
      <div className="-sw-mt-4 -sw-mb-4 -sw-ml-1 sw-mr-5 sw-relative sw-z-normal">
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
      </div>
      <JupyterCodeCell
        className="-sw-mt-6 sw-relative"
        source={renderedCells.after.source as string[]}
        outputs={renderedCells.after.outputs}
      />
    </>
  );
}
