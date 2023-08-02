/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { IssueActions } from '../../../types/issues';
import { Issue } from '../../../types/types';
import { CleanCodeAttributePill } from '../../shared/CleanCodeAttributePill';
import IssueMessage from './IssueMessage';
import IssueTags from './IssueTags';

export interface IssueTitleBarProps {
  currentPopup?: string;
  branchLike?: BranchLike;
  displayWhyIsThisAnIssue?: boolean;
  issue: Issue;
  onChange: (issue: Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default function IssueTitleBar(props: IssueTitleBarProps) {
  const { issue, displayWhyIsThisAnIssue, currentPopup } = props;
  const canSetTags = issue.actions.includes(IssueActions.SetTags);

  return (
    <div className="sw-flex sw-items-end">
      <div className="sw-w-full sw-flex sw-flex-col">
        <CleanCodeAttributePill
          className="sw-mb-2"
          cleanCodeAttributeCategory={issue.cleanCodeAttributeCategory}
        />
        <div className="sw-w-fit">
          <IssueMessage
            issue={issue}
            branchLike={props.branchLike}
            displayWhyIsThisAnIssue={displayWhyIsThisAnIssue}
          />
        </div>
      </div>
      <div className="js-issue-tags sw-body-sm sw-grow-0 sw-whitespace-nowrap">
        <IssueTags
          canSetTags={canSetTags}
          issue={issue}
          onChange={props.onChange}
          togglePopup={props.togglePopup}
          open={currentPopup === 'edit-tags' && canSetTags}
        />
      </div>
    </div>
  );
}
