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
import {
  CoveredUnderline,
  CoveredUnderlineLabel,
  LineCodeLayer,
  LineCodeLayers,
  LineCodePreFormatted,
  LineMarker,
  LineToken,
  NewCodeUnderline,
  NewCodeUnderlineLabel,
  UncoveredUnderline,
  UncoveredUnderlineLabel,
  UnderlineLabels,
} from 'design-system';
import React, { PureComponent, ReactNode, RefObject, createRef } from 'react';
import { IssueSourceViewerScrollContext } from '../../../apps/issues/components/IssueSourceViewerScrollContext';
import { translate } from '../../../helpers/l10n';
import { LinearIssueLocation, SourceLine } from '../../../types/types';
import { Token, getHighlightedTokens } from '../helpers/highlight';

interface Props {
  displayCoverageUnderline?: boolean;
  displayLocationMarkers?: boolean;
  displayNewCodeUnderlineLabel?: boolean;
  hideLocationIndex?: boolean;
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  highlightedSymbols: string[] | undefined;
  issueLocations: LinearIssueLocation[];
  line: SourceLine;
  onLocationSelect: ((index: number) => void) | undefined;
  onSymbolClick: (symbols: string[]) => void;
  previousLine?: SourceLine;
  secondaryIssueLocations: LinearIssueLocation[];
}

export class LineCode extends PureComponent<React.PropsWithChildren<Props>> {
  symbols?: NodeListOf<HTMLElement>;
  findingNode?: RefObject<HTMLDivElement>;

  constructor(props: Props) {
    super(props);
    this.findingNode = createRef<HTMLDivElement>();
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

  addLineMarker = (marker: number, index: number, leadingMarker: boolean, markerIndex: number) => {
    const { highlightedLocationMessage, secondaryIssueLocations, hideLocationIndex } = this.props;
    const selected =
      highlightedLocationMessage !== undefined && highlightedLocationMessage.index === marker;
    const loc = secondaryIssueLocations.find((loc) => loc.index === marker);
    const message = loc?.text;
    const isLeading = leadingMarker && markerIndex === 0;
    return (
      <IssueSourceViewerScrollContext.Consumer key={`${marker}-${index}`}>
        {(ctx) => (
          <LineMarker
            hideLocationIndex={hideLocationIndex}
            index={marker}
            leading={isLeading}
            message={message}
            onLocationSelect={this.props.onLocationSelect}
            ref={selected ? ctx?.registerSelectedSecondaryLocationRef : undefined}
            selected={selected}
          />
        )}
      </IssueSourceViewerScrollContext.Consumer>
    );
  };

  addLineToken = (token: Token, shouldPlacePointer: boolean, index: number) => {
    return (
      <LineToken
        className={token.className}
        hasMarker={token.markers.length > 0}
        issueFindingRef={shouldPlacePointer ? this.findingNode : undefined}
        key={`${token.text}-${index}`}
        {...token.modifiers}
      >
        {token.text}
      </LineToken>
    );
  };

  renderTokens = (tokens: Token[]) => {
    const renderedTokens: ReactNode[] = [];

    // track if the first marker is displayed before the source code
    // set `false` for the first token in a row
    let leadingMarker = false;

    // track if a pointer is placed on the token
    let numberOfPlacedPointers = 0;

    tokens.forEach((token, index) => {
      if (this.props.displayLocationMarkers && token.markers.length > 0) {
        token.markers.forEach((marker, markerIndex) => {
          renderedTokens.push(this.addLineMarker(marker, index, leadingMarker, markerIndex));
        });
      }

      if (token.modifiers.isUnderlined && token.text.trim().length > 1) {
        numberOfPlacedPointers++;
      }
      renderedTokens.push(this.addLineToken(token, numberOfPlacedPointers === 1, index));

      if (numberOfPlacedPointers === 1) {
        numberOfPlacedPointers++;
      }

      // keep leadingMarker truthy if previous token has only whitespaces
      leadingMarker = (index === 0 ? true : leadingMarker) && !token.text.trim().length;
    });

    return renderedTokens;
  };

  render() {
    const {
      displayCoverageUnderline,
      displayNewCodeUnderlineLabel,
      children,
      highlightedLocationMessage,
      highlightedSymbols,
      issueLocations,
      line,
      previousLine,
      secondaryIssueLocations,
    } = this.props;

    const displayCoverageUnderlineLabel =
      displayCoverageUnderline && line.coverageBlock === line.line;
    const previousLineHasUnderline =
      previousLine?.isNew ||
      (previousLine?.coverageStatus && previousLine.coverageBlock === line.coverageBlock);

    return (
      <LineCodeLayers className="it__source-line-code" data-line-number={line.line}>
        {(displayCoverageUnderlineLabel || displayNewCodeUnderlineLabel) && (
          <UnderlineLabels aria-hidden transparentBackground={previousLineHasUnderline}>
            {displayCoverageUnderlineLabel && line.coverageStatus === 'covered' && (
              <CoveredUnderlineLabel>
                {translate('source_viewer.coverage.covered')}
              </CoveredUnderlineLabel>
            )}
            {displayCoverageUnderlineLabel &&
              (line.coverageStatus === 'uncovered' ||
                line.coverageStatus === 'partially-covered') && (
                <UncoveredUnderlineLabel>
                  {translate('source_viewer.coverage', line.coverageStatus)}
                </UncoveredUnderlineLabel>
              )}
            {displayNewCodeUnderlineLabel && (
              <NewCodeUnderlineLabel>{translate('source_viewer.new_code')}</NewCodeUnderlineLabel>
            )}
          </UnderlineLabels>
        )}
        {line.isNew && <NewCodeUnderline aria-hidden data-testid="new-code-underline" />}
        {displayCoverageUnderline && line.coverageStatus === 'covered' && (
          <CoveredUnderline aria-hidden data-testid="covered-underline" />
        )}
        {displayCoverageUnderline &&
          (line.coverageStatus === 'uncovered' || line.coverageStatus === 'partially-covered') && (
            <UncoveredUnderline aria-hidden data-testid="uncovered-underline" />
          )}

        <LineCodeLayer className="sw-px-3">
          <LineCodePreFormatted ref={this.nodeNodeRef}>
            {this.renderTokens(
              getHighlightedTokens({
                code: line.code,
                highlightedLocationMessage,
                highlightedSymbols,
                issueLocations,
                secondaryIssueLocations,
              }),
            )}
          </LineCodePreFormatted>
          <div ref={this.findingNode}>{children}</div>
        </LineCodeLayer>
      </LineCodeLayers>
    );
  }
}
