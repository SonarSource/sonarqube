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
import {
  BranchLike,
  Branch,
  BranchType,
  ShortLivingBranch,
  LongLivingBranch,
  PullRequest,
  MainBranch,
  BranchParameters
} from '../app/types';

export function isBranch(branchLike?: BranchLike): branchLike is Branch {
  return branchLike !== undefined && (branchLike as Branch).isMain !== undefined;
}

export function isShortLivingBranch(branchLike?: BranchLike): branchLike is ShortLivingBranch {
  return (
    isBranch(branchLike) &&
    !branchLike.isMain &&
    (branchLike as ShortLivingBranch).type === BranchType.SHORT
  );
}

export function isLongLivingBranch(branchLike?: BranchLike): branchLike is LongLivingBranch {
  return (
    isBranch(branchLike) &&
    !branchLike.isMain &&
    (branchLike as LongLivingBranch).type === BranchType.LONG
  );
}

export function isMainBranch(branchLike?: BranchLike): branchLike is MainBranch {
  return isBranch(branchLike) && branchLike.isMain;
}

export function isPullRequest(branchLike?: BranchLike): branchLike is PullRequest {
  return branchLike !== undefined && (branchLike as PullRequest).id !== undefined;
}

export function getPullRequestDisplayName(pullRequest: PullRequest) {
  return `${pullRequest.id} – ${pullRequest.title}`;
}

export function getBranchLikeDisplayName(branchLike: BranchLike) {
  return isPullRequest(branchLike) ? getPullRequestDisplayName(branchLike) : branchLike.name;
}

export function getBranchLikeKey(branchLike: BranchLike) {
  return isPullRequest(branchLike) ? `pull-request-${branchLike.id}` : `branch-${branchLike.name}`;
}

export function isSameBranchLike(a: BranchLike | undefined, b: BranchLike | undefined) {
  // main branches are always equal
  if (isMainBranch(a) && isMainBranch(b)) {
    return true;
  }

  // short- and long-living branches are compared by type and name
  if (
    (isLongLivingBranch(a) && isLongLivingBranch(b)) ||
    (isShortLivingBranch(a) && isShortLivingBranch(b))
  ) {
    return a.type === b.type && a.name === b.name;
  }

  // pull requests are compared by id
  if (isPullRequest(a) && isPullRequest(b)) {
    return a.id === b.id;
  }

  // finally if both parameters are `undefined`, consider them equal
  return a === b;
}

export function sortBranchesAsTree(branchLikes: BranchLike[]) {
  const result: BranchLike[] = [];

  const mainBranch = branchLikes.find(isMainBranch);
  const longLivingBranches = branchLikes.filter(isLongLivingBranch);
  const shortLivingBranches = branchLikes.filter(isShortLivingBranch);
  const pullRequests = branchLikes.filter(isPullRequest);

  // main branch is always first
  if (mainBranch) {
    result.push(
      mainBranch,
      ...getPullRequests(mainBranch.name),
      ...getNestedShortLivingBranches(mainBranch.name)
    );
  }

  // then all long-living branches
  sortBy(longLivingBranches, 'name').forEach(longLivingBranch => {
    result.push(
      longLivingBranch,
      ...getPullRequests(longLivingBranch.name),
      ...getNestedShortLivingBranches(longLivingBranch.name)
    );
  });

  // finally all orhpan pull requests and branches
  result.push(
    ...pullRequests.filter(pr => pr.isOrphan),
    ...shortLivingBranches.filter(branch => branch.isOrphan)
  );

  return result;

  /** Get all short-living branches (possibly nested) which should be merged to a given branch */
  function getNestedShortLivingBranches(mergeBranch: string) {
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

  function getPullRequests(base: string) {
    return pullRequests.filter(pr => pr.base === base);
  }
}

export function getBranchLikeQuery(branchLike?: BranchLike): BranchParameters {
  if (isShortLivingBranch(branchLike) || isLongLivingBranch(branchLike)) {
    return { branch: branchLike.name };
  } else if (isPullRequest(branchLike)) {
    return { pullRequest: branchLike.id };
  } else {
    return {};
  }
}
