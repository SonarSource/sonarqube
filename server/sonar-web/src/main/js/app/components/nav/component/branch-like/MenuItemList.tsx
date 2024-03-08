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
import { HelperHintIcon, ItemDivider, ItemHeader } from 'design-system';
import * as React from 'react';
import HelpTooltip from '../../../../../components/controls/HelpTooltip';
import { getBranchLikeKey, isSameBranchLike } from '../../../../../helpers/branch-like';
import { translate } from '../../../../../helpers/l10n';
import { isDefined } from '../../../../../helpers/types';
import { BranchLike, BranchLikeTree } from '../../../../../types/branch-like';
import MenuItem from './MenuItem';

export interface MenuItemListProps {
  branchLikeTree: BranchLikeTree;
  hasResults: boolean;
  onSelect: (branchLike: BranchLike) => void;
  selectedBranchLike: BranchLike | undefined;
}

export function MenuItemList(props: MenuItemListProps) {
  let selectedNode: HTMLLIElement | null = null;

  React.useEffect(() => {
    if (selectedNode) {
      selectedNode.scrollIntoView({ block: 'center' });
      selectedNode.focus();
    }
  });

  const { branchLikeTree, hasResults, onSelect, selectedBranchLike } = props;

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

  const branches = [branchLikeTree.mainBranchTree, ...branchLikeTree.branchTree];

  return (
    <ul className="item-list sw-overflow-y-auto sw-overflow-x-hidden">
      {!hasResults && (
        <div className="sw-px-3 sw-py-2">
          <span>{translate('no_results')}</span>
        </div>
      )}

      {/* BRANCHES & PR */}
      {branches.filter(isDefined).map((tree, treeIndex) => (
        <React.Fragment key={getBranchLikeKey(tree.branch)}>
          {renderItem(tree.branch)}
          {tree.pullRequests.length > 0 && (
            <>
              <ItemDivider />
              <ItemHeader>{translate('branch_like_navigation.pull_requests')}</ItemHeader>
              <ItemDivider />
              {tree.pullRequests.map((pr) => renderItem(pr, true))}
              {tree.pullRequests.length > 0 && treeIndex !== branches.length - 1 && <ItemDivider />}
            </>
          )}
        </React.Fragment>
      ))}

      {/* PARENTLESS PR (for display during search) */}
      {branchLikeTree.parentlessPullRequests.length > 0 && (
        <>
          <ItemDivider />
          <ItemHeader>{translate('branch_like_navigation.pull_requests')}</ItemHeader>
          <ItemDivider />
          {branchLikeTree.parentlessPullRequests.map((pr) => renderItem(pr))}
        </>
      )}

      {/* ORPHAN PR */}
      {branchLikeTree.orphanPullRequests.length > 0 && (
        <>
          <ItemDivider />
          <ItemHeader>
            {translate('branch_like_navigation.orphan_pull_requests')}
            <HelpTooltip
              className="sw-ml-1"
              overlay={translate('branch_like_navigation.orphan_pull_requests.tooltip')}
            >
              <HelperHintIcon />
            </HelpTooltip>
          </ItemHeader>
          <ItemDivider />
          {branchLikeTree.orphanPullRequests.map((pr) => renderItem(pr))}
        </>
      )}
    </ul>
  );
}

export default React.memo(MenuItemList);
