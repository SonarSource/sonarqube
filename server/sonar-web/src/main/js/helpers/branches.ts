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
import { sortBy } from 'lodash';
import { Branch, BranchType, ShortLivingBranch, LongLivingBranch } from '../app/types';

export function isShortLivingBranch(branch?: Branch): branch is ShortLivingBranch {
  return branch !== undefined && !branch.isMain && branch.type === BranchType.SHORT;
}

export function isLongLivingBranch(branch?: Branch): branch is LongLivingBranch {
  return branch !== undefined && !branch.isMain && branch.type === BranchType.LONG;
}

export function getBranchName(branch?: Branch): string | undefined {
  return !branch || branch.isMain ? undefined : branch.name;
}

export function sortBranchesAsTree(branches: Branch[]): Branch[] {
  const result: Branch[] = [];

  const shortLivingBranches = branches.filter(isShortLivingBranch);

  // main branch is always first
  const mainBranch = branches.find(branch => branch.isMain);
  if (mainBranch) {
    result.push(mainBranch, ...getNestedShortLivingBranches(mainBranch.name));
  }

  // the all long-living branches
  sortBy(branches.filter(isLongLivingBranch), 'name').forEach(longLivingBranch => {
    result.push(longLivingBranch, ...getNestedShortLivingBranches(longLivingBranch.name));
  });

  // finally all orhpan branches
  result.push(...shortLivingBranches.filter(branch => branch.isOrphan));

  return result;

  /** Get all short-living branches (possibly nested) which should be merged to a given branch */
  function getNestedShortLivingBranches(mergeBranch: string): ShortLivingBranch[] {
    const found: ShortLivingBranch[] = shortLivingBranches.filter(
      branch => branch.mergeBranch === mergeBranch
    );

    let i = 0;
    while (i < found.length) {
      const current = found[i];
      found.push(...shortLivingBranches.filter(branch => branch.mergeBranch === current.name));
      i++;
    }

    return sortBy(found, 'name');
  }
}
