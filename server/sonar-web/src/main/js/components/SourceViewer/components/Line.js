/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import times from 'lodash/times';
import LineNumber from './LineNumber';
import LineSCM from './LineSCM';
import LineCoverage from './LineCoverage';
import LineDuplications from './LineDuplications';
import LineDuplicationBlock from './LineDuplicationBlock';
import LineIssuesIndicatorContainer from './LineIssuesIndicatorContainer';
import LineCode from './LineCode';
import { TooltipsContainer } from '../../mixins/tooltips-mixin';
import type { SourceLine } from '../types';
import type {
  LinearIssueLocation,
  IndexedIssueLocation,
  IndexedIssueLocationMessage
} from '../helpers/indexing';

type Props = {
  displayAllIssues: boolean,
  displayCoverage: boolean,
  displayDuplications: boolean,
  displayFiltered: boolean,
  displayIssues: boolean,
  duplications: Array<number>,
  duplicationsCount: number,
  filtered: boolean | null,
  highlighted: boolean,
  highlightedSymbol: string | null,
  issueLocations: Array<LinearIssueLocation>,
  issues: Array<string>,
  line: SourceLine,
  loadDuplications: (SourceLine, HTMLElement) => void,
  onClick: (SourceLine, HTMLElement) => void,
  onCoverageClick: (SourceLine, HTMLElement) => void,
  onDuplicationClick: (number, number) => void,
  onIssueSelect: (string) => void,
  onIssueUnselect: () => void,
  onIssuesOpen: (SourceLine) => void,
  onIssuesClose: (SourceLine) => void,
  onSCMClick: (SourceLine, HTMLElement) => void,
  onLocationSelect: (flowIndex: number, locationIndex: number) => void,
  onSymbolClick: (string) => void,
  openIssues: boolean,
  previousLine?: SourceLine,
  selectedIssue: string | null,
  secondaryIssueLocations: Array<IndexedIssueLocation>,
  // $FlowFixMe
  secondaryIssueLocationMessages: Array<IndexedIssueLocationMessage>,
  selectedIssueLocation: IndexedIssueLocation | null
};

export default class Line extends React.PureComponent {
  props: Props;

  handleIssuesIndicatorClick = () => {
    if (this.props.openIssues) {
      this.props.onIssuesClose(this.props.line);
      this.props.onIssueUnselect();
    } else {
      this.props.onIssuesOpen(this.props.line);

      const { issues } = this.props;
      if (issues.length > 0) {
        this.props.onIssueSelect(issues[0]);
      }
    }
  };

  render() {
    const { line, duplications, duplicationsCount, filtered } = this.props;
    const className = classNames('source-line', {
      'source-line-highlighted': this.props.highlighted,
      'source-line-shadowed': filtered === false,
      'source-line-filtered': filtered === true
    });

    return (
      <TooltipsContainer>
        <tr className={className} data-line-number={line.line}>
          <LineNumber line={line} onClick={this.props.onClick} />

          <LineSCM
            line={line}
            onClick={this.props.onSCMClick}
            previousLine={this.props.previousLine}
          />

          {this.props.displayCoverage &&
            <LineCoverage line={line} onClick={this.props.onCoverageClick} />}

          {this.props.displayDuplications &&
            <LineDuplications line={line} onClick={this.props.loadDuplications} />}

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
            !this.props.displayAllIssues &&
            <LineIssuesIndicatorContainer
              issueKeys={this.props.issues}
              line={line}
              onClick={this.handleIssuesIndicatorClick}
            />}

          {this.props.displayFiltered &&
            <td className="source-meta source-line-filtered-container" data-line-number={line.line}>
              <div className="source-line-bar" />
            </td>}

          <LineCode
            highlightedSymbol={this.props.highlightedSymbol}
            issueKeys={this.props.issues}
            issueLocations={this.props.issueLocations}
            line={line}
            onIssueSelect={this.props.onIssueSelect}
            onLocationSelect={this.props.onLocationSelect}
            onSymbolClick={this.props.onSymbolClick}
            secondaryIssueLocationMessages={this.props.secondaryIssueLocationMessages}
            secondaryIssueLocations={this.props.secondaryIssueLocations}
            selectedIssue={this.props.selectedIssue}
            selectedIssueLocation={this.props.selectedIssueLocation}
            showIssues={this.props.openIssues || this.props.displayAllIssues}
          />
        </tr>
      </TooltipsContainer>
    );
  }
}
