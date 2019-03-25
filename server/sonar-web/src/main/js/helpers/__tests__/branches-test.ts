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
import { sortBranchesAsTree, isSameBranchLike } from '../branches';
import {
  mockShortLivingBranch,
  mockLongLivingBranch,
  mockPullRequest,
  mockMainBranch
} from '../testMocks';

describe('#sortBranchesAsTree', () => {
  it('sorts main branch and short-living branches', () => {
    const main = mockMainBranch();
    const foo = mockShortLivingBranch({ name: 'foo' });
    const bar = mockShortLivingBranch({ name: 'bar' });
    expect(sortBranchesAsTree([main, foo, bar])).toEqual([main, bar, foo]);
  });

  it('sorts main branch and long-living branches', () => {
    const main = mockMainBranch();
    const foo = mockLongLivingBranch({ name: 'foo' });
    const bar = mockLongLivingBranch({ name: 'bar' });
    expect(sortBranchesAsTree([main, foo, bar])).toEqual([main, bar, foo]);
  });

  it('sorts all types of branches', () => {
    const main = mockMainBranch();
    const shortFoo = mockShortLivingBranch({ name: 'shortFoo', mergeBranch: 'master' });
    const shortBar = mockShortLivingBranch({ name: 'shortBar', mergeBranch: 'longBaz' });
    const shortPre = mockShortLivingBranch({ name: 'shortPre', mergeBranch: 'shortFoo' });
    const longBaz = mockLongLivingBranch({ name: 'longBaz' });
    const longQux = mockLongLivingBranch({ name: 'longQux' });
    const longQwe = mockLongLivingBranch({ name: 'longQwe' });
    const pr = mockPullRequest({ base: 'master' });
    // - main                     - main
    //    - shortFoo                - shortFoo
    //      - shortPre              - shortPre
    //    - longBaz       ---->   - longBaz
    //       - shortBar             - shortBar
    //       - longQwe            - longQwe
    //    - longQux               - longQux
    expect(
      sortBranchesAsTree([main, shortFoo, shortBar, shortPre, longBaz, longQux, longQwe, pr])
    ).toEqual([main, pr, shortFoo, shortPre, longBaz, shortBar, longQux, longQwe]);
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
