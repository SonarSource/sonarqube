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
import { withTheme } from '@emotion/react';
import styled from '@emotion/styled';
import { Spinner, themeColor } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Hotspot } from '../../../types/security-hotspots';
import {
  ExpandDirection,
  FlowLocation,
  LinearIssueLocation,
  SourceLine,
  SourceViewerFile,
} from '../../../types/types';
import SnippetViewer from '../../issues/crossComponentSourceViewer/SnippetViewer';
import HotspotPrimaryLocationBox from './HotspotPrimaryLocationBox';

export interface HotspotSnippetContainerRendererProps {
  highlightedSymbols: string[];
  hotspot: Hotspot;
  loading: boolean;
  locations: { [line: number]: LinearIssueLocation[] };
  selectedHotspotLocation?: number;
  onExpandBlock: (direction: ExpandDirection) => Promise<void>;
  onSymbolClick: (symbols: string[]) => void;
  onLocationSelect: (index: number) => void;
  sourceLines: SourceLine[];
  sourceViewerFile: SourceViewerFile;
  secondaryLocations: FlowLocation[];
}

const noop = () => undefined;
const EXPAND_ANIMATION_SPEED = 200;

/* Exported for testing */
export async function animateExpansion(
  scrollableRef: React.RefObject<HTMLDivElement>,
  expandBlock: (direction: ExpandDirection) => Promise<void>,
  direction: ExpandDirection,
) {
  const wrapper = scrollableRef.current?.querySelector<HTMLElement>('.it__source-viewer-code');
  const table = wrapper?.firstChild as HTMLElement;

  if (!wrapper || !table) {
    return;
  }

  // lock the wrapper's height before adding the additional rows
  const startHeight = table.getBoundingClientRect().height;
  wrapper.style.maxHeight = `${startHeight}px`;

  await expandBlock(direction);

  const targetHeight = table.getBoundingClientRect().height;

  if (direction === 'up') {
    /*
     * Add a negative margin to keep the original alignment
     * Remove the transition to do so instantaneously
     */
    table.style.transition = 'none';
    table.style.marginTop = `${startHeight - targetHeight}px`;

    setTimeout(() => {
      /*
       * Reset the transition to the default
       * transition the margin back to 0 at the same time as the maxheight
       */
      table.style.transition = '';
      table.style.marginTop = '0px';
      wrapper.style.maxHeight = `${targetHeight}px`;
    }, 0);
  } else {
    // False positive:
    // eslint-disable-next-line require-atomic-updates
    wrapper.style.maxHeight = `${targetHeight}px`;
  }

  // after the animation is done, clear the applied styles
  setTimeout(() => {
    table.style.marginTop = '';
    wrapper.style.maxHeight = '';
  }, EXPAND_ANIMATION_SPEED);
}

export default function HotspotSnippetContainerRenderer(
  props: Readonly<HotspotSnippetContainerRendererProps>,
) {
  const {
    highlightedSymbols,
    hotspot,
    loading,
    locations: primaryLocations,
    secondaryLocations,
    selectedHotspotLocation,
    sourceLines,
    sourceViewerFile,
  } = props;

  const scrollableRef = React.useRef<HTMLDivElement>(null);

  const secondaryLocationSelected = selectedHotspotLocation !== undefined;

  /* Use memo is important to not rerender and trigger additional scrolls */
  const hotspotPrimaryLocationBox = React.useMemo(
    () => (
      <HotspotPrimaryLocationBox
        hotspot={hotspot}
        secondaryLocationSelected={secondaryLocationSelected}
      />
    ),
    [hotspot, secondaryLocationSelected],
  );

  const renderHotspotBoxInLine = (line: SourceLine) =>
    line.line === hotspot.line ? hotspotPrimaryLocationBox : undefined;

  const highlightedLocation =
    selectedHotspotLocation !== undefined
      ? { index: selectedHotspotLocation, text: hotspot.message }
      : undefined;

  return (
    <>
      {!loading && sourceLines.length === 0 && (
        <p className="sw-my-4">{translate('hotspots.no_associated_lines')}</p>
      )}

      <SourceFileWrapper className="sw-box-border sw-w-full sw-rounded-1" ref={scrollableRef}>
        <Spinner className="sw-m-4" loading={loading} />

        {!loading && sourceLines.length > 0 && (
          <SnippetViewer
            component={sourceViewerFile}
            displayLineNumberOptions={false}
            displaySCM={false}
            expandBlock={(_i, direction) =>
              animateExpansion(scrollableRef, props.onExpandBlock, direction)
            }
            handleSymbolClick={props.onSymbolClick}
            highlightedLocationMessage={highlightedLocation}
            highlightedSymbols={highlightedSymbols}
            index={0}
            locations={secondaryLocations}
            locationsByLine={primaryLocations}
            onLocationSelect={props.onLocationSelect}
            renderAdditionalChildInLine={renderHotspotBoxInLine}
            renderDuplicationPopup={noop}
            snippet={sourceLines}
            hideLocationIndex={secondaryLocations.length !== 0}
          />
        )}
      </SourceFileWrapper>
    </>
  );
}

const SourceFileWrapper = withTheme(styled.div`
  background-color: ${themeColor('codeLine')};
`);
