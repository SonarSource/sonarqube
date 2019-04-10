/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Issue from '../../issue/Issue';

interface Props {
  branchLike: T.BranchLike | undefined;
  issuePopup: { issue: string; name: string } | undefined;
  issues: T.Issue[];
  onIssueChange: (issue: T.Issue) => void;
  onIssueClick: (issueKey: string) => void;
  onIssuePopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  selectedIssue: string | undefined;
}

export default function LineIssuesList(props: Props) {
  const { issuePopup } = props;

  return (
    <div className="issue-list">
      {props.issues.map(issue => (
        <Issue
          branchLike={props.branchLike}
          issue={issue}
          key={issue.key}
          onChange={props.onIssueChange}
          onClick={props.onIssueClick}
          onPopupToggle={props.onIssuePopupToggle}
          openPopup={issuePopup && issuePopup.issue === issue.key ? issuePopup.name : undefined}
          selected={props.selectedIssue === issue.key}
        />
      ))}
    </div>
  );
}
