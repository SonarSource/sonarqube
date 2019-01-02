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
import { sortBy } from 'lodash';

export function isBranch(branchLike?: T.BranchLike): branchLike is T.Branch {
  return branchLike !== undefined && (branchLike as T.Branch).isMain !== undefined;
}

export function isShortLivingBranch(branchLike?: T.BranchLike): branchLike is T.ShortLivingBranch {
  return (
    isBranch(branchLike) &&
    !branchLike.isMain &&
    (branchLike as T.ShortLivingBranch).type === 'SHORT'
  );
}

export function isLongLivingBranch(branchLike?: T.BranchLike): branchLike is T.LongLivingBranch {
  return (
    isBranch(branchLike) && !branchLike.isMain && (branchLike as T.LongLivingBranch).type === 'LONG'
  );
}

export function isMainBranch(branchLike?: T.BranchLike): branchLike is T.MainBranch {
  return isBranch(branchLike) && branchLike.isMain;
}

export function isPullRequest(branchLike?: T.BranchLike): branchLike is T.PullRequest {
  return branchLike !== undefined && (branchLike as T.PullRequest).key !== undefined;
}

export function getPullRequestDisplayName(pullRequest: T.PullRequest) {
  return `${pullRequest.key} – ${pullRequest.title}`;
}

export function getBranchLikeDisplayName(branchLike: T.BranchLike) {
  return isPullRequest(branchLike) ? getPullRequestDisplayName(branchLike) : branchLike.name;
}

export function getBranchLikeKey(branchLike: T.BranchLike) {
  return isPullRequest(branchLike) ? `pull-request-${branchLike.key}` : `branch-${branchLike.name}`;
}

export function getBranchQualityGateColor(status: string) {
  let indicatorColor = 'gray';
  if (status === 'ERROR') {
    indicatorColor = 'red';
  } else if (status === 'WARN') {
    indicatorColor = 'orange';
  } else if (status === 'OK') {
    indicatorColor = 'green';
  }
  return indicatorColor;
}

export function isSameBranchLike(a: T.BranchLike | undefined, b: T.BranchLike | undefined) {
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
    return a.key === b.key;
  }

  // finally if both parameters are `undefined`, consider them equal
  return a === b;
}

export function sortBranchesAsTree(branchLikes: T.BranchLike[]) {
  const result: T.BranchLike[] = [];

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
    ...sortBy(pullRequests.filter(pr => pr.isOrphan), pullRequest => pullRequest.key),
    ...sortBy(shortLivingBranches.filter(branch => branch.isOrphan), branch => branch.name)
  );

  return result;

  /** Get all short-living branches (possibly nested) which should be merged to a given branch */
  function getNestedShortLivingBranches(mergeBranch: string) {
    const found: T.ShortLivingBranch[] = shortLivingBranches.filter(
      branch => branch.mergeBranch === mergeBranch
    );

    let i = 0;
    while (i < found.length) {
      const current = found[i];
      found.push(...shortLivingBranches.filter(branch => branch.mergeBranch === current.name));
      i++;
    }

    return sortBy(found, branch => branch.name);
  }

  function getPullRequests(base: string) {
    return sortBy(pullRequests.filter(pr => pr.base === base), pullRequest => pullRequest.key);
  }
}

export function getBranchLikeQuery(branchLike?: T.BranchLike): T.BranchParameters {
  if (isShortLivingBranch(branchLike) || isLongLivingBranch(branchLike)) {
    return { branch: branchLike.name };
  } else if (isPullRequest(branchLike)) {
    return { pullRequest: branchLike.key };
  } else {
    return {};
  }
}

// Create branch object from branch name or pull request key
export function fillBranchLike(
  branch?: string,
  pullRequest?: string
): T.ShortLivingBranch | T.PullRequest | undefined {
  if (branch) {
    return {
      isMain: false,
      mergeBranch: '',
      name: branch,
      type: 'SHORT'
    } as T.ShortLivingBranch;
  } else if (pullRequest) {
    return { base: '', branch: '', key: pullRequest, title: '' } as T.PullRequest;
  }
  return undefined;
}
