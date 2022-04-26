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
import * as React from 'react';
import { LinearIssueLocation, SourceLine } from '../../../types/types';
import LocationIndex from '../../common/LocationIndex';
import Tooltip from '../../controls/Tooltip';
import { highlightIssueLocations, highlightSymbol, splitByTokens } from '../helpers/highlight';

interface Props {
  className?: string;
  displayLocationMarkers?: boolean;
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  highlightedSymbols: string[] | undefined;
  issueLocations: LinearIssueLocation[];
  line: SourceLine;
  onLocationSelect: ((index: number) => void) | undefined;
  onSymbolClick: (symbols: Array<string>) => void;
  padding?: number;
  scroll?: (element: HTMLElement) => void;
  secondaryIssueLocations: LinearIssueLocation[];
}

export default class LineCode extends React.PureComponent<React.PropsWithChildren<Props>> {
  activeMarkerNode?: HTMLElement | null;
  symbols?: NodeListOf<HTMLElement>;

  componentDidMount() {
    if (this.props.highlightedLocationMessage && this.activeMarkerNode && this.props.scroll) {
      this.props.scroll(this.activeMarkerNode);
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (
      this.props.highlightedLocationMessage &&
      (!prevProps.highlightedLocationMessage ||
        prevProps.highlightedLocationMessage.index !==
          this.props.highlightedLocationMessage.index) &&
      this.activeMarkerNode &&
      this.props.scroll
    ) {
      this.props.scroll(this.activeMarkerNode);
    }
  }

  nodeNodeRef = (el: HTMLElement | null) => {
    if (el) {
      this.attachEvents(el);
    } else {
      this.detachEvents();
    }
  };

  attachEvents(codeNode: HTMLElement) {
    this.symbols = codeNode.querySelectorAll('.sym');
    if (this.symbols) {
      for (let i = 0; i < this.symbols.length; i++) {
        const symbol = this.symbols[i];
        symbol.addEventListener('click', this.handleSymbolClick);
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
      <Tooltip key={`marker-${index}`} overlay={message} placement="top">
        <LocationIndex
          leading={leading}
          onClick={onClick}
          selected={selected}
          aria-label={message ? `${index + 1}-${message}` : index + 1}>
          <span ref={ref}>{index + 1}</span>
        </LocationIndex>
      </Tooltip>
    );
  }

  render() {
    const {
      children,
      className,
      highlightedLocationMessage,
      highlightedSymbols,
      issueLocations,
      line,
      padding,
      secondaryIssueLocations
    } = this.props;

    let tokens = splitByTokens(this.props.line.code || '');

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

    const renderedTokens: React.ReactNode[] = [];

    // track if the first marker is displayed before the source code
    // set `false` for the first token in a row
    let leadingMarker = false;

    tokens.forEach((token, index) => {
      if (this.props.displayLocationMarkers && token.markers.length > 0) {
        token.markers.forEach(marker => {
          const selected =
            highlightedLocationMessage !== undefined && highlightedLocationMessage.index === marker;
          const loc = secondaryIssueLocations.find(loc => loc.index === marker);
          const message = loc && loc.text;
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

    const style = padding ? { paddingBottom: `${padding}px` } : undefined;

    return (
      <td
        className={classNames('source-line-code code', className)}
        data-line-number={line.line}
        style={style}>
        <div className="source-line-code-inner">
          <pre ref={this.nodeNodeRef}>{renderedTokens}</pre>
        </div>

        {children}
      </td>
    );
  }
}
