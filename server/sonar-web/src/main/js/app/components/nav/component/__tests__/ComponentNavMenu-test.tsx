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
import ComponentNavMenu from '../ComponentNavMenu';
import {
  Component,
  ShortLivingBranch,
  BranchType,
  LongLivingBranch,
  MainBranch
} from '../../../../types';

const mainBranch: MainBranch = {
  isMain: true,
  name: 'master'
};

it('should work with extensions', () => {
  const component = {
    key: 'foo',
    qualifier: 'TRK',
    extensions: [{ key: 'component-foo', name: 'ComponentFoo' }]
  };
  const conf = {
    showSettings: true,
    extensions: [{ key: 'foo', name: 'Foo' }]
  };
  expect(
    shallow(
      <ComponentNavMenu branch={mainBranch} component={component as Component} conf={conf} />,
      { context: { branchesEnabled: true } }
    )
  ).toMatchSnapshot();
});

it('should work with multiple extensions', () => {
  const component = {
    key: 'foo',
    qualifier: 'TRK',
    extensions: [
      { key: 'component-foo', name: 'ComponentFoo' },
      { key: 'component-bar', name: 'ComponentBar' }
    ]
  };
  const conf = {
    showSettings: true,
    extensions: [{ key: 'foo', name: 'Foo' }, { key: 'bar', name: 'Bar' }]
  };
  expect(
    shallow(
      <ComponentNavMenu branch={mainBranch} component={component as Component} conf={conf} />,
      { context: { branchesEnabled: true } }
    )
  ).toMatchSnapshot();
});

it('should work for short-living branches', () => {
  const branch: ShortLivingBranch = {
    isMain: false,
    mergeBranch: 'master',
    name: 'feature',
    status: { bugs: 0, codeSmells: 2, vulnerabilities: 3 },
    type: BranchType.SHORT
  };
  const component = { key: 'foo', qualifier: 'TRK' } as Component;
  const conf = { showSettings: true };
  expect(
    shallow(<ComponentNavMenu branch={branch} component={component} conf={conf} />, {
      context: { branchesEnabled: true }
    })
  ).toMatchSnapshot();
});

it('should work for long-living branches', () => {
  const branch: LongLivingBranch = { isMain: false, name: 'release', type: BranchType.LONG };
  const component = { key: 'foo', qualifier: 'TRK' } as Component;
  const conf = { showSettings: true };
  expect(
    shallow(<ComponentNavMenu branch={branch} component={component} conf={conf} />, {
      context: { branchesEnabled: true }
    })
  ).toMatchSnapshot();
});
