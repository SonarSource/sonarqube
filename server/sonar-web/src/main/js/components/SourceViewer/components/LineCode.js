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
import LocationIndex from '../../common/LocationIndex';
import LocationMessage from '../../common/LocationMessage';
import { splitByTokens, highlightSymbol, highlightIssueLocations } from '../helpers/highlight';
/*:: import type { Tokens } from '../helpers/highlight'; */
/*:: import type { SourceLine } from '../types'; */
/*:: import type { LinearIssueLocation } from '../helpers/indexing'; */
/*:: import type { Issue } from '../../issue/types'; */

/*::
type Props = {|
  highlightedLocationMessage?: { index: number, text: string },
  highlightedSymbols?: Array<string>,
  issues: Array<Issue>,
  issueLocations: Array<LinearIssueLocation>,
  line: SourceLine,
  onIssueChange: Issue => void,
  onIssueSelect: (issueKey: string) => void,
  onLocationSelect?: number => void,
  onSymbolClick: (Array<string>) => void,
  scroll?: HTMLElement => void,
  secondaryIssueLocations: Array<{
    from: number,
    to: number,
    line: number,
    index: number,
    startLine: number
  }>,
  selectedIssue: string | null,
  showIssues: boolean
|};
*/

/*::
type State = {
  tokens: Tokens
};
*/

export default class LineCode extends React.PureComponent {
  /*:: activeMarkerNode: ?HTMLElement; */
  /*:: codeNode: HTMLElement; */
  /*:: props: Props; */
  /*:: state: State; */
  /*:: symbols: NodeList<HTMLElement>; */

  constructor(props /*: Props */) {
    super(props);
    this.state = {
      tokens: splitByTokens(props.line.code || '')
    };
  }

  componentDidMount() {
    this.attachEvents();
    if (this.props.highlightedLocationMessage && this.activeMarkerNode && this.props.scroll) {
      this.props.scroll(this.activeMarkerNode);
    }
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (nextProps.line.code !== this.props.line.code) {
      this.setState({
        tokens: splitByTokens(nextProps.line.code || '')
      });
    }
  }

  componentWillUpdate() {
    this.detachEvents();
  }

  componentDidUpdate(prevProps /*: Props */) {
    this.attachEvents();
    if (
      this.props.highlightedLocationMessage &&
      prevProps.highlightedLocationMessage !== this.props.highlightedLocationMessage &&
      this.activeMarkerNode &&
      this.props.scroll
    ) {
      this.props.scroll(this.activeMarkerNode);
    }
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

  handleSymbolClick = (e /*: Object */) => {
    e.preventDefault();
    const keys = e.currentTarget.className.match(/sym-\d+/g);
    if (keys.length > 0) {
      this.props.onSymbolClick(keys);
    }
  };

  renderMarker(index /*: number */, message /*: ?string */) {
    const { onLocationSelect } = this.props;
    const onClick = onLocationSelect ? () => onLocationSelect(index) : undefined;
    const ref = message != null ? node => (this.activeMarkerNode = node) : undefined;
    return (
      <LocationIndex key={`marker-${index}`} onClick={onClick} selected={message != null}>
        <span href="#" ref={ref}>
          {index + 1}
        </span>
        {message != null &&
          <LocationMessage selected={true}>
            {message}
          </LocationMessage>}
      </LocationIndex>
    );
  }

  render() {
    const {
      highlightedLocationMessage,
      highlightedSymbols,
      issues,
      issueLocations,
      line,
      onIssueSelect,
      secondaryIssueLocations,
      selectedIssue,
      showIssues
    } = this.props;

    let tokens = [...this.state.tokens];

    if (highlightedSymbols) {
      highlightedSymbols.forEach(symbol => {
        tokens = highlightSymbol(tokens, symbol);
      });
    }

    if (issueLocations.length > 0) {
      tokens = highlightIssueLocations(tokens, issueLocations);
    }

    if (secondaryIssueLocations) {
      tokens = highlightIssueLocations(tokens, secondaryIssueLocations, 'issue-location');

      if (highlightedLocationMessage) {
        const location = secondaryIssueLocations.find(
          location => location.index === highlightedLocationMessage.index
        );
        if (location) {
          tokens = highlightIssueLocations(tokens, [location], 'selected');
        }
      }
    }

    const className = classNames('source-line-code', 'code', {
      'has-issues': issues.length > 0
    });

    const renderedTokens = [];
    tokens.forEach((token, index) => {
      if (token.markers.length > 0) {
        token.markers.forEach(marker => {
          const message =
            highlightedLocationMessage != null && highlightedLocationMessage.index === marker
              ? highlightedLocationMessage.text
              : null;
          renderedTokens.push(this.renderMarker(marker, message));
        });
      }
      renderedTokens.push(
        <span className={token.className} key={index}>
          {token.text}
        </span>
      );
    });

    return (
      <td className={className} data-line-number={line.line}>
        <div className="source-line-code-inner">
          <pre ref={node => (this.codeNode = node)}>
            {renderedTokens}
          </pre>
        </div>
        {showIssues &&
          issues.length > 0 &&
          <LineIssuesList
            issues={issues}
            onIssueChange={this.props.onIssueChange}
            onIssueClick={onIssueSelect}
            selectedIssue={selectedIssue}
          />}
      </td>
    );
  }
}
