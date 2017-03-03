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
import ConnectedIssue from '../issue/ConnectedIssue';
import SourceViewerIssuesIndicator from './SourceViewerIssuesIndicator';
import { translate } from '../../helpers/l10n';
import { splitByTokens, highlightSymbol, highlightIssueLocations, generateHTML } from './helpers/highlight';
import type { SourceLine } from './types';

type Props = {
  displayAllIssues: boolean,
  displayCoverage: boolean,
  displayDuplications: boolean,
  displayFiltered: boolean,
  displayIssues: boolean,
  displaySCM: boolean,
  duplications: Array<number>,
  duplicationsCount: number,
  filtered: boolean | null,
  highlighted: boolean,
  highlightedSymbol: string | null,
  issueLocations: Array<{ from: number, to: number }>,
  issues: Array<string>,
  line: SourceLine,
  loadDuplications: (SourceLine, HTMLElement) => void,
  onClick: (number, HTMLElement) => void,
  onCoverageClick: (SourceLine, HTMLElement) => void,
  onDuplicationClick: (number, number) => void,
  onIssueSelect: (string) => void,
  onIssueUnselect: () => void,
  onSCMClick: (SourceLine, HTMLElement) => void,
  onSymbolClick: (string) => void,
  selectedIssue: string | null,
  // $FlowFixMe
  secondaryIssueLocations: Array<{ from: number, to: number }>,
  // $FlowFixMe
  secondaryIssueLocationMessages: Array<{ msg: string, index?: number }>
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

  componentDidUpdate (prevProps: Props) {
    /* eslint-disable no-console */
    console.log('re-render line', this.props.line.line, 'because they are not equal:');
    Object.keys(this.props).forEach(prop => {
      if (this.props[prop] !== prevProps[prop]) {
        console.log(prop);
      }
    });
    console.log('');

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

  handleClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    this.props.onClick(this.props.line.line, e.target);
  };

  handleCoverageClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    this.props.onCoverageClick(this.props.line, e.target);
  };

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

  handleSCMClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    this.props.onSCMClick(this.props.line, e.target);
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

  renderLineNumber () {
    const { line } = this.props;
    return (
      <td className="source-meta source-line-number"
          // don't display 0
          data-line-number={line.line ? line.line : undefined}
          role={line.line ? 'button' : undefined}
          tabIndex={line.line ? 0 : undefined}
          onClick={line.line ? this.handleClick : undefined}/>
    );
  }

  renderSCM () {
    const { line } = this.props;
    const clickable = !!line.line;
    return (
      <td className="source-meta source-line-scm"
          data-line-number={line.line}
          role={clickable ? 'button' : undefined}
          tabIndex={clickable ? 0 : undefined}
          onClick={clickable ? this.handleSCMClick : undefined}>
        {this.props.displaySCM && (
          <div className="source-line-scm-inner" data-author={line.scmAuthor}/>
        )}
      </td>
    );
  }

  renderCoverage () {
    const { line } = this.props;
    const className = 'source-meta source-line-coverage' +
      (line.coverageStatus != null ? ` source-line-${line.coverageStatus}` : '');
    return (
      <td className={className}
          data-line-number={line.line}
          title={line.coverageStatus != null && translate('source_viewer.tooltip', line.coverageStatus)}
          data-placement={line.coverageStatus != null && 'right'}
          data-toggle={line.coverageStatus != null && 'tooltip'}
          role={line.coverageStatus != null ? 'button' : undefined}
          tabIndex={line.coverageStatus != null ? 0 : undefined}
          onClick={line.coverageStatus != null && this.handleCoverageClick}>
        <div className="source-line-bar"/>
      </td>
    );
  }

  renderDuplications () {
    const { line } = this.props;
    const className = classNames('source-meta', 'source-line-duplications', {
      'source-line-duplicated': line.duplicated
    });

    const handleDuplicationClick = (e: SyntheticInputEvent) => {
      e.preventDefault();
      this.props.loadDuplications(this.props.line, e.target);
    };

    return (
      <td className={className}
          title={line.duplicated && translate('source_viewer.tooltip.duplicated_line')}
          data-placement={line.duplicated && 'right'}
          data-toggle={line.duplicated && 'tooltip'}
          role="button"
          tabIndex="0"
          onClick={handleDuplicationClick}>
        <div className="source-line-bar"/>
      </td>
    );
  }

  renderDuplicationsExtra () {
    const { duplications, duplicationsCount } = this.props;
    return times(duplicationsCount).map(index => this.renderDuplication(index, duplications.includes(index)));
  }

  renderDuplication = (index: number, duplicated: boolean) => {
    const className = classNames('source-meta', 'source-line-duplications-extra', {
      'source-line-duplicated': duplicated
    });

    const handleDuplicationClick = (e: SyntheticInputEvent) => {
      e.preventDefault();
      this.props.onDuplicationClick(index, this.props.line.line);
    };

    return (
      <td key={index}
          className={className}
          data-line-number={this.props.line.line}
          data-index={index}
          title={duplicated ? translate('source_viewer.tooltip.duplicated_block') : undefined}
          data-placement={duplicated ? 'right' : undefined}
          data-toggle={duplicated ? 'tooltip' : undefined}
          role={duplicated ? 'button' : undefined}
          tabIndex={duplicated ? '0' : undefined}
          onClick={duplicated ? handleDuplicationClick : undefined}>
        <div className="source-line-bar"/>
      </td>
    );
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

  renderSecondaryIssueLocationMessages (locationMessages: Array<{ msg: string, index?: number }>) {
    const limitString = (str: string) => (
      str.length > 30 ? str.substr(0, 30) + '...' : str
    );

    return (
      <div className="source-line-issue-locations">
        {locationMessages.map((locationMessage, index) => (
          <div key={index} className="source-viewer-issue-location" title={locationMessage.msg}>
            {locationMessage.index && (
              <strong>{locationMessage.index}: </strong>
            )}
            {limitString(locationMessage.msg)}
          </div>
        ))}
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
      tokens = highlightIssueLocations(tokens, secondaryIssueLocations, 'source-line-code-secondary-issue');
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
    const { line, duplicationsCount, filtered } = this.props;
    const className = classNames('source-line', {
      'source-line-highlighted': this.props.highlighted,
      'source-line-shadowed': filtered === false,
      'source-line-filtered': filtered === true
    });

    return (
      <tr className={className} data-line-number={line.line}>
        {this.renderLineNumber()}

        {this.renderSCM()}

        {this.props.displayCoverage && this.renderCoverage()}

        {this.props.displayDuplications && this.renderDuplications()}

        {duplicationsCount > 0 && this.renderDuplicationsExtra()}

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
