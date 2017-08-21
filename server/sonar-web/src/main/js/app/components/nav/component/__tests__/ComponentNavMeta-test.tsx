/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import ComponentNavMeta from '../ComponentNavMeta';
import {
  Branch,
  Component,
  BranchType,
  ShortLivingBranch,
  LongLivingBranch
} from '../../../../types';

it('renders incremental badge', () => {
  check(true);
  check(false);

  function check(incremental: boolean) {
    expect(
      shallow(
        <ComponentNavMeta
          branch={{} as Branch}
          component={{ key: 'foo' } as Component}
          conf={{}}
          incremental={incremental}
        />
      ).find('IncrementalBadge')
    ).toHaveLength(incremental ? 1 : 0);
  }
});

it('renders status of short-living branch', () => {
  const branch: ShortLivingBranch = {
    isMain: false,
    name: 'feature',
    status: { bugs: 0, codeSmells: 2, vulnerabilities: 3 },
    type: BranchType.SHORT
  };
  expect(
    shallow(<ComponentNavMeta branch={branch} component={{ key: 'foo' } as Component} conf={{}} />)
  ).toMatchSnapshot();
});

it('renders nothing for long-living branch', () => {
  const branch: LongLivingBranch = {
    isMain: false,
    name: 'release',
    status: { qualityGateStatus: 'OK' },
    type: BranchType.LONG
  };
  expect(
    shallow(<ComponentNavMeta branch={branch} component={{ key: 'foo' } as Component} conf={{}} />)
  ).toMatchSnapshot();
});
