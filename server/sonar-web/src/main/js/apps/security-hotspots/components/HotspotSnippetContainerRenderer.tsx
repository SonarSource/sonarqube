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
import * as React from 'react';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Hotspot } from '../../../types/security-hotspots';
import {
  Component,
  ExpandDirection,
  FlowLocation,
  LinearIssueLocation,
  SourceLine,
  SourceViewerFile
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
  onCommentButtonClick: () => void;
  onExpandBlock: (direction: ExpandDirection) => Promise<void>;
  onSymbolClick: (symbols: string[]) => void;
  sourceLines: SourceLine[];
  sourceViewerFile: SourceViewerFile;
  secondaryLocations: FlowLocation[];
  component: Component;
}

const noop = () => undefined;

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
    component
  } = props;

  const renderHotspotBoxInLine = (lineNumber: number) =>
    lineNumber === hotspot.line ? (
      <HotspotPrimaryLocationBox hotspot={hotspot} onCommentClick={props.onCommentButtonClick} />
    ) : (
      undefined
    );

  return (
    <>
      {!loading && sourceLines.length === 0 && (
        <p className="spacer-bottom">{translate('hotspots.no_associated_lines')}</p>
      )}
      <HotspotSnippetHeader hotspot={hotspot} component={component} branchLike={branchLike} />
      <div className="bordered">
        <DeferredSpinner className="big-spacer" loading={loading}>
          {sourceLines.length > 0 && (
            <SnippetViewer
              branchLike={undefined}
              component={sourceViewerFile}
              displayLineNumberOptions={false}
              displaySCM={false}
              expandBlock={(_i, direction) => props.onExpandBlock(direction)}
              handleCloseIssues={noop}
              handleOpenIssues={noop}
              handleSymbolClick={props.onSymbolClick}
              highlightedLocationMessage={undefined}
              highlightedSymbols={highlightedSymbols}
              index={0}
              issue={hotspot}
              issuesByLine={{}}
              lastSnippetOfLastGroup={false}
              locations={secondaryLocations}
              locationsByLine={primaryLocations}
              onIssueChange={noop}
              onIssuePopupToggle={noop}
              onLocationSelect={noop}
              openIssuesByLine={{}}
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
