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
import LineIssuesList from './LineIssuesList';
import LocationIndex from '../../common/LocationIndex';
import LocationMessage from '../../common/LocationMessage';
import {
  highlightIssueLocations,
  highlightSymbol,
  splitByTokens,
  Token
} from '../helpers/highlight';

interface Props {
  branchLike: T.BranchLike | undefined;
  displayIssueLocationsCount?: boolean;
  displayIssueLocationsLink?: boolean;
  displayLocationMarkers?: boolean;
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  highlightedSymbols: string[] | undefined;
  issueLocations: T.LinearIssueLocation[];
  issuePopup: { issue: string; name: string } | undefined;
  issues: T.Issue[];
  line: T.SourceLine;
  onIssueChange: (issue: T.Issue) => void;
  onIssuePopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  onIssueSelect: (issueKey: string) => void;
  onLocationSelect: ((index: number) => void) | undefined;
  onSymbolClick: (symbols: Array<string>) => void;
  scroll?: (element: HTMLElement) => void;
  secondaryIssueLocations: Array<{
    from: number;
    to: number;
    line: number;
    index: number;
    startLine: number;
  }>;
  selectedIssue: string | undefined;
  showIssues?: boolean;
}

interface State {
  tokens: Token[];
}

export default class LineCode extends React.PureComponent<Props, State> {
  activeMarkerNode?: HTMLElement | null;
  codeNode?: HTMLElement | null;
  symbols?: NodeListOf<HTMLElement>;

  constructor(props: Props) {
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

  componentDidUpdate(prevProps: Props) {
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
    if (this.codeNode) {
      this.symbols = this.codeNode.querySelectorAll('.sym');
      if (this.symbols) {
        for (let i = 0; i < this.symbols.length; i++) {
          const symbol = this.symbols[i];
          symbol.addEventListener('click', this.handleSymbolClick);
        }
      }
    }
  }

  detachEvents() {
    if (this.symbols) {
      for (let i = 0; i < this.symbols.length; i++) {
        const symbol = this.symbols[i];
        symbol.addEventListener('click', this.handleSymbolClick);
      }
    }
  }

  handleSymbolClick = (event: MouseEvent) => {
    event.preventDefault();
    const keys = (event.currentTarget as HTMLElement).className.match(/sym-\d+/g);
    if (keys && keys.length > 0) {
      this.props.onSymbolClick(keys);
    }
  };

  renderMarker(index: number, message: string | undefined, selected: boolean, leading: boolean) {
    const { onLocationSelect } = this.props;
    const onClick = onLocationSelect ? () => onLocationSelect(index) : undefined;
    const ref = selected ? (node: HTMLElement | null) => (this.activeMarkerNode = node) : undefined;
    return (
      <LocationIndex
        key={`marker-${index}`}
        leading={leading}
        onClick={onClick}
        selected={selected}>
        <span ref={ref}>{index + 1}</span>
        {message && <LocationMessage selected={true}>{message}</LocationMessage>}
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

    const renderedTokens: React.ReactNode[] = [];

    // track if the first marker is displayed before the source code
    // set `false` for the first token in a row
    let leadingMarker = false;

    tokens.forEach((token, index) => {
      if (this.props.displayLocationMarkers && token.markers.length > 0) {
        token.markers.forEach(marker => {
          const selected =
            highlightedLocationMessage !== undefined && highlightedLocationMessage.index === marker;
          const message = selected ? highlightedLocationMessage!.text : undefined;
          renderedTokens.push(this.renderMarker(marker, message, selected, leadingMarker));
        });
      }
      renderedTokens.push(
        <span className={token.className} key={index}>
          {token.text}
        </span>
      );

      // keep leadingMarker truthy if previous token has only whitespaces
      leadingMarker = (index === 0 ? true : leadingMarker) && !token.text.trim().length;
    });

    return (
      <td className={className} data-line-number={line.line}>
        <div className="source-line-code-inner">
          <pre ref={node => (this.codeNode = node)}>{renderedTokens}</pre>
        </div>
        {showIssues && issues.length > 0 && (
          <LineIssuesList
            branchLike={this.props.branchLike}
            displayIssueLocationsCount={this.props.displayIssueLocationsCount}
            displayIssueLocationsLink={this.props.displayIssueLocationsLink}
            issuePopup={this.props.issuePopup}
            issues={issues}
            onIssueChange={this.props.onIssueChange}
            onIssueClick={onIssueSelect}
            onIssuePopupToggle={this.props.onIssuePopupToggle}
            selectedIssue={selectedIssue}
          />
        )}
      </td>
    );
  }
}
