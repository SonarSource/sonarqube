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
import ActionsDropdown, {
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import { translate } from 'sonar-ui-common/helpers/l10n';
import BranchStatus from '../../../components/common/BranchStatus';
import BranchLikeIcon from '../../../components/icons/BranchLikeIcon';
import DateFromNow from '../../../components/intl/DateFromNow';
import { getBranchLikeDisplayName, isMainBranch, isPullRequest } from '../../../helpers/branches';

export interface BranchLikeRowRendererProps {
  branchLike: T.BranchLike;
  component: T.Component;
  onDelete: () => void;
  onRename: () => void;
}

export function BranchLikeRowRenderer(props: BranchLikeRowRendererProps) {
  const { branchLike, component, onDelete, onRename } = props;

  return (
    <tr>
      <td>
        <BranchLikeIcon branchLike={branchLike} className="little-spacer-right" />
        {getBranchLikeDisplayName(branchLike)}
        {isMainBranch(branchLike) && (
          <div className="badge spacer-left">{translate('branches.main_branch')}</div>
        )}
      </td>
      <td className="thin nowrap">
        <BranchStatus branchLike={branchLike} component={component.key} />
      </td>
      <td className="thin nowrap text-right big-spacer-left">
        {branchLike.analysisDate && <DateFromNow date={branchLike.analysisDate} />}
      </td>
      <td className="thin nowrap text-right">
        <ActionsDropdown className="big-spacer-left">
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

export default React.memo(BranchLikeRowRenderer);
