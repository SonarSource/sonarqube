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

import { ICell, INotebookContent, isCode, isMarkdown } from '@jupyterlab/nbformat';
import { Spinner } from '@sonarsource/echoes-react';
import {
  Card,
  FlagMessage,
  hljsIssueIndicatorPlugin,
  hljsUnderlinePlugin,
  UnderlineRangePosition,
} from 'design-system';

import React, { forwardRef, useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { To } from 'react-router-dom';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import {
  JupyterCodeCell,
  JupyterMarkdownCell,
} from '~sonar-aligned/components/SourceViewer/JupyterNotebookViewer';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { getOffsetsForIssue } from '~sonar-aligned/helpers/json-issue-mapper';
import { getComponentIssuesUrl } from '~sonar-aligned/helpers/urls';
import { ComponentContext } from '../../app/components/componentContext/ComponentContext';
import { parseQuery, serializeQuery } from '../../apps/issues/utils';
import { translate } from '../../helpers/l10n';
import { getIssuesUrl } from '../../helpers/urls';
import { useRawSourceQuery } from '../../queries/sources';
import { BranchLike } from '../../types/branch-like';
import { Component, Issue } from '../../types/types';
import LineIssuesIndicator from './components/LineIssuesIndicator';
import loadIssues from './helpers/loadIssues';

export interface Props {
  branchLike: BranchLike | undefined;
  component: string;
}

type IssuesByCell = { [key: number]: IssuesByLine };
type IssueByLine = {
  end: UnderlineRangePosition;
  issue: Issue;
  start: UnderlineRangePosition;
};
type IssuesByLine = {
  [line: number]: IssueByLine[];
};
type IssueKeysByLine = { [line: number]: string[] };
type IssueIndicatorsProps = {
  branchLike: BranchLike;
  component: Component;
  issuesByCell: IssuesByCell;
  jupyterRef: React.RefObject<HTMLDivElement>;
};
type IssueMapper = {
  issueUrl: To;
  key: string;
  lineIndex: number;
  onlyIssues: Issue[];
};

export default function SourceViewerPreview(props: Readonly<Props>) {
  const { component, branchLike } = props;
  const [issues, setIssues] = useState<Issue[]>([]);
  const [issuesByCell, setIssuesByCell] = useState<IssuesByCell>({});
  const jupyterNotebookRef = useRef<HTMLDivElement>(null);

  const { data, isLoading } = useRawSourceQuery({
    key: component,
    ...getBranchLikeQuery(branchLike),
  });
  const { component: componentContext } = React.useContext(ComponentContext);

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

  useEffect(() => {
    const fetchData = async () => {
      const issues = await loadIssues(component, branchLike);
      setIssues(issues);
    };

    fetchData();
  }, [component, branchLike]);

  useEffect(() => {
    processIssuesByCell(issues, data, setIssuesByCell);
  }, [issues, data]);

  if (isLoading) {
    return <Spinner isLoading={isLoading} />;
  }

  if (typeof data !== 'string') {
    return (
      <FlagMessage className="sw-mt-2" variant="warning">
        {translate('component_viewer.no_component')}
      </FlagMessage>
    );
  }

  if (!jupyterNotebook?.cells) {
    return (
      <FlagMessage className="sw-mt-2" variant="warning">
        {translate('source_viewer.jupyter.preview.error')}
      </FlagMessage>
    );
  }

  return (
    <Card>
      <JupyterNotebookSourceViewer
        cells={jupyterNotebook.cells}
        issuesByCell={issuesByCell}
        ref={jupyterNotebookRef}
      />
      {issues && componentContext && branchLike && (
        <IssueIndicators
          issuesByCell={issuesByCell}
          component={componentContext}
          branchLike={branchLike}
          jupyterRef={jupyterNotebookRef}
        />
      )}
    </Card>
  );
}

type JupyterNotebookProps = {
  cells: ICell[];
  issuesByCell: IssuesByCell;
};

function mapIssuesToIssueKeys(issuesByLine: IssuesByLine): IssueKeysByLine {
  return Object.entries(issuesByLine).reduce((acc, [line, issues]) => {
    acc[Number(line)] = issues.map(({ issue }) => issue.key);
    return acc;
  }, {} as IssueKeysByLine);
}

const JupyterNotebookSourceViewer = forwardRef<HTMLDivElement, JupyterNotebookProps>(
  ({ cells, issuesByCell }, ref) => {
    const buildCellsBlocks = useMemo(() => {
      return cells.map((cell: ICell, index: number) => {
        let sourceLines = Array.isArray(cell.source) ? cell.source : [cell.source];
        const issuesByLine = issuesByCell[index];
        if (!issuesByLine) {
          return {
            cell,
            sourceLines,
          };
        }
        const issues = mapIssuesToIssueKeys(issuesByLine);
        const flatIssues = Object.entries(issuesByLine).flatMap(([, issues]) => issues);

        sourceLines = hljsUnderlinePlugin.tokenize(sourceLines, flatIssues);
        sourceLines = hljsIssueIndicatorPlugin.addIssuesToLines(sourceLines, issues);

        return {
          cell,
          sourceLines,
        };
      });
    }, [cells, issuesByCell]);

    return (
      <div ref={ref}>
        {buildCellsBlocks.map((element, index) => {
          const { cell, sourceLines } = element;
          if (isCode(cell)) {
            return (
              <JupyterCodeCell
                source={sourceLines}
                outputs={cell.outputs}
                key={`${cell.cell_type}-${index}`}
              />
            );
          } else if (isMarkdown(cell)) {
            return <JupyterMarkdownCell cell={cell} key={`${cell.cell_type}-${index}`} />;
          }
          return null;
        })}
      </div>
    );
  },
);

JupyterNotebookSourceViewer.displayName = 'JupyterNotebookSourceViewer';

function IssueIndicators({
  issuesByCell,
  component,
  branchLike,
  jupyterRef,
}: Readonly<IssueIndicatorsProps>) {
  const location = useLocation();
  const query = parseQuery(location.query);
  const onlyIssuesMap = (issues: IssueByLine[]) => issues.map(({ issue }) => issue);
  const mappedIssues = useMemo(() => {
    return Object.entries(issuesByCell).flatMap(([, issuesByLine]) =>
      Object.entries(issuesByLine).map(([, issues]) => {
        const firstIssue = issues[0].issue;
        const onlyIssues = onlyIssuesMap(issues);
        const urlQuery = {
          ...getBranchLikeQuery(branchLike),
          ...serializeQuery(query),
          open: firstIssue.key,
        };
        const issueUrl = component?.key
          ? getComponentIssuesUrl(component?.key, urlQuery)
          : getIssuesUrl(urlQuery);
        return { key: firstIssue.key, issueUrl, onlyIssues, lineIndex: issues[0].start.line };
      }),
    );
    // we only need to recompute this with new issues
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [issuesByCell]);

  return mappedIssues.map((mappedIssues) => (
    <PortalLineIssuesIndicator
      key={mappedIssues.key}
      issueMapper={mappedIssues}
      jupyterRef={jupyterRef}
    />
  ));
}

/* 
Creates a react portal bound to an element id `issue-key-${key}` that represents the first issue 
present on a virtual line of code. The element id is generated from executing the 
HljsIssueIndicatorPlugin.addIssuesToLines function on the source code of a Jupyter notebook cell.
*/
function PortalLineIssuesIndicator(props: {
  issueMapper: IssueMapper;
  jupyterRef: React.RefObject<HTMLDivElement>;
}) {
  const { jupyterRef, issueMapper } = props;
  const router = useRouter();

  // We use this state to force-re-render the component when the jupyterRef changes
  // eslint-disable-next-line react/hook-use-state
  const [, setMutationCount] = useState(0);

  useEffect(() => {
    if (!jupyterRef.current) {
      return;
    }
    setMutationCount((count) => count + 1);
  }, [jupyterRef]);

  const { key, lineIndex, onlyIssues, issueUrl } = issueMapper;
  const element = document.getElementById(`issue-key-${key}`);

  if (!element) {
    return null;
  }

  return createPortal(
    <LineIssuesIndicator
      issues={onlyIssues}
      onClick={() => router.navigate(issueUrl)}
      line={{ line: Number(lineIndex) }}
      as="span"
    />,
    element,
  );
}

/**
 * Processes issues and maps them to their corresponding cells and lines in a Jupyter notebook.
 * This function updates the state with a new mapping of issues by cell.
 *
 * @param {Array} issues - The list of issues to process.
 * @param {string} data - The JSON data representing the notebook.
 * @param {Function} setIssuesByCell - Function to update the state with the new issues mapping.
 */
function processIssuesByCell(
  issues: Issue[],
  data: string | null | undefined,
  setIssuesByCell: Function,
) {
  const newIssuesByCell: IssuesByCell = {};

  issues.forEach((issue) => {
    if (typeof data !== 'string') {
      return;
    }

    const { startOffset, endOffset } = getOffsetsForIssue(issue, data);

    // failed to parse the issue offsets, skip the issue
    if (!startOffset || !endOffset) {
      return;
    }

    const { cell } = startOffset;

    if (!newIssuesByCell[cell]) {
      newIssuesByCell[cell] = {};
    }

    if (!newIssuesByCell[cell][startOffset.line]) {
      newIssuesByCell[cell][startOffset.line] = [{ issue, start: startOffset, end: endOffset }];
    }

    const existingIssues = newIssuesByCell[cell][startOffset.line];
    const issueExists = existingIssues.some(
      ({ issue: existingIssue }) => existingIssue.key === issue.key,
    );

    if (!issueExists) {
      newIssuesByCell[cell][startOffset.line].push({ issue, start: startOffset, end: endOffset });
    }
  });

  setIssuesByCell(newIssuesByCell);
}
