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
import { ChevronDownIcon, TextMuted } from 'design-system';
import * as React from 'react';
import BranchLikeIcon from '../../../../../components/icons/BranchLikeIcon';
import { getBranchLikeDisplayName } from '../../../../../helpers/branch-like';
import { BranchLike, BranchStatusData } from '../../../../../types/branch-like';
import QualityGateStatus from './QualityGateStatus';

export interface CurrentBranchLikeProps extends Pick<BranchStatusData, 'status'> {
  currentBranchLike: BranchLike;
}

export function CurrentBranchLike(props: CurrentBranchLikeProps) {
  const { currentBranchLike } = props;

  const displayName = getBranchLikeDisplayName(currentBranchLike);

  return (
    <div className="sw-flex sw-items-center text-ellipsis">
      <BranchLikeIcon branchLike={currentBranchLike} />
      <TextMuted text={displayName} className="sw-ml-3" />
      <QualityGateStatus branchLike={currentBranchLike} className="sw-ml-4" />
      <ChevronDownIcon className="sw-ml-1" />
    </div>
  );
}

export default React.memo(CurrentBranchLike);
