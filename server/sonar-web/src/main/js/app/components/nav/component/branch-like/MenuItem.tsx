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
import * as classNames from 'classnames';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import BranchStatus from '../../../../../components/common/BranchStatus';
import BranchLikeIcon from '../../../../../components/icons/BranchLikeIcon';
import { getBranchLikeDisplayName, isMainBranch } from '../../../../../helpers/branch-like';
import { BranchLike } from '../../../../../types/branch-like';

export interface MenuItemProps {
  branchLike: BranchLike;
  component: T.Component;
  indent?: boolean;
  onSelect: (branchLike: BranchLike) => void;
  selected: boolean;
  setSelectedNode?: (node: HTMLLIElement) => void;
}

export function MenuItem(props: MenuItemProps) {
  const { branchLike, component, indent, setSelectedNode, onSelect, selected } = props;
  const displayName = getBranchLikeDisplayName(branchLike);

  return (
    <li
      className={classNames('item', {
        active: selected
      })}
      onClick={() => onSelect(branchLike)}
      ref={selected ? setSelectedNode : undefined}>
      <div
        className={classNames('display-flex-center display-flex-space-between', {
          'big-spacer-left': indent
        })}>
        <div className="item-name text-ellipsis" title={displayName}>
          <BranchLikeIcon branchLike={branchLike} />
          <span className="spacer-left">{displayName}</span>
          {isMainBranch(branchLike) && (
            <span className="badge spacer-left">{translate('branches.main_branch')}</span>
          )}
        </div>
        <div className="spacer-left">
          <BranchStatus branchLike={branchLike} component={component.key} />
        </div>
      </div>
    </li>
  );
}

export default React.memo(MenuItem);
