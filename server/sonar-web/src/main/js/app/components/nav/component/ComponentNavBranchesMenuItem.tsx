/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Link } from 'react-router';
import * as classNames from 'classnames';
import BranchStatus from '../../../../components/common/BranchStatus';
import { BranchLike, Component } from '../../../types';
import BranchIcon from '../../../../components/icons-components/BranchIcon';
import {
  isShortLivingBranch,
  getBranchLikeDisplayName,
  getBranchLikeKey,
  isMainBranch,
  isPullRequest
} from '../../../../helpers/branches';
import { translate } from '../../../../helpers/l10n';
import { getBranchLikeUrl } from '../../../../helpers/urls';
import Tooltip from '../../../../components/controls/Tooltip';

export interface Props {
  branchLike: BranchLike;
  component: Component;
  onSelect: (branchLike: BranchLike) => void;
  selected: boolean;
}

export default function ComponentNavBranchesMenuItem({ branchLike, ...props }: Props) {
  const handleMouseEnter = () => {
    props.onSelect(branchLike);
  };

  const displayName = getBranchLikeDisplayName(branchLike);
  const shouldBeIndented =
    (isShortLivingBranch(branchLike) && !branchLike.isOrphan) || isPullRequest(branchLike);

  return (
    <li key={getBranchLikeKey(branchLike)} onMouseEnter={handleMouseEnter}>
      <Tooltip mouseEnterDelay={0.5} overlay={displayName} placement="right">
        <Link
          className={classNames('navbar-context-meta-branch-menu-item', {
            active: props.selected
          })}
          to={getBranchLikeUrl(props.component.key, branchLike)}>
          <div className="navbar-context-meta-branch-menu-item-name text-ellipsis">
            <BranchIcon
              branchLike={branchLike}
              className={classNames('little-spacer-right', { 'big-spacer-left': shouldBeIndented })}
            />
            {displayName}
            {isMainBranch(branchLike) && (
              <div className="outline-badge spacer-left">{translate('branches.main_branch')}</div>
            )}
          </div>
          <div className="big-spacer-left note">
            <BranchStatus branchLike={branchLike} concise={true} />
          </div>
        </Link>
      </Tooltip>
    </li>
  );
}
