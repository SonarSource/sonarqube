/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { BranchLike } from '../../../types/branch-like';
import { Issue, LinearIssueLocation, SourceLine } from '../../../types/types';
import './Line.css';
import LineCode from './LineCode';
import LineCoverage from './LineCoverage';
import LineDuplicationBlock from './LineDuplicationBlock';
import LineIssuesIndicator from './LineIssuesIndicator';
import LineNumber from './LineNumber';
import LineSCM from './LineSCM';

interface Props {
  additionalChild?: React.ReactNode;
  branchLike: BranchLike | undefined;
  displayAllIssues?: boolean;
  displayCoverage: boolean;
  displayDuplications: boolean;
  displayIssueLocationsCount?: boolean;
  displayIssueLocationsLink?: boolean;
  displayIssues: boolean;
  displayLineNumberOptions?: boolean;
  displayLocationMarkers?: boolean;
  displaySCM?: boolean;
  duplications: number[];
  duplicationsCount: number;
  firstLineNumber: number;
  highlighted: boolean;
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  highlightedSymbols: string[] | undefined;
  issueLocations: LinearIssueLocation[];
  issuePopup: { issue: string; name: string } | undefined;
  issues: Issue[];
  last: boolean;
  line: SourceLine;
  loadDuplications: (line: SourceLine) => void;
  onIssueChange: (issue: Issue) => void;
  onIssuePopupToggle: (issueKey: string, popupName: string, open?: boolean) => void;
  onIssuesClose: (line: SourceLine) => void;
  onIssueSelect: (issueKey: string) => void;
  onIssuesOpen: (line: SourceLine) => void;
  onIssueUnselect: () => void;
  onLocationSelect: ((x: number) => void) | undefined;
  onSymbolClick: (symbols: string[]) => void;
  openIssues: boolean;
  previousLine: SourceLine | undefined;
  renderDuplicationPopup: (index: number, line: number) => React.ReactNode;
  scroll?: (element: HTMLElement) => void;
  scrollToUncoveredLine?: boolean;
  secondaryIssueLocations: LinearIssueLocation[];
  selectedIssue: string | undefined;
  verticalBuffer?: number;
}

const LINE_HEIGHT = 18;

export default class Line extends React.PureComponent<Props> {
  handleIssuesIndicatorClick = () => {
    if (this.props.openIssues) {
      this.props.onIssuesClose(this.props.line);
      this.props.onIssueUnselect();
    } else {
      this.props.onIssuesOpen(this.props.line);

      const { issues } = this.props;
      if (issues.length > 0) {
        this.props.onIssueSelect(issues[0].key);
      }
    }
  };

  render() {
    const {
      additionalChild,
      branchLike,
      displayAllIssues,
      displayCoverage,
      displayDuplications,
      displayIssueLocationsCount,
      displayIssueLocationsLink,
      displayLineNumberOptions,
      displayLocationMarkers,
      highlightedLocationMessage,
      displayIssues,
      displaySCM = true,
      duplications,
      duplicationsCount,
      firstLineNumber,
      highlighted,
      highlightedSymbols,
      issueLocations,
      issuePopup,
      issues,
      last,
      line,
      openIssues,
      previousLine,
      scrollToUncoveredLine,
      secondaryIssueLocations,
      selectedIssue,
      verticalBuffer
    } = this.props;

    const className = classNames('source-line', {
      'source-line-highlighted': highlighted,
      'source-line-filtered': line.isNew,
      'source-line-filtered-dark':
        displayCoverage &&
        (line.coverageStatus === 'uncovered' || line.coverageStatus === 'partially-covered'),
      'source-line-last': last === true
    });

    const bottomPadding = verticalBuffer ? verticalBuffer * LINE_HEIGHT : undefined;

    // default is true
    const displayOptions = displayLineNumberOptions !== false;

    return (
      <tr className={className} data-line-number={line.line}>
        <LineNumber displayOptions={displayOptions} firstLineNumber={firstLineNumber} line={line} />

        {displaySCM && <LineSCM line={line} previousLine={previousLine} />}
        {displayIssues && !displayAllIssues ? (
          <LineIssuesIndicator
            issues={issues}
            issuesOpen={openIssues}
            line={line}
            onClick={this.handleIssuesIndicatorClick}
          />
        ) : (
          <td className="source-meta source-line-issues" />
        )}

        {displayDuplications && (
          <LineDuplicationBlock
            blocksLoaded={duplicationsCount > 0}
            duplicated={Boolean(line.duplicated)}
            index={0}
            key={0}
            line={this.props.line}
            onClick={this.props.loadDuplications}
            renderDuplicationPopup={this.props.renderDuplicationPopup}
          />
        )}

        {duplicationsCount > 1 &&
          times(duplicationsCount - 1, index => (
            <LineDuplicationBlock
              blocksLoaded={true}
              duplicated={duplications.includes(index + 1)}
              index={index + 1}
              key={index + 1}
              line={this.props.line}
              renderDuplicationPopup={this.props.renderDuplicationPopup}
            />
          ))}

        {displayCoverage && (
          <LineCoverage
            line={line}
            scroll={this.props.scroll}
            scrollToUncoveredLine={scrollToUncoveredLine}
          />
        )}

        <LineCode
          additionalChild={additionalChild}
          branchLike={branchLike}
          displayIssueLocationsCount={displayIssueLocationsCount}
          displayIssueLocationsLink={displayIssueLocationsLink}
          displayLocationMarkers={displayLocationMarkers}
          highlightedLocationMessage={highlightedLocationMessage}
          highlightedSymbols={highlightedSymbols}
          issueLocations={issueLocations}
          issuePopup={issuePopup}
          issues={issues}
          line={line}
          onIssueChange={this.props.onIssueChange}
          onIssuePopupToggle={this.props.onIssuePopupToggle}
          onIssueSelect={this.props.onIssueSelect}
          onLocationSelect={this.props.onLocationSelect}
          onSymbolClick={this.props.onSymbolClick}
          padding={bottomPadding}
          scroll={this.props.scroll}
          secondaryIssueLocations={secondaryIssueLocations}
          selectedIssue={selectedIssue}
          showIssues={openIssues || displayAllIssues}
        />
      </tr>
    );
  }
}
