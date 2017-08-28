/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { Branch, Component } from '../../../types';
import BranchIcon from '../../../../components/icons-components/BranchIcon';
import { isShortLivingBranch } from '../../../../helpers/branches';
import { getProjectBranchUrl } from '../../../../helpers/urls';

interface Props {
  branch: Branch;
  component: Component;
  onSelect: (branch: Branch) => void;
  selected: boolean;
}

export default function ComponentNavBranchesMenuItem({ branch, ...props }: Props) {
  const handleMouseEnter = () => {
    props.onSelect(branch);
  };

  return (
    <li key={branch.name} onMouseEnter={handleMouseEnter}>
      <Link
        className={classNames('navbar-context-meta-branch-menu-item', {
          active: props.selected
        })}
        to={getProjectBranchUrl(props.component.key, branch)}>
        <div>
          <BranchIcon
            branch={branch}
            className={classNames('little-spacer-right', {
              'big-spacer-left': isShortLivingBranch(branch) && !branch.isOrphan
            })}
          />
          {branch.name}
        </div>
        <div className="big-spacer-left note">
          <BranchStatus branch={branch} concise={true} />
        </div>
      </Link>
    </li>
  );
}
