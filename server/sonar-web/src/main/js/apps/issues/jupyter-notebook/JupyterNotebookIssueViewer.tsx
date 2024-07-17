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
import { INotebookContent } from '@jupyterlab/nbformat';
import { Spinner } from '@sonarsource/echoes-react';
import { FlagMessage, IssueMessageHighlighting, LineFinding } from 'design-system';
import React, { useMemo } from 'react';
import { JupyterCell } from '~sonar-aligned/components/SourceViewer/JupyterNotebookViewer';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { JsonIssueMapper } from '~sonar-aligned/helpers/json-issue-mapper';
import { translate } from '../../../helpers/l10n';
import { useRawSourceQuery } from '../../../queries/sources';
import { BranchLike } from '../../../types/branch-like';
import { Issue } from '../../../types/types';
import { JupyterNotebookCursorPath } from './types';
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
  const [startOffset, setStartOffset] = React.useState<JupyterNotebookCursorPath | null>(null);
  const [endPath, setEndPath] = React.useState<JupyterNotebookCursorPath | null>(null);

  const jupyterNotebook = useMemo(() => {
    if (typeof data !== 'string') {
      return null;
    }
    try {
      return JSON.parse(data) as INotebookContent;
    } catch (error) {
      return null;
    }
  }, [data]);

  React.useEffect(() => {
    if (typeof data !== 'string') {
      return;
    }

    if (!issue.textRange) {
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
    if (
      startOffset &&
      endOffset &&
      startOffset.cell === endOffset.cell &&
      startOffset.line === endOffset.line
    ) {
      setStartOffset(startOffset);
      setEndPath(endOffset);
    }
  }, [issue, data]);

  if (isLoading) {
    return <Spinner />;
  }

  if (!jupyterNotebook || !startOffset || !endPath) {
    return (
      <FlagMessage className="sw-mt-2" variant="warning">
        {translate('issue.preview.jupyter_notebook.error')}
      </FlagMessage>
    );
  }

  // Cells to display
  const cells = Array.from(new Set([startOffset.cell, endPath.cell])).map(
    (cellIndex) => jupyterNotebook.cells[cellIndex],
  );

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
      {cells.map((cell, index) => (
        <JupyterCell key={'cell-' + index} cell={cell} />
      ))}
    </>
  );
}
