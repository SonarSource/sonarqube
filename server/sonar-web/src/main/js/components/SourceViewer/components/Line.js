/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import classNames from 'classnames';
import { times } from 'lodash';
import LineNumber from './LineNumber';
import LineSCM from './LineSCM';
import LineCoverage from './LineCoverage';
import LineDuplications from './LineDuplications';
import LineDuplicationBlock from './LineDuplicationBlock';
import LineIssuesIndicator from './LineIssuesIndicator';
import LineCode from './LineCode';
/*:: import type { SourceLine } from '../types'; */
/*:: import type { LinearIssueLocation } from '../helpers/indexing'; */
/*:: import type { Issue } from '../../issue/types'; */

/*::
type Props = {|
  branch?: string,
  displayAllIssues: boolean,
  displayCoverage: boolean,
  displayDuplications: boolean,
  displayIssues: boolean,
  displayIssueLocationsCount?: boolean;
  displayIssueLocationsLink?: boolean;
  displayLocationMarkers?: boolean;
  duplications: Array<number>,
  duplicationsCount: number,
  filtered: boolean | null,
  highlighted: boolean,
  highlightedLocationMessage?: { index: number, text: string },
  highlightedSymbols?: Array<string>,
  issueLocations: Array<LinearIssueLocation>,
  issues: Array<Issue>,
  last: boolean,
  line: SourceLine,
  loadDuplications: SourceLine => void,
  onClick: (SourceLine, HTMLElement) => void,
  onCoverageClick: (SourceLine, HTMLElement) => void,
  onDuplicationClick: (number, number) => void,
  onIssueChange: Issue => void,
  onIssueSelect: string => void,
  onIssueUnselect: () => void,
  onIssuesOpen: SourceLine => void,
  onIssuesClose: SourceLine => void,
  onLocationSelect?: number => void,
  onSCMClick: (SourceLine, HTMLElement) => void,
  onSymbolClick: (Array<string>) => void,
  openIssues: boolean,
  onPopupToggle: (issue: string, popupName: string, open: ?boolean ) => void,
  openPopup: ?{ issue: string, name: string},
  previousLine?: SourceLine,
  scroll?: HTMLElement => void,
  secondaryIssueLocations: Array<{
    from: number,
    to: number,
    line: number,
    index: number,
    startLine: number
  }>,
  selectedIssue: string | null
|};
*/

export default class Line extends React.PureComponent {
  /*:: props: Props; */

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
    const { line, duplications, displayCoverage, duplicationsCount, filtered } = this.props;
    const className = classNames('source-line', {
      'source-line-highlighted': this.props.highlighted,
      'source-line-filtered': filtered === true,
      'source-line-filtered-dark':
        displayCoverage &&
        (line.coverageStatus === 'uncovered' || line.coverageStatus === 'partially-covered'),
      'source-line-last': this.props.last
    });

    return (
      <tr className={className} data-line-number={line.line}>
        <LineNumber line={line} onClick={this.props.onClick} />

        <LineSCM
          line={line}
          onClick={this.props.onSCMClick}
          previousLine={this.props.previousLine}
        />

        {this.props.displayCoverage && (
          <LineCoverage line={line} onClick={this.props.onCoverageClick} />
        )}

        {this.props.displayDuplications && (
          <LineDuplications line={line} onClick={this.props.loadDuplications} />
        )}

        {times(duplicationsCount).map(index => (
          <LineDuplicationBlock
            duplicated={duplications.includes(index)}
            index={index}
            key={index}
            line={this.props.line}
            onClick={this.props.onDuplicationClick}
          />
        ))}

        {this.props.displayIssues &&
          !this.props.displayAllIssues && (
            <LineIssuesIndicator
              issues={this.props.issues}
              line={line}
              onClick={this.handleIssuesIndicatorClick}
            />
          )}

        <LineCode
          branch={this.props.branch}
          displayIssueLocationsCount={this.props.displayIssueLocationsCount}
          displayIssueLocationsLink={this.props.displayIssueLocationsLink}
          displayLocationMarkers={this.props.displayLocationMarkers}
          highlightedLocationMessage={this.props.highlightedLocationMessage}
          highlightedSymbols={this.props.highlightedSymbols}
          issues={this.props.issues}
          issueLocations={this.props.issueLocations}
          line={line}
          onIssueChange={this.props.onIssueChange}
          onIssueSelect={this.props.onIssueSelect}
          onLocationSelect={this.props.onLocationSelect}
          onSymbolClick={this.props.onSymbolClick}
          onPopupToggle={this.props.onPopupToggle}
          openPopup={this.props.openPopup}
          scroll={this.props.scroll}
          secondaryIssueLocations={this.props.secondaryIssueLocations}
          selectedIssue={this.props.selectedIssue}
          showIssues={this.props.openIssues || this.props.displayAllIssues}
        />
      </tr>
    );
  }
}
