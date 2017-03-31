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
import LineIssuesList from './LineIssuesList';
import {
  splitByTokens,
  highlightSymbol,
  highlightIssueLocations,
  generateHTML
} from '../helpers/highlight';
import type { Tokens } from '../helpers/highlight';
import type { SourceLine } from '../types';
import type {
  LinearIssueLocation,
  IndexedIssueLocation,
  IndexedIssueLocationMessage
} from '../helpers/indexing';

type Props = {
  highlightedSymbol: string | null,
  issueKeys: Array<string>,
  issueLocations: Array<LinearIssueLocation>,
  line: SourceLine,
  onIssueSelect: (issueKey: string) => void,
  onLocationSelect: (flowIndex: number, locationIndex: number) => void,
  onSymbolClick: (symbol: string) => void,
  // $FlowFixMe
  secondaryIssueLocations: Array<IndexedIssueLocation>,
  secondaryIssueLocationMessages: Array<IndexedIssueLocationMessage>,
  selectedIssue: string | null,
  selectedIssueLocation: IndexedIssueLocation | null,
  showIssues: boolean
};

type State = {
  tokens: Tokens
};

export default class LineCode extends React.PureComponent {
  codeNode: HTMLElement;
  props: Props;
  state: State;
  symbols: NodeList<HTMLElement>;

  constructor(props: Props) {
    super(props);
    this.state = {
      tokens: splitByTokens(props.line.code || '')
    };
  }

  componentDidMount() {
    this.attachEvents();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.line.code !== this.props.line.code) {
      this.setState({
        tokens: splitByTokens(nextProps.line.code || '')
      });
    }
  }

  componentWillUpdate() {
    this.detachEvents();
  }

  componentDidUpdate() {
    this.attachEvents();
  }

  componentWillUnmount() {
    this.detachEvents();
  }

  attachEvents() {
    this.symbols = this.codeNode.querySelectorAll('.sym');
    for (const symbol of this.symbols) {
      symbol.addEventListener('click', this.handleSymbolClick);
    }
  }

  detachEvents() {
    if (this.symbols) {
      for (const symbol of this.symbols) {
        symbol.removeEventListener('click', this.handleSymbolClick);
      }
    }
  }

  handleSymbolClick = (e: Object) => {
    e.preventDefault();
    const key = e.currentTarget.className.match(/sym-\d+/);
    if (key && key[0]) {
      this.props.onSymbolClick(key[0]);
    }
  };

  handleLocationMessageClick = (
    e: SyntheticInputEvent,
    flowIndex: number,
    locationIndex: number
  ) => {
    e.preventDefault();
    this.props.onLocationSelect(flowIndex, locationIndex);
  };

  isSecondaryIssueLocationSelected(location: IndexedIssueLocation | IndexedIssueLocationMessage) {
    const { selectedIssueLocation } = this.props;
    if (selectedIssueLocation == null) {
      return false;
    } else {
      return selectedIssueLocation.flowIndex === location.flowIndex &&
        selectedIssueLocation.locationIndex === location.locationIndex;
    }
  }

  renderSecondaryIssueLocationMessage = (location: IndexedIssueLocationMessage) => {
    const className = classNames('source-viewer-issue-location', 'issue-location-message', {
      selected: this.isSecondaryIssueLocationSelected(location)
    });

    const limitString = (str: string) => str.length > 30 ? str.substr(0, 30) + '...' : str;

    return (
      <a
        key={`${location.flowIndex}-${location.locationIndex}`}
        href="#"
        className={className}
        title={location.msg}
        onClick={e =>
          this.handleLocationMessageClick(e, location.flowIndex, location.locationIndex)}>
        {location.index && <strong>{location.index}: </strong>}
        {location.msg ? limitString(location.msg) : ''}
      </a>
    );
  };

  renderSecondaryIssueLocationMessages(locations: Array<IndexedIssueLocationMessage>) {
    return (
      <div className="source-line-issue-locations">
        {locations.map(this.renderSecondaryIssueLocationMessage)}
      </div>
    );
  }

  render() {
    const {
      highlightedSymbol,
      issueKeys,
      issueLocations,
      line,
      onIssueSelect,
      secondaryIssueLocationMessages,
      secondaryIssueLocations,
      selectedIssue,
      selectedIssueLocation,
      showIssues
    } = this.props;

    let tokens = [...this.state.tokens];

    if (highlightedSymbol) {
      tokens = highlightSymbol(tokens, highlightedSymbol);
    }

    if (issueLocations.length > 0) {
      tokens = highlightIssueLocations(tokens, issueLocations);
    }

    if (secondaryIssueLocations) {
      tokens = highlightIssueLocations(tokens, secondaryIssueLocations, 'issue-location');
      if (selectedIssueLocation != null) {
        const x = secondaryIssueLocations.find(location =>
          this.isSecondaryIssueLocationSelected(location));
        if (x) {
          tokens = highlightIssueLocations(tokens, [x], 'selected');
        }
      }
    }

    const finalCode = generateHTML(tokens);

    const className = classNames('source-line-code', 'code', {
      'has-issues': issueKeys.length > 0
    });

    return (
      <td className={className} data-line-number={line.line}>
        <div className="source-line-code-inner">
          <pre ref={node => this.codeNode = node} dangerouslySetInnerHTML={{ __html: finalCode }} />
          {secondaryIssueLocationMessages != null &&
            secondaryIssueLocationMessages.length > 0 &&
            this.renderSecondaryIssueLocationMessages(secondaryIssueLocationMessages)}
        </div>
        {showIssues &&
          issueKeys.length > 0 &&
          <LineIssuesList
            issueKeys={issueKeys}
            onIssueClick={onIssueSelect}
            selectedIssue={selectedIssue}
          />}
      </td>
    );
  }
}
