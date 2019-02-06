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
import * as React from 'react';
import { shallow } from 'enzyme';
import ComponentNavBranchesMenuItem, { Props } from '../ComponentNavBranchesMenuItem';

const component = { key: 'component' } as T.Component;

const shortBranch: T.ShortLivingBranch = {
  isMain: false,
  mergeBranch: 'master',
  name: 'foo',
  status: { qualityGateStatus: 'ERROR' },
  type: 'SHORT'
};

const mainBranch: T.MainBranch = { isMain: true, name: 'master' };

it('renders main branch', () => {
  expect(shallowRender({ branchLike: mainBranch })).toMatchSnapshot();
});

it('renders short-living branch', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('renders short-living orhpan branch', () => {
  const orhpan: T.ShortLivingBranch = { ...shortBranch, isOrphan: true };
  expect(shallowRender({ branchLike: orhpan })).toMatchSnapshot();
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow(
    <ComponentNavBranchesMenuItem
      branchLike={shortBranch}
      component={component}
      onSelect={jest.fn()}
      selected={false}
      {...props}
    />
  );
}
