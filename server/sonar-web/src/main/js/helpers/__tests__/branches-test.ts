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

import { getBrancheLikesAsTree, isSameBranchLike, sortBranches } from '../branches';
import {
  mockLongLivingBranch,
  mockMainBranch,
  mockPullRequest,
  mockShortLivingBranch
} from '../testMocks';

describe('#getBrancheLikesAsTree', () => {
  it('should correctly map branches and prs to tree object', () => {
    const main = mockMainBranch({ name: 'master' });
    const llb1 = mockLongLivingBranch({ name: 'llb1' });
    const llb2 = mockLongLivingBranch({ name: 'llb2' });
    const slb1 = mockShortLivingBranch({ name: 'slb1' });
    const slb2 = mockShortLivingBranch({ name: 'slb2' });

    const mainPr1 = mockPullRequest({ base: main.name, key: 'PR1' });
    const mainPr2 = mockPullRequest({ base: main.name, key: 'PR2' });
    const llb1Pr1 = mockPullRequest({ base: llb1.name, key: 'PR1' });
    const llb1Pr2 = mockPullRequest({ base: llb1.name, key: 'PR2' });
    const llb2Pr1 = mockPullRequest({ base: llb2.name, key: 'PR1' });
    const llb2Pr2 = mockPullRequest({ base: llb2.name, key: 'PR1' });
    const orphanPR1 = mockPullRequest({ isOrphan: true, key: 'PR1' });
    const orphanPR2 = mockPullRequest({ isOrphan: true, key: 'PR2' });
    const parentlessPR1 = mockPullRequest({ base: 'not_present_branch_1', key: 'PR1' });
    const parentlessPR2 = mockPullRequest({ base: 'not_present_branch_2', key: 'PR2' });

    expect(
      getBrancheLikesAsTree([
        llb2,
        llb1,
        main,
        orphanPR2,
        orphanPR1,
        slb2,
        slb1,
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
        { branch: llb1, pullRequests: [llb1Pr1, llb1Pr2] },
        { branch: llb2, pullRequests: [llb2Pr1, llb2Pr1] },
        { branch: slb1, pullRequests: [] },
        { branch: slb2, pullRequests: [] }
      ],
      parentlessPullRequests: [parentlessPR1, parentlessPR2],
      orphanPullRequests: [orphanPR1, orphanPR2]
    });
  });
});

describe('#sortBranches', () => {
  it('should sort branches correctly', () => {
    const main = mockMainBranch();
    const shortFoo = mockShortLivingBranch({ name: 'shortFoo', mergeBranch: 'master' });
    const shortBar = mockShortLivingBranch({ name: 'shortBar', mergeBranch: 'longBaz' });
    const shortPre = mockShortLivingBranch({ name: 'shortPre', mergeBranch: 'shortFoo' });
    const longBaz = mockLongLivingBranch({ name: 'longBaz' });
    const longQux = mockLongLivingBranch({ name: 'longQux' });
    const longQwe = mockLongLivingBranch({ name: 'longQwe' });
    const branchList = [shortFoo, longBaz, shortPre, longQux, main, longQwe, shortBar];

    const sortedBrancList = sortBranches(branchList);

    expect(sortedBrancList).toEqual([
      main,
      longBaz,
      longQux,
      longQwe,
      shortBar,
      shortFoo,
      shortPre
    ]);
  });
});

describe('#isSameBranchLike', () => {
  it('compares different kinds', () => {
    const main = mockMainBranch();
    const short = mockShortLivingBranch({ name: 'foo' });
    const long = mockLongLivingBranch({ name: 'foo' });
    const pr = mockPullRequest();
    expect(isSameBranchLike(main, pr)).toBeFalsy();
    expect(isSameBranchLike(main, short)).toBeFalsy();
    expect(isSameBranchLike(main, long)).toBeFalsy();
    expect(isSameBranchLike(pr, short)).toBeFalsy();
    expect(isSameBranchLike(pr, long)).toBeFalsy();
    expect(isSameBranchLike(short, long)).toBeFalsy();
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
    expect(
      isSameBranchLike(mockLongLivingBranch({ name: 'foo' }), mockLongLivingBranch({ name: 'foo' }))
    ).toBeTruthy();
    expect(
      isSameBranchLike(
        mockShortLivingBranch({ name: 'foo' }),
        mockShortLivingBranch({ name: 'foo' })
      )
    ).toBeTruthy();
    expect(
      isSameBranchLike(mockLongLivingBranch({ name: 'foo' }), mockLongLivingBranch({ name: 'bar' }))
    ).toBeFalsy();
    expect(
      isSameBranchLike(
        mockShortLivingBranch({ name: 'foo' }),
        mockShortLivingBranch({ name: 'bar' })
      )
    ).toBeFalsy();
  });
});
