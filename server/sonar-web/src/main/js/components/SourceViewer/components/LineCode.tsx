/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { IssueSourceViewerScrollContext } from '../../../apps/issues/components/IssueSourceViewerScrollContext';
import { MessageFormatting } from '../../../types/issues';
import { LinearIssueLocation, SourceLine } from '../../../types/types';
import LocationIndex from '../../common/LocationIndex';
import Tooltip from '../../controls/Tooltip';
import { IssueMessageHighlighting } from '../../issue/IssueMessageHighlighting';
import {
  highlightIssueLocations,
  highlightSymbol,
  splitByTokens,
  Token,
} from '../helpers/highlight';

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
  secondaryIssueLocations: LinearIssueLocation[];
}

export default class LineCode extends React.PureComponent<React.PropsWithChildren<Props>> {
  symbols?: NodeListOf<HTMLElement>;

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

  renderToken(tokens: Token[]) {
    const { highlightedLocationMessage, secondaryIssueLocations } = this.props;
    const renderedTokens: React.ReactNode[] = [];

    // track if the first marker is displayed before the source code
    // set `false` for the first token in a row
    let leadingMarker = false;

    tokens.forEach((token, index) => {
      if (this.props.displayLocationMarkers && token.markers.length > 0) {
        token.markers.forEach((marker) => {
          const selected =
            highlightedLocationMessage !== undefined && highlightedLocationMessage.index === marker;
          const loc = secondaryIssueLocations.find((loc) => loc.index === marker);
          const message = loc?.text;
          const messageFormattings = loc?.textFormatting;
          renderedTokens.push(
            this.renderMarker(marker, message, messageFormattings, selected, leadingMarker)
          );
        });
      }
      renderedTokens.push(
        // eslint-disable-next-line react/no-array-index-key
        <span className={token.className} key={index}>
          {token.text}
        </span>
      );

      // keep leadingMarker truthy if previous token has only whitespaces
      leadingMarker = (index === 0 ? true : leadingMarker) && !token.text.trim().length;
    });
    return renderedTokens;
  }

  renderMarker(
    index: number,
    message: string | undefined,
    messageFormattings: MessageFormatting[] | undefined,
    selected: boolean,
    leading: boolean
  ) {
    const { onLocationSelect } = this.props;
    const onClick = onLocationSelect ? () => onLocationSelect(index) : undefined;

    return (
      <Tooltip
        key={`marker-${index}`}
        overlay={
          <IssueMessageHighlighting message={message} messageFormattings={messageFormattings} />
        }
        placement="top"
      >
        <LocationIndex
          leading={leading}
          onClick={onClick}
          selected={selected}
          aria-current={selected ? 'location' : false}
        >
          <IssueSourceViewerScrollContext.Consumer>
            {(ctx) => (
              <span ref={selected ? ctx?.registerSelectedSecondaryLocationRef : undefined}>
                {index + 1}
              </span>
            )}
          </IssueSourceViewerScrollContext.Consumer>
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
      secondaryIssueLocations,
    } = this.props;

    const container = document.createElement('div');
    container.innerHTML = this.props.line.code || '';

    let tokens = splitByTokens(container.childNodes);

    if (highlightedSymbols) {
      highlightedSymbols.forEach((symbol) => {
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
          (location) => location.index === highlightedLocationMessage.index
        );
        if (location) {
          tokens = highlightIssueLocations(tokens, [location], 'selected');
        }
      }
    }

    const renderedTokens = this.renderToken(tokens);

    const style = padding ? { paddingBottom: `${padding}px` } : undefined;

    return (
      <td
        className={classNames('source-line-code code', className)}
        data-line-number={line.line}
        style={style}
      >
        <div className="source-line-code-inner">
          <pre ref={this.nodeNodeRef}>{renderedTokens}</pre>
        </div>

        {children}
      </td>
    );
  }
}
