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
import ActionsDropdown, {
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import { translate } from 'sonar-ui-common/helpers/l10n';
import BranchStatus from '../../../components/common/BranchStatus';
import BranchLikeIcon from '../../../components/icons/BranchLikeIcon';
import DateFromNow from '../../../components/intl/DateFromNow';
import {
  getBranchLikeDisplayName,
  isBranch,
  isMainBranch,
  isPullRequest
} from '../../../helpers/branch-like';
import { BranchLike } from '../../../types/branch-like';
import BranchPurgeSetting from './BranchPurgeSetting';

export interface BranchLikeRowProps {
  branchLike: BranchLike;
  component: T.Component;
  displayPurgeSetting?: boolean;
  onDelete: () => void;
  onRename: () => void;
}

export function BranchLikeRow(props: BranchLikeRowProps) {
  const { branchLike, component, displayPurgeSetting, onDelete, onRename } = props;
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
        <BranchStatus branchLike={branchLike} component={component.key} />
      </td>
      <td className="nowrap">
        {branchLike.analysisDate && <DateFromNow date={branchLike.analysisDate} />}
      </td>
      {displayPurgeSetting && isBranch(branchLike) && (
        <td className="nowrap js-test-purge-toggle-container">
          <BranchPurgeSetting branch={branchLike} component={component} />
        </td>
      )}
      <td className="nowrap">
        <ActionsDropdown>
          {isMainBranch(branchLike) ? (
            <ActionsDropdownItem className="js-rename" onClick={onRename}>
              {translate('project_branch_pull_request.branch.rename')}
            </ActionsDropdownItem>
          ) : (
            <ActionsDropdownItem className="js-delete" destructive={true} onClick={onDelete}>
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
