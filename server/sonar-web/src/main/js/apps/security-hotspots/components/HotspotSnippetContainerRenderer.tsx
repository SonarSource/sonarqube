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
import * as React from 'react';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { scrollToElement } from '../../../helpers/scrolling';
import { BranchLike } from '../../../types/branch-like';
import { Hotspot } from '../../../types/security-hotspots';
import {
  Component,
  ExpandDirection,
  FlowLocation,
  LinearIssueLocation,
  SourceLine,
  SourceViewerFile,
} from '../../../types/types';
import SnippetViewer from '../../issues/crossComponentSourceViewer/SnippetViewer';
import HotspotPrimaryLocationBox from './HotspotPrimaryLocationBox';
import HotspotSnippetHeader from './HotspotSnippetHeader';

export interface HotspotSnippetContainerRendererProps {
  branchLike?: BranchLike;
  highlightedSymbols: string[];
  hotspot: Hotspot;
  loading: boolean;
  locations: { [line: number]: LinearIssueLocation[] };
  selectedHotspotLocation?: number;
  onCommentButtonClick: () => void;
  onExpandBlock: (direction: ExpandDirection) => Promise<void>;
  onSymbolClick: (symbols: string[]) => void;
  onLocationSelect: (index: number) => void;
  sourceLines: SourceLine[];
  sourceViewerFile: SourceViewerFile;
  secondaryLocations: FlowLocation[];
  component: Component;
}

const noop = () => undefined;
const SCROLL_DELAY = 100;
const EXPAND_ANIMATION_SPEED = 200;

const TOP_OFFSET = 100; // 5 lines above
const BOTTOM_OFFSET = 28; // 1 line below + margin

/* Exported for testing */
export function getScrollHandler(scrollableRef: React.RefObject<HTMLDivElement>) {
  return (element: Element, offset?: number, smooth = true) => {
    /* We need this delay to let the parent resize itself before scrolling */
    setTimeout(() => {
      const parent = scrollableRef.current;
      if (parent) {
        scrollToElement(element, {
          parent,
          topOffset: offset ?? TOP_OFFSET,
          bottomOffset: offset ?? BOTTOM_OFFSET,
          smooth,
        });
      }
    }, SCROLL_DELAY);
  };
}

/* Exported for testing */
export async function animateExpansion(
  scrollableRef: React.RefObject<HTMLDivElement>,
  expandBlock: (direction: ExpandDirection) => Promise<void>,
  direction: ExpandDirection
) {
  const wrapper = scrollableRef.current?.querySelector<HTMLElement>('.snippet');
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
  props: HotspotSnippetContainerRendererProps
) {
  const {
    branchLike,
    highlightedSymbols,
    hotspot,
    loading,
    locations: primaryLocations,
    sourceLines,
    sourceViewerFile,
    secondaryLocations,
    component,
    selectedHotspotLocation,
  } = props;

  const scrollableRef = React.useRef<HTMLDivElement>(null);

  const secondaryLocationSelected = selectedHotspotLocation !== undefined;

  /* Use memo is important to not rerender and trigger additional scrolls */
  const hotspotPrimaryLocationBox = React.useMemo(
    () => (
      <HotspotPrimaryLocationBox
        hotspot={hotspot}
        onCommentClick={props.onCommentButtonClick}
        scroll={getScrollHandler(scrollableRef)}
        secondaryLocationSelected={secondaryLocationSelected}
      />
    ),
    [hotspot, secondaryLocationSelected, props.onCommentButtonClick]
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
        <p className="spacer-bottom">{translate('hotspots.no_associated_lines')}</p>
      )}
      <HotspotSnippetHeader hotspot={hotspot} component={component} branchLike={branchLike} />
      <div className="hotspot-snippet-container bordered" ref={scrollableRef}>
        <DeferredSpinner className="big-spacer" loading={loading}>
          {sourceLines.length > 0 && (
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
              issue={hotspot}
              lastSnippetOfLastGroup={false}
              locations={secondaryLocations}
              locationsByLine={primaryLocations}
              onLocationSelect={props.onLocationSelect}
              renderAdditionalChildInLine={renderHotspotBoxInLine}
              renderDuplicationPopup={noop}
              snippet={sourceLines}
            />
          )}
        </DeferredSpinner>
      </div>
    </>
  );
}
