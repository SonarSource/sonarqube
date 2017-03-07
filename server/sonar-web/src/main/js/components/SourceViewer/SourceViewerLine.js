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
import LineNumber from './components/LineNumber';
import LineSCM from './components/LineSCM';
import LineCoverage from './components/LineCoverage';
import LineDuplications from './components/LineDuplications';
import LineDuplicationBlock from './components/LineDuplicationBlock';
import ConnectedIssue from '../issue/ConnectedIssue';
import SourceViewerIssuesIndicator from './SourceViewerIssuesIndicator';
import { splitByTokens, highlightSymbol, highlightIssueLocations, generateHTML } from './helpers/highlight';
import type { SourceLine } from './types';
import type { LinearIssueLocation, IndexedIssueLocation, IndexedIssueLocationMessage } from './helpers/indexing';

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
  onSCMClick: (SourceLine, HTMLElement) => void,
  onSelectLocation: (flowIndex: number, locationIndex: number) => void,
  onSymbolClick: (string) => void,
  previousLine?: SourceLine,
  selectedIssue: string | null,
  secondaryIssueLocations: Array<IndexedIssueLocation>,
  // $FlowFixMe
  secondaryIssueLocationMessages: Array<IndexedIssueLocationMessage>,
  selectedIssueLocation: IndexedIssueLocation | null
};

type State = {
  issuesOpen: boolean
};

export default class SourceViewerLine extends React.PureComponent {
  codeNode: HTMLElement;
  props: Props;
  issueElements: { [string]: HTMLElement } = {};
  issueViews: { [string]: { destroy: () => void } } = {};
  state: State = { issuesOpen: false };
  symbols: NodeList<HTMLElement>;

  componentDidMount () {
    this.attachEvents();
  }

  componentWillUpdate () {
    this.detachEvents();
  }

  componentDidUpdate () {
    this.attachEvents();
  }

  componentWillUnmount () {
    this.detachEvents();
  }

  attachEvents () {
    this.symbols = this.codeNode.querySelectorAll('.sym');
    for (const symbol of this.symbols) {
      symbol.addEventListener('click', this.handleSymbolClick);
    }
  }

  detachEvents () {
    if (this.symbols) {
      for (const symbol of this.symbols) {
        symbol.removeEventListener('click', this.handleSymbolClick);
      }
    }
  }

  handleIssuesIndicatorClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    this.setState(prevState => {
      // TODO not sure if side effects allowed here
      if (!prevState.issuesOpen) {
        const { issues } = this.props;
        if (issues.length > 0) {
          this.props.onIssueSelect(issues[0]);
        }
      } else {
        this.props.onIssueUnselect();
      }

      return { issuesOpen: !prevState.issuesOpen };
    });
  }

  handleSymbolClick = (e: Object) => {
    e.preventDefault();
    const key = e.currentTarget.className.match(/sym-\d+/);
    if (key && key[0]) {
      this.props.onSymbolClick(key[0]);
    }
  };

  handleIssueSelect = (issueKey: string) => {
    this.props.onIssueSelect(issueKey);
  };

  renderIssuesIndicator () {
    const { issues } = this.props;
    const hasIssues = issues.length > 0;
    const className = classNames('source-meta', 'source-line-issues', { 'source-line-with-issues': hasIssues });
    const onClick = hasIssues ? this.handleIssuesIndicatorClick : undefined;

    return (
      <td className={className}
          data-line-number={this.props.line.line}
          role="button"
          tabIndex="0"
          onClick={onClick}>
        {hasIssues && (
          <SourceViewerIssuesIndicator issues={issues}/>
        )}
        {issues.length > 1 && (
          <span className="source-line-issues-counter">{issues.length}</span>
        )}
      </td>
    );
  }

  isSecondaryIssueLocationSelected (location: IndexedIssueLocation | IndexedIssueLocationMessage) {
    const { selectedIssueLocation } = this.props;
    if (selectedIssueLocation == null) {
      return false;
    } else {
      return selectedIssueLocation.flowIndex === location.flowIndex &&
        selectedIssueLocation.locationIndex === location.locationIndex;
    }
  }

  handleLocationMessageClick (flowIndex: number, locationIndex: number, e: SyntheticInputEvent) {
    e.preventDefault();
    this.props.onSelectLocation(flowIndex, locationIndex);
  }

  renderSecondaryIssueLocationMessage = (location: IndexedIssueLocationMessage) => {
    const className = classNames('source-viewer-issue-location', 'issue-location-message', {
      'selected': this.isSecondaryIssueLocationSelected(location)
    });

    const limitString = (str: string) => (
      str.length > 30 ? str.substr(0, 30) + '...' : str
    );

    return (
      <a
        key={`${location.flowIndex}-${location.locationIndex}`}
        href="#"
        className={className}
        title={location.msg}
        onClick={e => this.handleLocationMessageClick(location.flowIndex, location.locationIndex, e)}>
        {location.index && (
          <strong>{location.index}: </strong>
        )}
        {limitString(location.msg)}
      </a>
    );
  };

  renderSecondaryIssueLocationMessages (locations: Array<IndexedIssueLocationMessage>) {
    return (
      <div className="source-line-issue-locations">
        {locations.map(this.renderSecondaryIssueLocationMessage)}
      </div>
    );
  }

  renderCode () {
    const { line, highlightedSymbol, issueLocations, issues, secondaryIssueLocations } = this.props;
    const { secondaryIssueLocationMessages } = this.props;
    const className = classNames('source-line-code', 'code', { 'has-issues': issues.length > 0 });

    const code = line.code || '';
    let tokens = splitByTokens(code);

    if (highlightedSymbol) {
      tokens = highlightSymbol(tokens, highlightedSymbol);
    }

    if (issueLocations.length > 0) {
      tokens = highlightIssueLocations(tokens, issueLocations);
    }

    if (secondaryIssueLocations) {
      const linearLocations = secondaryIssueLocations.map(location => ({
        from: location.from,
        line: location.line,
        to: location.to
      }));
      tokens = highlightIssueLocations(tokens, linearLocations, 'issue-location');
      const { selectedIssueLocation } = this.props;
      if (selectedIssueLocation != null) {
        const x = secondaryIssueLocations.find(location => this.isSecondaryIssueLocationSelected(location));
        if (x) {
          tokens = highlightIssueLocations(tokens, [x], 'selected');
        }
      }
    }

    const finalCode = generateHTML(tokens);

    const showIssues = (this.state.issuesOpen || this.props.displayAllIssues) && issues.length > 0;

    return (
      <td className={className} data-line-number={line.line}>
        <div className="source-line-code-inner">
          <pre ref={node => this.codeNode = node} dangerouslySetInnerHTML={{ __html: finalCode }}/>
          {secondaryIssueLocationMessages != null && secondaryIssueLocationMessages.length > 0 && (
            this.renderSecondaryIssueLocationMessages(secondaryIssueLocationMessages)
          )}
        </div>
        {showIssues && (
          <div className="issue-list">
            {issues.map(issue => (
              <ConnectedIssue
                key={issue}
                issueKey={issue}
                onClick={this.handleIssueSelect}
                selected={this.props.selectedIssue === issue}/>
            ))}
          </div>
        )}
      </td>
    );
  }

  render () {
    const { line, duplications, duplicationsCount, filtered } = this.props;
    const className = classNames('source-line', {
      'source-line-highlighted': this.props.highlighted,
      'source-line-shadowed': filtered === false,
      'source-line-filtered': filtered === true
    });

    return (
      <tr className={className} data-line-number={line.line}>
        <LineNumber line={line} onClick={this.props.onClick}/>

        <LineSCM
          line={line}
          onClick={this.props.onSCMClick}
          previousLine={this.props.previousLine}/>

        {this.props.displayCoverage &&
          <LineCoverage line={line} onClick={this.props.onCoverageClick}/>}

        {this.props.displayDuplications &&
          <LineDuplications line={line} onClick={this.props.loadDuplications}/>}

        {times(duplicationsCount).map(index => (
          <LineDuplicationBlock
            duplicated={duplications.includes(index)}
            index={index}
            key={index}
            line={this.props.line}
            onClick={this.props.onDuplicationClick}/>
        ))}

        {this.props.displayIssues && !this.props.displayAllIssues && this.renderIssuesIndicator()}

        {this.props.displayFiltered && (
          <td className="source-meta source-line-filtered-container" data-line-number={line.line}>
            <div className="source-line-bar"/>
          </td>
        )}

        {this.renderCode()}
      </tr>
    );
  }
}
