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
import { useIntl } from 'react-intl';
import { HelperHintIcon, ItemDivider, ItemHeader } from '~design-system';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { getBranchLikeKey, isSameBranchLike } from '../../../../../helpers/branch-like';
import { translate } from '../../../../../helpers/l10n';
import { isDefined } from '../../../../../helpers/types';
import { BranchLike, BranchLikeTree } from '../../../../../types/branch-like';
import MenuItem from './MenuItem';

export interface MenuItemListProps {
  branchLikeTree: BranchLikeTree;
  hasResults: boolean;
  onSelect: (branchLike: BranchLike) => void;
  search: string;
  selectedBranchLike: BranchLike | undefined;
}

export function MenuItemList(props: MenuItemListProps) {
  const intl = useIntl();
  let selectedNode: HTMLLIElement | null = null;

  React.useEffect(() => {
    if (selectedNode) {
      selectedNode.scrollIntoView({ block: 'center' });
      selectedNode.focus();
    }
  });

  const { branchLikeTree, hasResults, onSelect, selectedBranchLike, search } = props;

  const renderItem = (branchLike: BranchLike, indent = false) => (
    <MenuItem
      branchLike={branchLike}
      key={getBranchLikeKey(branchLike)}
      onSelect={onSelect}
      selected={isSameBranchLike(branchLike, selectedBranchLike)}
      setSelectedNode={(node) => (selectedNode = node)}
      indent={indent}
    />
  );

  const branches = [branchLikeTree.mainBranchTree, ...branchLikeTree.branchTree].filter(isDefined);
  const total =
    branches.length +
    branches.reduce((t, branchTree) => t + (branchTree?.pullRequests.length ?? 0), 0) +
    branchLikeTree.parentlessPullRequests.length +
    branchLikeTree.orphanPullRequests.length;

  return (
    <ul
      aria-label={`- ${translate('branch_like_navigation.list')}`}
      className="item-list sw-overflow-y-auto sw-overflow-x-hidden"
    >
      <output>
        {!hasResults && (
          <div className="sw-px-3 sw-py-2">
            <span>{intl.formatMessage({ id: 'no_results_for_x' }, { '0': search })}</span>
          </div>
        )}
        {hasResults && (
          <span className="sw-sr-only">
            {intl.formatMessage({ id: 'results_shown_x' }, { count: total })}
          </span>
        )}
      </output>

      {/* BRANCHES & PR */}
      {branches.map((tree, treeIndex) => (
        <React.Fragment key={getBranchLikeKey(tree.branch)}>
          {renderItem(tree.branch)}
          {tree.pullRequests.length > 0 && (
            <ul
              aria-label={` - ${intl.formatMessage({ id: 'branch_like_navigation.pull_requests_targeting' }, { branch: tree.branch.name })}`}
            >
              <ItemDivider aria-hidden />
              <ItemHeader aria-hidden>
                {translate('branch_like_navigation.pull_requests')}
              </ItemHeader>
              <ItemDivider aria-hidden />
              {tree.pullRequests.map((pr) => renderItem(pr, true))}
              {tree.pullRequests.length > 0 && treeIndex !== branches.length - 1 && <ItemDivider />}
            </ul>
          )}
        </React.Fragment>
      ))}

      {/* PARENTLESS PR (for display during search) */}
      {branchLikeTree.parentlessPullRequests.length > 0 && (
        <ul aria-label={` - ${translate('branch_like_navigation.pull_requests')}`}>
          <ItemDivider aria-hidden />
          <ItemHeader aria-hidden>{translate('branch_like_navigation.pull_requests')}</ItemHeader>
          <ItemDivider aria-hidden />
          {branchLikeTree.parentlessPullRequests.map((pr) => renderItem(pr))}
        </ul>
      )}

      {/* ORPHAN PR */}
      {branchLikeTree.orphanPullRequests.length > 0 && (
        <ul aria-label={` - ${translate('branch_like_navigation.orphan_pull_requests')}`}>
          <ItemDivider aria-hidden />
          <ItemHeader>
            {translate('branch_like_navigation.orphan_pull_requests')}
            <HelpTooltip
              className="sw-ml-1"
              overlay={translate('branch_like_navigation.orphan_pull_requests.tooltip')}
            >
              <HelperHintIcon />
            </HelpTooltip>
          </ItemHeader>
          <ItemDivider aria-hidden />
          {branchLikeTree.orphanPullRequests.map((pr) => renderItem(pr))}
        </ul>
      )}
    </ul>
  );
}

export default React.memo(MenuItemList);
