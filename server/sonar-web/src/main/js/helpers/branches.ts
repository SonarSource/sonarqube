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
import { sortBy } from 'lodash';
import { Branch, BranchType, ShortLivingBranch, LongLivingBranch } from '../app/types';

export const MAIN_BRANCH: Branch = {
  isMain: true,
  name: undefined,
  status: { qualityGateStatus: 'OK' },
  type: BranchType.LONG
};

const MAIN_BRANCH_DISPLAY_NAME = 'master';

export function isShortLivingBranch(branch: Branch | null): branch is ShortLivingBranch {
  return branch != null && !branch.isMain && branch.type === BranchType.SHORT;
}

export function getBranchDisplayName(branch: Branch): string {
  return branch.isMain ? MAIN_BRANCH_DISPLAY_NAME : branch.name;
}

export function getBranchName(branch: Branch): string | undefined {
  return branch.isMain ? undefined : branch.name;
}

export function sortBranchesAsTree(branches: Branch[]): Branch[] {
  const result: Branch[] = [];

  // main branch is always first
  const mainBranch = branches.find(branch => branch.isMain);
  if (mainBranch) {
    result.push(mainBranch);
  }

  // then process all branches where merge branch is empty
  getChildren().forEach(processBranch);

  return result;

  function getChildren(mergeBranch?: string): Branch[] {
    return sortBy(
      branches.filter(branch => !branch.isMain && branch.mergeBranch === mergeBranch),
      branch => !isShortLivingBranch(branch),
      'name'
    );
  }

  function processBranch(branch: ShortLivingBranch | LongLivingBranch) {
    result.push(branch);
    getChildren(branch.name).forEach(processBranch);
  }
}
