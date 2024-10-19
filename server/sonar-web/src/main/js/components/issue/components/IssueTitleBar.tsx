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
import { Issue } from '../../../types/types';
import { CleanCodeAttributePill } from '../../shared/CleanCodeAttributePill';
import IssueMessage from './IssueMessage';

export interface IssueTitleBarProps {
  branchLike?: BranchLike;
  displayWhyIsThisAnIssue?: boolean;
  issue: Issue;
}

export default function IssueTitleBar(props: Readonly<IssueTitleBarProps>) {
  const { issue, displayWhyIsThisAnIssue, branchLike } = props;

  return (
    <div className="sw-mt-1 sw-flex sw-items-start sw-justify-between sw-gap-8">
      <div className="sw-w-fit">
        <IssueMessage
          issue={issue}
          branchLike={branchLike}
          displayWhyIsThisAnIssue={displayWhyIsThisAnIssue}
        />
      </div>

      <CleanCodeAttributePill cleanCodeAttributeCategory={issue.cleanCodeAttributeCategory} />
    </div>
  );
}
