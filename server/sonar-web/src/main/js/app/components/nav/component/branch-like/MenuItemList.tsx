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
import HelpTooltip from '../../../../../components/controls/HelpTooltip';
import { getBranchLikeKey, isSameBranchLike } from '../../../../../helpers/branch-like';
import { translate } from '../../../../../helpers/l10n';
import { scrollToElement } from '../../../../../helpers/scrolling';
import { isDefined } from '../../../../../helpers/types';
import { BranchLike, BranchLikeTree } from '../../../../../types/branch-like';
import { Component } from '../../../../../types/types';
import MenuItem from './MenuItem';

export interface MenuItemListProps {
  branchLikeTree: BranchLikeTree;
  component: Component;
  hasResults: boolean;
  onSelect: (branchLike: BranchLike) => void;
  selectedBranchLike: BranchLike | undefined;
  comparisonBranchesEnabled: boolean;
}

export function MenuItemList(props: MenuItemListProps) {
  let listNode: HTMLUListElement | null = null;
  let selectedNode: HTMLLIElement | null = null;

  React.useEffect(() => {
    if (listNode && selectedNode) {
      scrollToElement(selectedNode, { parent: listNode, smooth: false });
    }
  });

  const { branchLikeTree, component, hasResults, onSelect, selectedBranchLike } = props;

  const renderItem = (branchLike: BranchLike, indent?: boolean) => (
    <MenuItem
      branchLike={branchLike}
      component={component}
      indent={indent}
      key={getBranchLikeKey(branchLike)}
      onSelect={onSelect}
      selected={isSameBranchLike(branchLike, selectedBranchLike)}
      setSelectedNode={(node) => (selectedNode = node)}
    />
  );

  return (
    <ul className="item-list" ref={(node) => (listNode = node)}>
      {!hasResults && (
        <li className="item">
          <span className="note">{translate('no_results')}</span>
        </li>
      )}

      {/* BRANCHES & PR */}
      {[branchLikeTree.mainBranchTree, ...branchLikeTree.branchTree]
        .filter(isDefined)
        .map((tree) => (
          <React.Fragment key={getBranchLikeKey(tree.branch)}>
            {renderItem(tree.branch)}
            {tree.pullRequests.length > 0 && (
              <>
                <li className="item header">
                  <span className="big-spacer-left">
                    {
                      props.comparisonBranchesEnabled
                          ? translate('branch_like_navigation.comparison_branches')
                          : translate('branch_like_navigation.pull_requests')
                    }
                  </span>
                </li>
                {tree.pullRequests.map((pr) => renderItem({...pr, isComparisonBranch: props.comparisonBranchesEnabled}, true))}
              </>
            )}
            <hr />
          </React.Fragment>
        ))}

      {/* PARENTLESS PR (for display during search) */}
      {branchLikeTree.parentlessPullRequests.length > 0 && (
        <>
          <li className="item header">
            {
              props.comparisonBranchesEnabled
                  ? translate('branch_like_navigation.comparison_branches')
                  : translate('branch_like_navigation.pull_requests')
            }
          </li>
          {branchLikeTree.parentlessPullRequests.map((pr) => renderItem({...pr, isComparisonBranch: props.comparisonBranchesEnabled}))}
        </>
      )}

      {/* ORPHAN PR */}
      {branchLikeTree.orphanPullRequests.length > 0 && (
        <>
          <li className="item header">
            {translate('branch_like_navigation.orphan_pull_requests')}
            <HelpTooltip
              className="little-spacer-left"
              overlay={translate('branch_like_navigation.orphan_pull_requests.tooltip')}
            />
          </li>
          {branchLikeTree.orphanPullRequests.map((pr) => renderItem({...pr, isComparisonBranch: props.comparisonBranchesEnabled}))}
        </>
      )}
    </ul>
  );
}

export default React.memo(MenuItemList);
