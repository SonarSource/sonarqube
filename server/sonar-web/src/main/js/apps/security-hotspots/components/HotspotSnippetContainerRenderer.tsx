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
import { SourceViewerContext } from '../../../components/SourceViewer/SourceViewerContext';
import SourceViewerHeaderSlim from '../../../components/SourceViewer/SourceViewerHeaderSlim';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Hotspot } from '../../../types/security-hotspots';
import {
  ExpandDirection,
  LinearIssueLocation,
  SourceLine,
  SourceViewerFile
} from '../../../types/types';
import SnippetViewer from '../../issues/crossComponentSourceViewer/SnippetViewer';

export interface HotspotSnippetContainerRendererProps {
  branchLike?: BranchLike;
  displayProjectName?: boolean;
  highlightedSymbols: string[];
  hotspot: Hotspot;
  loading: boolean;
  locations: { [line: number]: LinearIssueLocation[] };
  onExpandBlock: (direction: ExpandDirection) => Promise<void>;
  onSymbolClick: (symbols: string[]) => void;
  sourceLines: SourceLine[];
  sourceViewerFile: SourceViewerFile;
}

const noop = () => undefined;

export default function HotspotSnippetContainerRenderer(
  props: HotspotSnippetContainerRendererProps
) {
  const {
    branchLike,
    displayProjectName,
    highlightedSymbols,
    hotspot,
    loading,
    locations,
    sourceLines,
    sourceViewerFile
  } = props;

  return (
    <>
      {!loading && sourceLines.length === 0 && (
        <p className="spacer-bottom">{translate('hotspots.no_associated_lines')}</p>
      )}
      <div className="bordered big-spacer-bottom">
        <SourceViewerHeaderSlim
          branchLike={branchLike}
          expandable={false}
          displayProjectName={displayProjectName}
          linkToProject={false}
          loading={loading}
          onExpand={noop}
          sourceViewerFile={sourceViewerFile}
        />
        <DeferredSpinner className="big-spacer" loading={loading}>
          {sourceLines.length > 0 && (
            <SourceViewerContext.Provider /* Used by LineOptionsPopup */
              value={{ branchLike, file: sourceViewerFile }}>
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
    </>
  );
}
