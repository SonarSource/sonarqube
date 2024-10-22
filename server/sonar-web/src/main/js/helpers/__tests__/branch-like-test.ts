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

import { getBrancheLikesAsTree, isSameBranchLike, sortBranches } from '../branch-like';
import { mockBranch, mockMainBranch, mockPullRequest } from '../mocks/branch-like';

describe('#getBrancheLikesAsTree', () => {
  it('should correctly map branches and prs to tree object', () => {
    const main = mockMainBranch({ name: 'master' });
    const branch1 = mockBranch({ name: 'branch-1' });
    const branch2 = mockBranch({ name: 'branch-2' });
    const branch3 = mockBranch({ name: 'branch-3' });
    const branch4 = mockBranch({ name: 'branch-4' });

    const mainPr1 = mockPullRequest({ base: main.name, key: '1' });
    const mainPr2 = mockPullRequest({ base: main.name, key: '2' });
    const branch1Pr1 = mockPullRequest({ base: branch1.name, key: '3' });
    const branch1Pr2 = mockPullRequest({ base: branch1.name, key: '4' });
    const branch2Pr1 = mockPullRequest({ base: branch2.name, key: '5' });
    const branch2Pr2 = mockPullRequest({ base: branch2.name, key: '6' });
    const orphanPR1 = mockPullRequest({ isOrphan: true, key: '7' });
    const orphanPR2 = mockPullRequest({ isOrphan: true, key: '8' });
    const parentlessPR1 = mockPullRequest({ base: 'not_present_branch_1', key: '9' });
    const parentlessPR2 = mockPullRequest({ base: 'not_present_branch_2', key: '10' });

    expect(
      getBrancheLikesAsTree([
        branch2,
        branch1,
        main,
        orphanPR2,
        orphanPR1,
        branch4,
        branch3,
        mainPr2,
        mainPr1,
        parentlessPR2,
        parentlessPR1,
        branch2Pr2,
        branch2Pr1,
        branch1Pr2,
        branch1Pr1,
      ]),
    ).toEqual({
      mainBranchTree: {
        branch: main,
        pullRequests: [mainPr2, mainPr1],
      },
      branchTree: [
        { branch: branch1, pullRequests: [branch1Pr2, branch1Pr1] },
        { branch: branch2, pullRequests: [branch2Pr2, branch2Pr1] },
        { branch: branch3, pullRequests: [] },
        { branch: branch4, pullRequests: [] },
      ],
      parentlessPullRequests: [parentlessPR2, parentlessPR1],
      orphanPullRequests: [orphanPR2, orphanPR1],
    });
  });
});

describe('#sortBranches', () => {
  it('should sort branches correctly', () => {
    const main = mockMainBranch();
    const foo = mockBranch({ name: 'shortFoo' });
    const bar = mockBranch({ name: 'shortBar' });
    const pre = mockBranch({ name: 'shortPre' });
    const baz = mockBranch({ name: 'longBaz' });
    const qux = mockBranch({ name: 'longQux' });
    const qwe = mockBranch({ name: 'longQwe' });
    const branchList = [foo, baz, pre, qux, main, qwe, bar];

    const sortedBrancList = sortBranches(branchList);

    expect(sortedBrancList).toEqual([main, baz, qux, qwe, bar, foo, pre]);
  });
});

describe('#isSameBranchLike', () => {
  it('compares different kinds', () => {
    const main = mockMainBranch();
    const foo = mockBranch({ name: 'foo' });
    const foo1 = mockBranch({ name: 'foo-1' });
    const pr = mockPullRequest();
    expect(isSameBranchLike(main, pr)).toBe(false);
    expect(isSameBranchLike(main, foo1)).toBe(false);
    expect(isSameBranchLike(main, foo)).toBe(false);
    expect(isSameBranchLike(pr, foo1)).toBe(false);
    expect(isSameBranchLike(pr, foo)).toBe(false);
    expect(isSameBranchLike(foo1, foo)).toBe(false);
  });

  it('compares pull requests', () => {
    expect(
      isSameBranchLike(mockPullRequest({ key: '1234' }), mockPullRequest({ key: '1234' })),
    ).toBe(true);
    expect(
      isSameBranchLike(mockPullRequest({ key: '1234' }), mockPullRequest({ key: '5678' })),
    ).toBe(false);
  });

  it('compares branches', () => {
    expect(isSameBranchLike(mockBranch({ name: 'foo' }), mockBranch({ name: 'foo' }))).toBe(true);
    expect(isSameBranchLike(mockBranch({ name: 'foo' }), mockBranch({ name: 'foo' }))).toBe(true);
    expect(isSameBranchLike(mockBranch({ name: 'foo' }), mockBranch({ name: 'bar' }))).toBe(false);
    expect(isSameBranchLike(mockBranch({ name: 'foo' }), mockBranch({ name: 'bar' }))).toBe(false);
  });
});
