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
import BranchStatus from '../../../components/common/BranchStatus';
import ActionsDropdown, { ActionsDropdownItem } from '../../../components/controls/ActionsDropdown';
import BranchLikeIcon from '../../../components/icons/BranchLikeIcon';
import DateFromNow from '../../../components/intl/DateFromNow';
import {
  getBranchLikeDisplayName,
  isBranch,
  isMainBranch,
  isPullRequest,
} from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Component } from '../../../types/types';
import BranchPurgeSetting from './BranchPurgeSetting';

export interface BranchLikeRowProps {
  branchLike: BranchLike;
  component: Component;
  displayPurgeSetting?: boolean;
  onDelete: () => void;
  onRename: () => void;
  onUpdatePurgeSetting: () => void;
}

export function BranchLikeRow(props: BranchLikeRowProps) {
  const { branchLike, component, displayPurgeSetting } = props;
  const branchLikeDisplayName = getBranchLikeDisplayName(branchLike);

  return (
    <tr>
      <td className="nowrap hide-overflow">
        <BranchLikeIcon branchLike={branchLike} className="little-spacer-right" />
        <span title={branchLikeDisplayName}>{branchLikeDisplayName}</span>
        <span>
          {isMainBranch(branchLike) && (
            <div className="badge spacer-left">{translate('branches.main_branch')}</div>
          )}
        </span>
      </td>
      <td className="nowrap">
        <BranchStatus branchLike={branchLike} component={component} />
      </td>
      <td className="nowrap">{<DateFromNow date={branchLike.analysisDate} />}</td>
      {displayPurgeSetting && isBranch(branchLike) && (
        <td className="nowrap js-test-purge-toggle-container">
          <BranchPurgeSetting
            branch={branchLike}
            component={component}
            onUpdatePurgeSetting={props.onUpdatePurgeSetting}
          />
        </td>
      )}
      <td className="nowrap">
        <ActionsDropdown>
          {isMainBranch(branchLike) ? (
            <ActionsDropdownItem className="js-rename" onClick={props.onRename}>
              {translate('project_branch_pull_request.branch.rename')}
            </ActionsDropdownItem>
          ) : (
            <ActionsDropdownItem className="js-delete" destructive={true} onClick={props.onDelete}>
              {translate(
                isPullRequest(branchLike)
                  ? 'project_branch_pull_request.pull_request.delete'
                  : 'project_branch_pull_request.branch.delete'
              )}
            </ActionsDropdownItem>
          )}
        </ActionsDropdown>
      </td>
    </tr>
  );
}

export default React.memo(BranchLikeRow);
