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
import * as classNames from 'classnames';
import * as React from 'react';
import { Link } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import BranchStatus from '../../../../components/common/BranchStatus';
import BranchIcon from '../../../../components/icons-components/BranchIcon';
import {
  getBranchLikeDisplayName,
  getBranchLikeKey,
  isMainBranch,
  isPullRequest,
  isShortLivingBranch
} from '../../../../helpers/branches';
import { getBranchLikeUrl } from '../../../../helpers/urls';

export interface Props {
  branchLike: T.BranchLike;
  component: T.Component;
  onSelect: (branchLike: T.BranchLike) => void;
  selected: boolean;
  innerRef?: (node: HTMLLIElement) => void;
}

export default function ComponentNavBranchesMenuItem({ branchLike, ...props }: Props) {
  const handleMouseEnter = () => {
    props.onSelect(branchLike);
  };

  const displayName = getBranchLikeDisplayName(branchLike);
  const shouldBeIndented =
    (isShortLivingBranch(branchLike) && !branchLike.isOrphan) || isPullRequest(branchLike);

  return (
    <li key={getBranchLikeKey(branchLike)} onMouseEnter={handleMouseEnter} ref={props.innerRef}>
      <Link
        className={classNames('navbar-context-meta-branch-menu-item', {
          active: props.selected
        })}
        to={getBranchLikeUrl(props.component.key, branchLike)}>
        <div
          className="navbar-context-meta-branch-menu-item-name text-ellipsis"
          title={displayName}>
          <BranchIcon
            branchLike={branchLike}
            className={classNames('little-spacer-right', { 'big-spacer-left': shouldBeIndented })}
          />
          {displayName}
          {isMainBranch(branchLike) && (
            <div className="badge spacer-left">{translate('branches.main_branch')}</div>
          )}
        </div>
        <div className="big-spacer-left note">
          <BranchStatus branchLike={branchLike} component={props.component.key} />
        </div>
      </Link>
    </li>
  );
}
