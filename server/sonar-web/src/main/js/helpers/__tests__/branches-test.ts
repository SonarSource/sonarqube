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
import { sortBranchesAsTree, isSameBranchLike } from '../branches';
import {
  MainBranch,
  BranchType,
  ShortLivingBranch,
  LongLivingBranch,
  PullRequest
} from '../../app/types';

describe('#sortBranchesAsTree', () => {
  it('sorts main branch and short-living branches', () => {
    const main = mainBranch();
    const foo = shortLivingBranch({ name: 'foo' });
    const bar = shortLivingBranch({ name: 'bar' });
    expect(sortBranchesAsTree([main, foo, bar])).toEqual([main, bar, foo]);
  });

  it('sorts main branch and long-living branches', () => {
    const main = mainBranch();
    const foo = longLivingBranch({ name: 'foo' });
    const bar = longLivingBranch({ name: 'bar' });
    expect(sortBranchesAsTree([main, foo, bar])).toEqual([main, bar, foo]);
  });

  it('sorts all types of branches', () => {
    const main = mainBranch();
    const shortFoo = shortLivingBranch({ name: 'shortFoo', mergeBranch: 'master' });
    const shortBar = shortLivingBranch({ name: 'shortBar', mergeBranch: 'longBaz' });
    const shortPre = shortLivingBranch({ name: 'shortPre', mergeBranch: 'shortFoo' });
    const longBaz = longLivingBranch({ name: 'longBaz' });
    const longQux = longLivingBranch({ name: 'longQux' });
    const longQwe = longLivingBranch({ name: 'longQwe' });
    const pr = pullRequest({ base: 'master' });
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
    const main = mainBranch();
    const short = shortLivingBranch({ name: 'foo' });
    const long = longLivingBranch({ name: 'foo' });
    const pr = pullRequest();
    expect(isSameBranchLike(main, pr)).toBeFalsy();
    expect(isSameBranchLike(main, short)).toBeFalsy();
    expect(isSameBranchLike(main, long)).toBeFalsy();
    expect(isSameBranchLike(pr, short)).toBeFalsy();
    expect(isSameBranchLike(pr, long)).toBeFalsy();
    expect(isSameBranchLike(short, long)).toBeFalsy();
  });

  it('compares pull requests', () => {
    expect(isSameBranchLike(pullRequest({ id: '1234' }), pullRequest({ id: '1234' }))).toBeTruthy();
    expect(isSameBranchLike(pullRequest({ id: '1234' }), pullRequest({ id: '5678' }))).toBeFalsy();
  });

  it('compares branches', () => {
    expect(
      isSameBranchLike(longLivingBranch({ name: 'foo' }), longLivingBranch({ name: 'foo' }))
    ).toBeTruthy();
    expect(
      isSameBranchLike(shortLivingBranch({ name: 'foo' }), shortLivingBranch({ name: 'foo' }))
    ).toBeTruthy();
    expect(
      isSameBranchLike(longLivingBranch({ name: 'foo' }), longLivingBranch({ name: 'bar' }))
    ).toBeFalsy();
    expect(
      isSameBranchLike(shortLivingBranch({ name: 'foo' }), shortLivingBranch({ name: 'bar' }))
    ).toBeFalsy();
  });
});

function mainBranch(): MainBranch {
  return { isMain: true, name: 'master' };
}

function shortLivingBranch(overrides?: Partial<ShortLivingBranch>): ShortLivingBranch {
  const status = { bugs: 0, codeSmells: 0, vulnerabilities: 0 };
  return {
    isMain: false,
    mergeBranch: 'master',
    name: 'foo',
    status,
    type: BranchType.SHORT,
    ...overrides
  };
}

function longLivingBranch(overrides?: Partial<LongLivingBranch>): LongLivingBranch {
  const status = { qualityGateStatus: 'OK' };
  return { isMain: false, name: 'foo', status, type: BranchType.LONG, ...overrides };
}

function pullRequest(overrides?: Partial<PullRequest>): PullRequest {
  const status = { bugs: 0, codeSmells: 0, vulnerabilities: 0 };
  return {
    base: 'master',
    branch: 'feature',
    id: '1234',
    status,
    title: 'Random Name',
    ...overrides
  };
}
