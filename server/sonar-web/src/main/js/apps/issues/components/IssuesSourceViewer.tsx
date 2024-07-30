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
import { ToggleButton } from 'design-system';
import * as React from 'react';
import { isJupyterNotebookFile } from '~sonar-aligned/helpers/component';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Issue } from '../../../types/types';
import CrossComponentSourceViewer from '../crossComponentSourceViewer/CrossComponentSourceViewer';
import { JupyterNotebookIssueViewer } from '../jupyter-notebook/JupyterNotebookIssueViewer';
import { getLocations, getSelectedLocation } from '../utils';
import { IssueSourceViewerScrollContext } from './IssueSourceViewerScrollContext';

export interface IssuesSourceViewerProps {
  branchLike: BranchLike | undefined;
  issues: Issue[];
  locationsNavigator: boolean;
  onIssueSelect: (issueKey: string) => void;
  onLocationSelect: (index: number) => void;
  openIssue: Issue;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default function IssuesSourceViewer(props: Readonly<IssuesSourceViewerProps>) {
  const {
    openIssue,
    selectedFlowIndex,
    selectedLocationIndex,
    locationsNavigator,
    branchLike,
    issues,
    onIssueSelect,
    onLocationSelect,
  } = props;

  const [primaryLocationRef, setPrimaryLocationRef] = React.useState<HTMLElement | null>(null);
  const [selectedSecondaryLocationRef, setSelectedSecondaryLocationRef] =
    React.useState<HTMLElement | null>(null);

  const isJupyterNotebook = isJupyterNotebookFile(openIssue.component);
  const [tab, setTab] = React.useState(isJupyterNotebook ? 'preview' : 'code');

  React.useEffect(() => {
    if (
      selectedLocationIndex !== undefined &&
      selectedLocationIndex !== -1 &&
      selectedSecondaryLocationRef
    ) {
      selectedSecondaryLocationRef.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
        inline: 'nearest',
      });
    } else if (primaryLocationRef) {
      primaryLocationRef.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
        inline: 'nearest',
      });
    }
  }, [selectedSecondaryLocationRef, primaryLocationRef, selectedLocationIndex]);

  function registerPrimaryLocationRef(ref: HTMLElement) {
    setPrimaryLocationRef(ref);
  }

  function registerSelectedSecondaryLocationRef(ref: HTMLElement) {
    setSelectedSecondaryLocationRef(ref);
  }

  const locations = getLocations(openIssue, selectedFlowIndex).map((loc, index) => {
    loc.index = index;
    return loc;
  });

  const selectedLocation = getSelectedLocation(openIssue, selectedFlowIndex, selectedLocationIndex);

  const highlightedLocationMessage =
    locationsNavigator && selectedLocationIndex !== undefined
      ? selectedLocation && { index: selectedLocationIndex, text: selectedLocation.msg }
      : undefined;

  return (
    <>
      {isJupyterNotebook && (
        <div className="sw-mb-2">
          <ToggleButton
            options={[
              { label: translate('preview'), value: 'preview' },
              { label: translate('code'), value: 'code' },
            ]}
            value={tab}
            onChange={(value) => setTab(value)}
          />
        </div>
      )}
      {tab === 'code' ? (
        <IssueSourceViewerScrollContext.Provider
          value={{
            registerPrimaryLocationRef,
            registerSelectedSecondaryLocationRef,
          }}
        >
          <CrossComponentSourceViewer
            branchLike={branchLike}
            highlightedLocationMessage={highlightedLocationMessage}
            issue={openIssue}
            issues={isJupyterNotebook ? [openIssue] : issues}
            locations={locations}
            onIssueSelect={onIssueSelect}
            onLocationSelect={onLocationSelect}
            selectedFlowIndex={selectedFlowIndex}
          />
        </IssueSourceViewerScrollContext.Provider>
      ) : (
        <JupyterNotebookIssueViewer branchLike={branchLike} issue={openIssue} />
      )}
    </>
  );
}
