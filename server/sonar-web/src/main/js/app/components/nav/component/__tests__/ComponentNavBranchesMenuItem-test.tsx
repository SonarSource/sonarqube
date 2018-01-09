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
import ComponentNavBranchesMenuItem, { Props } from '../ComponentNavBranchesMenuItem';
import { BranchType, MainBranch, ShortLivingBranch, Component } from '../../../../types';

const component = { key: 'component' } as Component;

const shortBranch: ShortLivingBranch = {
  isMain: false,
  mergeBranch: 'master',
  name: 'foo',
  status: { bugs: 1, codeSmells: 2, vulnerabilities: 3 },
  type: BranchType.SHORT
};

const mainBranch: MainBranch = { isMain: true, name: 'master' };

it('renders main branch', () => {
  expect(shallowRender({ branch: mainBranch })).toMatchSnapshot();
});

it('renders short-living branch', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('renders short-living orhpan branch', () => {
  expect(shallowRender({ branch: { ...shortBranch, isOrphan: true } })).toMatchSnapshot();
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow(
    <ComponentNavBranchesMenuItem
      branch={shortBranch}
      component={component}
      onSelect={jest.fn()}
      selected={false}
      {...props}
    />
  );
}
