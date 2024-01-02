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
import { BranchLike } from '../../../types/branch-like';
import { LinearIssueLocation, SourceLine, Issue as TypeIssue } from '../../../types/types';
import Issue from '../../issue/Issue';

export interface LineIssuesListProps {
  branchLike: BranchLike | undefined;
  displayAllIssues?: boolean;
  displayWhyIsThisAnIssue: boolean;
  issuesForLine: TypeIssue[];
  issuePopup: { issue: string; name: string } | undefined;
  issueLocationsByLine: { [line: number]: LinearIssueLocation[] };
  line: SourceLine;
  onIssueChange: (issue: TypeIssue) => void;
  onIssueClick: (issueKey: string) => void;
  onIssuePopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  openIssuesByLine: { [line: number]: boolean };
  selectedIssue: string | undefined;
}

export default function LineIssuesList(props: LineIssuesListProps) {
  const {
    line,
    displayWhyIsThisAnIssue,
    displayAllIssues,
    openIssuesByLine,
    selectedIssue,
    issuesForLine,
    issueLocationsByLine,
    issuePopup,
  } = props;

  const showIssues = openIssuesByLine[line.line] || displayAllIssues;
  const issueLocations = issueLocationsByLine[line.line] || [];
  let displayedIssue: TypeIssue[] = [];
  if (showIssues && issuesForLine.length > 0) {
    displayedIssue = issuesForLine;
  } else if (selectedIssue && !showIssues && issueLocations.length) {
    displayedIssue = issuesForLine.filter((i) => i.key === selectedIssue);
  }

  if (displayedIssue.length === 0) {
    return null;
  }
  return (
    <ul className="sw-my-4 sw-max-w-[980px]">
      {displayedIssue.map((issue) => (
        <Issue
          branchLike={props.branchLike}
          displayWhyIsThisAnIssue={displayWhyIsThisAnIssue}
          issue={issue}
          key={issue.key}
          onChange={props.onIssueChange}
          onSelect={props.onIssueClick}
          onPopupToggle={props.onIssuePopupToggle}
          openPopup={issuePopup && issuePopup.issue === issue.key ? issuePopup.name : undefined}
          selected={props.selectedIssue === issue.key}
        />
      ))}
    </ul>
  );
}
