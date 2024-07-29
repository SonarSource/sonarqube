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

import { ICell, isCode, isMarkdown } from '@jupyterlab/nbformat';
import { Spinner } from '@sonarsource/echoes-react';
import { FlagMessage, hljsIssueIndicatorPlugin, hljsUnderlinePlugin } from 'design-system';
import React, { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import {
  JupyterCodeCell,
  JupyterMarkdownCell,
} from '~sonar-aligned/components/SourceViewer/JupyterNotebookViewer';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { JsonIssueMapper } from '~sonar-aligned/helpers/json-issue-mapper';
import { getComponentIssuesUrl } from '~sonar-aligned/helpers/urls';
import { ComponentContext } from '../../app/components/componentContext/ComponentContext';
import { pathToCursorInCell } from '../../apps/issues/jupyter-notebook/utils';
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
type IssuesByLine = {
  [line: number]: {
    end: { cursorOffset: number; line: number };
    issue: Issue;
    start: { cursorOffset: number; line: number };
  }[];
};
type IssueKeysByLine = { [line: number]: string[] };

const DELAY_FOR_PORTAL_INDEX_ELEMENT = 200;

export default function SourceViewerPreview(props: Readonly<Props>) {
  const { component, branchLike } = props;
  const [issues, setIssues] = useState<Issue[]>([]);
  const [issuesByCell, setIssuesByCell] = useState<IssuesByCell>({});
  const [renderedCells, setRenderedCells] = useState<ICell[] | null>([]);
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
      return JSON.parse(data) as { cells: ICell[] };
    } catch (error) {
      return null;
    }
  }, [data]);

  const [hasRendered, setHasRendered] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      const issues = await loadIssues(component, branchLike);
      setIssues(issues);
    };

    fetchData();
  }, [component, branchLike]);

  useEffect(() => {
    const newIssuesByCell: IssuesByCell = {};

    if (!jupyterNotebook) {
      return;
    }

    issues.forEach((issue) => {
      if (!issue.textRange) {
        return;
      }

      if (typeof data !== 'string') {
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

      if (startOffset.cell !== endOffset.cell) {
        setRenderedCells(null);
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

    setRenderedCells(jupyterNotebook?.cells);
    setIssuesByCell(newIssuesByCell);
  }, [issues, data, jupyterNotebook]);

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

  if (!renderedCells) {
    return (
      <FlagMessage className="sw-mt-2" variant="warning">
        {translate('source_viewer.jupyter.preview.error')}
      </FlagMessage>
    );
  }

  return (
    <>
      <RenderJupyterNotebook
        cells={renderedCells}
        issuesByCell={issuesByCell}
        onRender={() => setHasRendered(true)}
      />
      {hasRendered && issues && componentContext && branchLike && (
        <IssueIndicators
          issuesByCell={issuesByCell}
          component={componentContext}
          branchLike={branchLike}
        />
      )}
    </>
  );
}

type JupyterNotebookProps = {
  cells: ICell[];
  issuesByCell: IssuesByCell;
  onRender: () => void;
};

function mapIssuesToIssueKeys(issuesByLine: IssuesByLine): IssueKeysByLine {
  return Object.entries(issuesByLine).reduce((acc, [line, issues]) => {
    acc[Number(line)] = issues.map(({ issue }) => issue.key);
    return acc;
  }, {} as IssueKeysByLine);
}

function RenderJupyterNotebook({ cells, issuesByCell, onRender }: Readonly<JupyterNotebookProps>) {
  useEffect(() => {
    // the `issue-key-${issue.key}` need to be rendered before we trigger the IssueIndicators below
    setTimeout(onRender, DELAY_FOR_PORTAL_INDEX_ELEMENT);
  }, [onRender]);

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
    <>
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
    </>
  );
}

type IssueIndicatorsProps = {
  branchLike: BranchLike;
  component: Component;
  issuesByCell: IssuesByCell;
};

function IssueIndicators({ issuesByCell, component, branchLike }: Readonly<IssueIndicatorsProps>) {
  const location = useLocation();
  const query = parseQuery(location.query);
  const router = useRouter();

  const issuePortals = Object.entries(issuesByCell).flatMap(([, issuesByLine]) =>
    Object.entries(issuesByLine).map(([lineIndex, issues]) => {
      const firstIssue = issues[0].issue;
      const onlyIssues = issues.map(({ issue }) => issue);
      const urlQuery = {
        ...getBranchLikeQuery(branchLike),
        ...serializeQuery(query),
        open: firstIssue.key,
      };
      const issueUrl = component?.key
        ? getComponentIssuesUrl(component?.key, urlQuery)
        : getIssuesUrl(urlQuery);
      const portalIndexElement = document.getElementById(`issue-key-${firstIssue.key}`);
      return portalIndexElement ? (
        <span key={`${firstIssue.key}-${lineIndex}`}>
          {createPortal(
            <LineIssuesIndicator
              issues={onlyIssues}
              onClick={() => router.navigate(issueUrl)}
              line={{ line: Number(lineIndex) }}
              as="span"
            />,
            portalIndexElement,
          )}
        </span>
      ) : null;
    }),
  );

  return <>{issuePortals}</>;
}
