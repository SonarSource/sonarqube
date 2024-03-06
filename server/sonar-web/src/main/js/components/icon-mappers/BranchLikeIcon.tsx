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
import { BranchIcon, IconProps, MainBranchIcon, PullRequestIcon, ThemeColors } from 'design-system';
import * as React from 'react';
import { isMainBranch, isPullRequest } from '../../helpers/branch-like';
import { BranchLike } from '../../types/branch-like';

export interface BranchLikeIconProps extends Omit<IconProps, 'fill'> {
  branchLike: BranchLike;
  fill?: ThemeColors;
}

export default function BranchLikeIcon({ branchLike, ...props }: Readonly<BranchLikeIconProps>) {
  if (isPullRequest(branchLike)) {
    return <PullRequestIcon fill="pageContentLight" {...props} />;
  } else if (isMainBranch(branchLike)) {
    return <MainBranchIcon fill="pageContentLight" {...props} />;
  }
  return <BranchIcon fill="pageContentLight" {...props} />;
}
