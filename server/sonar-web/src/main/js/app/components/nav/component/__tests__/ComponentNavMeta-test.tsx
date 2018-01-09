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
import * as React from 'react';
import { shallow } from 'enzyme';
import { ComponentNavMeta } from '../ComponentNavMeta';
import { BranchType, ShortLivingBranch, LongLivingBranch } from '../../../../types';

const component = {
  analysisDate: '2017-01-02T00:00:00.000Z',
  breadcrumbs: [],
  key: 'foo',
  name: 'Foo',
  organization: 'org',
  qualifier: 'TRK',
  version: '0.0.1'
};

it('renders status of short-living branch', () => {
  const branch: ShortLivingBranch = {
    isMain: false,
    mergeBranch: 'master',
    name: 'feature',
    status: { bugs: 0, codeSmells: 2, vulnerabilities: 3 },
    type: BranchType.SHORT
  };
  expect(
    shallow(
      <ComponentNavMeta branch={branch} component={component} currentUser={{ isLoggedIn: false }} />
    )
  ).toMatchSnapshot();
});

it('renders meta for long-living branch', () => {
  const branch: LongLivingBranch = {
    isMain: false,
    name: 'release',
    status: { qualityGateStatus: 'OK' },
    type: BranchType.LONG
  };
  expect(
    shallow(
      <ComponentNavMeta branch={branch} component={component} currentUser={{ isLoggedIn: false }} />
    )
  ).toMatchSnapshot();
});
