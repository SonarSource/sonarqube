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

import classNames from 'classnames';
import { times } from 'lodash';
import * as React from 'react';
import { LineCoverage, LineMeta, LineNumber, LineWrapper } from '~design-system';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getCodeUrl, getPathUrlAsString } from '../../../helpers/urls';
import { Issue, LinearIssueLocation, SourceLine } from '../../../types/types';
import { useSourceViewerContext } from '../SourceViewerContext';
import './Line.css';
import { LineCode } from './LineCode';
import LineDuplicationBlock from './LineDuplicationBlock';
import LineIssuesIndicator from './LineIssuesIndicator';
import LineOptionsPopup from './LineOptionsPopup';
import LineSCM from './LineSCM';

export interface LineProps {
  children?: React.ReactNode;
  displayAllIssues?: boolean;
  displayCoverage: boolean;
  displayCoverageUnderline: boolean;
  displayDuplications: boolean;
  displayIssues: boolean;
  displayLineNumberOptions?: boolean;
  displayLocationMarkers?: boolean;
  displayNewCodeUnderline: boolean;
  displaySCM?: boolean;
  duplications: number[];
  duplicationsCount: number;
  firstLineNumber: number;
  hideLocationIndex?: boolean;
  highlighted: boolean;
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  highlightedSymbols: string[] | undefined;
  issueLocations: LinearIssueLocation[];
  issues: Issue[];
  line: SourceLine;
  loadDuplications: (line: SourceLine) => void;
  onIssueSelect: (issueKey: string) => void;
  onIssueUnselect: () => void;
  onIssuesClose: (line: SourceLine) => void;
  onIssuesOpen: (line: SourceLine) => void;
  onLineMouseEnter: (line: number) => void;
  onLineMouseLeave: (line: number) => void;
  onLocationSelect: ((x: number) => void) | undefined;
  onSymbolClick: (symbols: string[]) => void;
  openIssues: boolean;
  previousLine: SourceLine | undefined;
  renderDuplicationPopup: (index: number, line: number) => React.ReactNode;
  scrollToUncoveredLine?: boolean;
  secondaryIssueLocations: LinearIssueLocation[];
}

export default function Line(props: LineProps) {
  const {
    children,
    displayAllIssues,
    displayCoverage,
    displayDuplications,
    displayLineNumberOptions = true,
    displayLocationMarkers,
    highlightedLocationMessage,
    displayNewCodeUnderline,
    displayIssues,
    displaySCM = true,
    duplications,
    duplicationsCount,
    firstLineNumber,
    highlighted,
    highlightedSymbols,
    issueLocations,
    issues,
    line,
    openIssues,
    previousLine,
    scrollToUncoveredLine,
    secondaryIssueLocations,
    displayCoverageUnderline,
    hideLocationIndex,
    onLineMouseEnter,
    onLineMouseLeave,
  } = props;

  const handleIssuesIndicatorClick = () => {
    if (props.openIssues) {
      props.onIssuesClose(props.line);
      props.onIssueUnselect();
    } else {
      props.onIssuesOpen(props.line);

      const { issues } = props;
      if (issues.length > 0) {
        props.onIssueSelect(issues[0].key);
      }
    }
  };

  const blocksLoaded = duplicationsCount > 0;

  const handleLineMouseEnter = React.useCallback(
    () => onLineMouseEnter(line.line),
    [line.line, onLineMouseEnter],
  );

  const handleLineMouseLeave = React.useCallback(
    () => onLineMouseLeave(line.line),
    [line.line, onLineMouseLeave],
  );

  const { branchLike, file } = useSourceViewerContext();
  const permalink = getPathUrlAsString(
    getCodeUrl(file.project, branchLike, file.key, line.line),
    false,
  );

  const getStatusTooltip = (line: SourceLine) => {
    switch (line.coverageStatus) {
      case 'uncovered':
        return line.conditions
          ? translateWithParameters('source_viewer.tooltip.uncovered.conditions', line.conditions)
          : translate('source_viewer.tooltip.uncovered');
      case 'covered':
        return line.conditions
          ? translateWithParameters('source_viewer.tooltip.covered.conditions', line.conditions)
          : translate('source_viewer.tooltip.covered');
      case 'partially-covered':
        return line.conditions
          ? translateWithParameters(
              'source_viewer.tooltip.partially-covered.conditions',
              line.coveredConditions ?? 0,
              line.conditions,
            )
          : translate('source_viewer.tooltip.partially-covered');
      default:
        return undefined;
    }
  };

  const status = getStatusTooltip(line);

  return (
    <LineWrapper
      data-line-number={line.line}
      displayCoverage={displayCoverage}
      displaySCM={displaySCM}
      duplicationsCount={!duplicationsCount && displayDuplications ? 1 : duplicationsCount}
      highlighted={highlighted}
      onMouseEnter={handleLineMouseEnter}
      onMouseLeave={handleLineMouseLeave}
      className={classNames('it__source-line', { 'it__source-line-filtered': line.isNew })}
    >
      <LineNumber
        displayOptions={displayLineNumberOptions}
        firstLineNumber={firstLineNumber}
        lineNumber={line.line}
        ariaLabel={translateWithParameters('source_viewer.line_X', line.line)}
        popup={<LineOptionsPopup line={line} permalink={permalink} />}
      />

      {displaySCM && <LineSCM line={line} previousLine={previousLine} />}

      {displayIssues && !displayAllIssues ? (
        <LineIssuesIndicator
          issues={issues}
          issuesOpen={openIssues}
          line={line}
          onClick={handleIssuesIndicatorClick}
        />
      ) : (
        <LineMeta data-line-number={line.line} />
      )}

      {displayDuplications && (
        <LineDuplicationBlock
          blocksLoaded={blocksLoaded}
          duplicated={!blocksLoaded ? Boolean(line.duplicated) : duplications.includes(0)}
          index={0}
          key={0}
          line={line}
          onClick={props.loadDuplications}
          renderDuplicationPopup={props.renderDuplicationPopup}
        />
      )}

      {blocksLoaded &&
        times(duplicationsCount - 1, (index) => {
          return (
            <LineDuplicationBlock
              blocksLoaded={blocksLoaded}
              duplicated={duplications.includes(index + 1)}
              index={index + 1}
              key={index + 1}
              line={line}
              renderDuplicationPopup={props.renderDuplicationPopup}
            />
          );
        })}

      {displayCoverage && (
        <LineCoverage
          lineNumber={line.line}
          scrollToUncoveredLine={scrollToUncoveredLine}
          status={status}
          coverageStatus={line.coverageStatus}
        />
      )}

      <LineCode
        displayCoverageUnderline={displayCoverage && displayCoverageUnderline}
        displayLocationMarkers={displayLocationMarkers}
        displayNewCodeUnderlineLabel={displayNewCodeUnderline}
        hideLocationIndex={hideLocationIndex}
        highlightedLocationMessage={highlightedLocationMessage}
        highlightedSymbols={highlightedSymbols}
        issueLocations={issueLocations}
        line={line}
        onLocationSelect={props.onLocationSelect}
        onSymbolClick={props.onSymbolClick}
        previousLine={previousLine}
        secondaryIssueLocations={secondaryIssueLocations}
      >
        {children}
      </LineCode>
    </LineWrapper>
  );
}
