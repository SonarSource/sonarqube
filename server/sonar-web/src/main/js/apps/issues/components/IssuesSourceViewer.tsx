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
import { BranchLike } from '../../../types/branch-like';
import { Issue } from '../../../types/types';
import CrossComponentSourceViewer from '../crossComponentSourceViewer/CrossComponentSourceViewer';
import { getLocations, getSelectedLocation } from '../utils';

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

export default function IssuesSourceViewer(props: IssuesSourceViewerProps) {
  const {
    openIssue,
    selectedFlowIndex,
    selectedLocationIndex,
    locationsNavigator,
    branchLike,
    issues
  } = props;

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
    <div>
      <CrossComponentSourceViewer
        branchLike={branchLike}
        highlightedLocationMessage={highlightedLocationMessage}
        issue={openIssue}
        issues={issues}
        locations={locations}
        onIssueSelect={props.onIssueSelect}
        onLocationSelect={props.onLocationSelect}
        selectedFlowIndex={selectedFlowIndex}
        selectedLocationIndex={selectedLocationIndex}
      />
    </div>
  );
}
