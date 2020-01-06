/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

    const mainPr1 = mockPullRequest({ base: main.name, key: 'PR1' });
    const mainPr2 = mockPullRequest({ base: main.name, key: 'PR2' });
    const llb1Pr1 = mockPullRequest({ base: branch1.name, key: 'PR1' });
    const llb1Pr2 = mockPullRequest({ base: branch1.name, key: 'PR2' });
    const llb2Pr1 = mockPullRequest({ base: branch2.name, key: 'PR1' });
    const llb2Pr2 = mockPullRequest({ base: branch2.name, key: 'PR1' });
    const orphanPR1 = mockPullRequest({ isOrphan: true, key: 'PR1' });
    const orphanPR2 = mockPullRequest({ isOrphan: true, key: 'PR2' });
    const parentlessPR1 = mockPullRequest({ base: 'not_present_branch_1', key: 'PR1' });
    const parentlessPR2 = mockPullRequest({ base: 'not_present_branch_2', key: 'PR2' });

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
        llb2Pr2,
        llb2Pr1,
        llb1Pr2,
        llb1Pr1
      ])
    ).toEqual({
      mainBranchTree: {
        branch: main,
        pullRequests: [mainPr1, mainPr2]
      },
      branchTree: [
        { branch: branch1, pullRequests: [llb1Pr1, llb1Pr2] },
        { branch: branch2, pullRequests: [llb2Pr1, llb2Pr1] },
        { branch: branch3, pullRequests: [] },
        { branch: branch4, pullRequests: [] }
      ],
      parentlessPullRequests: [parentlessPR1, parentlessPR2],
      orphanPullRequests: [orphanPR1, orphanPR2]
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
    expect(isSameBranchLike(main, pr)).toBeFalsy();
    expect(isSameBranchLike(main, foo1)).toBeFalsy();
    expect(isSameBranchLike(main, foo)).toBeFalsy();
    expect(isSameBranchLike(pr, foo1)).toBeFalsy();
    expect(isSameBranchLike(pr, foo)).toBeFalsy();
    expect(isSameBranchLike(foo1, foo)).toBeFalsy();
  });

  it('compares pull requests', () => {
    expect(
      isSameBranchLike(mockPullRequest({ key: '1234' }), mockPullRequest({ key: '1234' }))
    ).toBeTruthy();
    expect(
      isSameBranchLike(mockPullRequest({ key: '1234' }), mockPullRequest({ key: '5678' }))
    ).toBeFalsy();
  });

  it('compares branches', () => {
    expect(isSameBranchLike(mockBranch({ name: 'foo' }), mockBranch({ name: 'foo' }))).toBeTruthy();
    expect(isSameBranchLike(mockBranch({ name: 'foo' }), mockBranch({ name: 'foo' }))).toBeTruthy();
    expect(isSameBranchLike(mockBranch({ name: 'foo' }), mockBranch({ name: 'bar' }))).toBeFalsy();
    expect(isSameBranchLike(mockBranch({ name: 'foo' }), mockBranch({ name: 'bar' }))).toBeFalsy();
  });
});
