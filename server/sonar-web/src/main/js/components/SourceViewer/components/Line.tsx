/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as classNames from 'classnames';
import { times } from 'lodash';
import LineNumber from './LineNumber';
import LineSCM from './LineSCM';
import LineCoverage from './LineCoverage';
import LineDuplications from './LineDuplications';
import LineDuplicationBlock from './LineDuplicationBlock';
import LineIssuesIndicator from './LineIssuesIndicator';
import LineCode from './LineCode';

interface Props {
  branchLike: T.BranchLike | undefined;
  displayAllIssues?: boolean;
  displayCoverage: boolean;
  displayDuplications: boolean;
  displayIssueLocationsCount?: boolean;
  displayIssueLocationsLink?: boolean;
  displayIssues: boolean;
  displayLocationMarkers?: boolean;
  duplications: number[];
  duplicationsCount: number;
  highlighted: boolean;
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  highlightedSymbols: string[] | undefined;
  issueLocations: T.LinearIssueLocation[];
  issuePopup: { issue: string; name: string } | undefined;
  issues: T.Issue[];
  last: boolean;
  line: T.SourceLine;
  linePopup: { index?: number; line: number; name: string } | undefined;
  loadDuplications: (line: T.SourceLine) => void;
  onLinePopupToggle: (x: { index?: number; line: number; name: string; open?: boolean }) => void;
  onIssueChange: (issue: T.Issue) => void;
  onIssuePopupToggle: (issueKey: string, popupName: string, open?: boolean) => void;
  onIssuesClose: (line: T.SourceLine) => void;
  onIssueSelect: (issueKey: string) => void;
  onIssuesOpen: (line: T.SourceLine) => void;
  onIssueUnselect: () => void;
  onLocationSelect: ((x: number) => void) | undefined;
  onSymbolClick: (symbols: string[]) => void;
  openIssues: boolean;
  previousLine: T.SourceLine | undefined;
  renderDuplicationPopup: (index: number, line: number) => JSX.Element;
  scroll?: (element: HTMLElement) => void;
  secondaryIssueLocations: Array<{
    from: number;
    to: number;
    line: number;
    index: number;
    startLine: number;
  }>;
  selectedIssue: string | undefined;
}

export default class Line extends React.PureComponent<Props> {
  isPopupOpen = (name: string, index?: number) => {
    const { line, linePopup } = this.props;
    return (
      linePopup !== undefined &&
      linePopup.index === index &&
      linePopup.line === line.line &&
      linePopup.name === name
    );
  };

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
    const { displayCoverage, duplications, duplicationsCount, issuePopup, line } = this.props;
    const className = classNames('source-line', {
      'source-line-highlighted': this.props.highlighted,
      'source-line-filtered': line.isNew,
      'source-line-filtered-dark':
        displayCoverage &&
        (line.coverageStatus === 'uncovered' || line.coverageStatus === 'partially-covered'),
      'source-line-last': this.props.last
    });

    return (
      <tr className={className} data-line-number={line.line}>
        <LineNumber
          line={line}
          onPopupToggle={this.props.onLinePopupToggle}
          popupOpen={this.isPopupOpen('line-number')}
        />

        <LineSCM
          line={line}
          onPopupToggle={this.props.onLinePopupToggle}
          popupOpen={this.isPopupOpen('scm')}
          previousLine={this.props.previousLine}
        />

        {this.props.displayCoverage && <LineCoverage line={line} />}

        {this.props.displayDuplications && (
          <LineDuplications line={line} onClick={this.props.loadDuplications} />
        )}

        {times(duplicationsCount, index => (
          <LineDuplicationBlock
            duplicated={duplications.includes(index)}
            index={index}
            key={index}
            line={this.props.line}
            onPopupToggle={this.props.onLinePopupToggle}
            popupOpen={this.isPopupOpen('duplications', index)}
            renderDuplicationPopup={this.props.renderDuplicationPopup}
          />
        ))}

        {this.props.displayIssues && !this.props.displayAllIssues && (
          <LineIssuesIndicator
            issues={this.props.issues}
            line={line}
            onClick={this.handleIssuesIndicatorClick}
          />
        )}

        <LineCode
          branchLike={this.props.branchLike}
          displayIssueLocationsCount={this.props.displayIssueLocationsCount}
          displayIssueLocationsLink={this.props.displayIssueLocationsLink}
          displayLocationMarkers={this.props.displayLocationMarkers}
          highlightedLocationMessage={this.props.highlightedLocationMessage}
          highlightedSymbols={this.props.highlightedSymbols}
          issueLocations={this.props.issueLocations}
          issuePopup={issuePopup}
          issues={this.props.issues}
          line={line}
          onIssueChange={this.props.onIssueChange}
          onIssuePopupToggle={this.props.onIssuePopupToggle}
          onIssueSelect={this.props.onIssueSelect}
          onLocationSelect={this.props.onLocationSelect}
          onSymbolClick={this.props.onSymbolClick}
          scroll={this.props.scroll}
          secondaryIssueLocations={this.props.secondaryIssueLocations}
          selectedIssue={this.props.selectedIssue}
          showIssues={this.props.openIssues || this.props.displayAllIssues}
        />
      </tr>
    );
  }
}
