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

import classNames from 'classnames';
import * as React from 'react';
import { Badge, ItemButton, TextBold, TextMuted } from '~design-system';
import { isMainBranch } from '~sonar-aligned/helpers/branch-like';
import BranchLikeIcon from '../../../../../components/icon-mappers/BranchLikeIcon';
import { getBranchLikeDisplayName } from '../../../../../helpers/branch-like';
import { translate } from '../../../../../helpers/l10n';
import { BranchLike } from '../../../../../types/branch-like';
import QualityGateStatus from './QualityGateStatus';

export interface MenuItemProps {
  branchLike: BranchLike;
  indent: boolean;
  onSelect: (branchLike: BranchLike) => void;
  selected: boolean;
  setSelectedNode?: (node: HTMLLIElement) => void;
}

export function MenuItem(props: MenuItemProps) {
  const { branchLike, setSelectedNode, onSelect, selected, indent } = props;
  const displayName = getBranchLikeDisplayName(branchLike);

  return (
    <ItemButton
      className={classNames({ active: selected, 'sw-pl-6': indent })}
      innerRef={selected ? setSelectedNode : undefined}
      onClick={() => {
        onSelect(branchLike);
      }}
    >
      <div className="sw-flex sw-items-center sw-justify-between sw-truncate sw-flex-1">
        <div className="sw-flex sw-items-center">
          <BranchLikeIcon branchLike={branchLike} />

          {isMainBranch(branchLike) && (
            <>
              <TextBold name={displayName} className="sw-ml-4 sw-mr-2" />
              <Badge variant="default">{translate('branches.main_branch')}</Badge>
            </>
          )}
          {!isMainBranch(branchLike) && (
            <TextMuted text={displayName} className="sw-ml-3 sw-mr-2" />
          )}
        </div>
        <QualityGateStatus
          branchLike={branchLike}
          className="sw-flex sw-items-center sw-w-24"
          showStatusText
        />
      </div>
    </ItemButton>
  );
}

export default React.memo(MenuItem);
