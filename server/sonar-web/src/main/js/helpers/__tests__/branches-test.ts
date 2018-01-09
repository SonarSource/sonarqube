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
import { sortBranchesAsTree } from '../branches';
import { MainBranch, BranchType, ShortLivingBranch, LongLivingBranch } from '../../app/types';

describe('#sortBranchesAsTree', () => {
  it('sorts main branch and short-living branches', () => {
    const main = mainBranch();
    const foo = shortLivingBranch('foo', 'master');
    const bar = shortLivingBranch('bar', 'master');
    expect(sortBranchesAsTree([main, foo, bar])).toEqual([main, bar, foo]);
  });

  it('sorts main branch and long-living branches', () => {
    const main = mainBranch();
    const foo = longLivingBranch('foo');
    const bar = longLivingBranch('bar');
    expect(sortBranchesAsTree([main, foo, bar])).toEqual([main, bar, foo]);
  });

  it('sorts all types of branches', () => {
    const main = mainBranch();
    const shortFoo = shortLivingBranch('shortFoo', 'master');
    const shortBar = shortLivingBranch('shortBar', 'longBaz');
    const shortPre = shortLivingBranch('shortPre', 'shortFoo');
    const longBaz = longLivingBranch('longBaz');
    const longQux = longLivingBranch('longQux');
    const longQwe = longLivingBranch('longQwe');
    // - main                     - main
    //    - shortFoo                - shortFoo
    //      - shortPre              - shortPre
    //    - longBaz       ---->   - longBaz
    //       - shortBar             - shortBar
    //       - longQwe            - longQwe
    //    - longQux               - longQux
    expect(
      sortBranchesAsTree([main, shortFoo, shortBar, shortPre, longBaz, longQux, longQwe])
    ).toEqual([main, shortFoo, shortPre, longBaz, shortBar, longQux, longQwe]);
  });
});

function mainBranch(): MainBranch {
  return { isMain: true, name: 'master' };
}

function shortLivingBranch(name: string, mergeBranch: string): ShortLivingBranch {
  const status = { bugs: 0, codeSmells: 0, vulnerabilities: 0 };
  return { isMain: false, mergeBranch, name, status, type: BranchType.SHORT };
}

function longLivingBranch(name: string): LongLivingBranch {
  const status = { qualityGateStatus: 'OK' };
  return { isMain: false, name, status, type: BranchType.LONG };
}
