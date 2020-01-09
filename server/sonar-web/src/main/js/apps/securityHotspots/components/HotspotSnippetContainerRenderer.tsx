/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { SourceViewerContext } from '../../../components/SourceViewer/SourceViewerContext';
import SourceViewerHeaderSlim from '../../../components/SourceViewer/SourceViewerHeaderSlim';
import { BranchLike } from '../../../types/branch-like';
import { Hotspot } from '../../../types/security-hotspots';
import SnippetViewer from '../../issues/crossComponentSourceViewer/SnippetViewer';

export interface HotspotSnippetContainerRendererProps {
  branchLike?: BranchLike;
  highlightedSymbols: string[];
  hotspot: Hotspot;
  lastLine?: number;
  loading: boolean;
  locations: { [line: number]: T.LinearIssueLocation[] };
  linePopup?: T.LinePopup & { component: string };
  onExpandBlock: (direction: T.ExpandDirection) => Promise<void>;
  onLinePopupToggle: (line: T.SourceLine) => void;
  onSymbolClick: (symbols: string[]) => void;
  sourceLines: T.SourceLine[];
  sourceViewerFile: T.SourceViewerFile;
}

const noop = () => undefined;

export default function HotspotSnippetContainerRenderer(
  props: HotspotSnippetContainerRendererProps
) {
  const {
    branchLike,
    highlightedSymbols,
    hotspot,
    linePopup,
    loading,
    locations,
    sourceLines,
    sourceViewerFile
  } = props;

  return (
    <div className="bordered big-spacer-bottom">
      <SourceViewerHeaderSlim
        branchLike={branchLike}
        expandable={false}
        linkToProject={false}
        loading={loading}
        onExpand={noop}
        sourceViewerFile={sourceViewerFile}
      />
      <DeferredSpinner loading={loading}>
        {sourceLines.length > 0 && (
          <SourceViewerContext.Provider /* Used by LineOptionsPopup */
            value={{ branchLike, file: sourceViewerFile }}>
            <SnippetViewer
              branchLike={branchLike}
              component={sourceViewerFile}
              displaySCM={false}
              expandBlock={(_i, direction) => props.onExpandBlock(direction)}
              handleCloseIssues={noop}
              handleLinePopupToggle={props.onLinePopupToggle}
              handleOpenIssues={noop}
              handleSymbolClick={props.onSymbolClick}
              highlightedLocationMessage={undefined}
              highlightedSymbols={highlightedSymbols}
              index={0}
              issue={hotspot}
              issuesByLine={{}}
              last={false}
              linePopup={linePopup}
              loadDuplications={noop}
              locations={[]}
              locationsByLine={locations}
              onIssueChange={noop}
              onIssuePopupToggle={noop}
              onLocationSelect={noop}
              openIssuesByLine={{}}
              renderDuplicationPopup={noop}
              snippet={sourceLines}
            />
          </SourceViewerContext.Provider>
        )}
      </DeferredSpinner>
    </div>
  );
}
