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
import ComponentNavBranch from '../ComponentNavBranch';
import {
  BranchType,
  ShortLivingBranch,
  MainBranch,
  Component,
  LongLivingBranch
} from '../../../../types';
import { click } from '../../../../../helpers/testUtils';

const fooBranch: LongLivingBranch = { isMain: false, name: 'foo', type: BranchType.LONG };

it('renders main branch', () => {
  const branch: MainBranch = { isMain: true, name: 'master' };
  const component = {} as Component;
  expect(
    shallow(
      <ComponentNavBranch
        branches={[branch, fooBranch]}
        component={component}
        currentBranch={branch}
      />,
      { context: { branchesEnabled: true } }
    )
  ).toMatchSnapshot();
});

it('renders short-living branch', () => {
  const branch: ShortLivingBranch = {
    isMain: false,
    mergeBranch: 'master',
    name: 'foo',
    status: { bugs: 0, codeSmells: 0, vulnerabilities: 0 },
    type: BranchType.SHORT
  };
  const component = {} as Component;
  expect(
    shallow(
      <ComponentNavBranch
        branches={[branch, fooBranch]}
        component={component}
        currentBranch={branch}
      />,
      { context: { branchesEnabled: true } }
    )
  ).toMatchSnapshot();
});

it('opens menu', () => {
  const branch: MainBranch = { isMain: true, name: 'master' };
  const component = {} as Component;
  const wrapper = shallow(
    <ComponentNavBranch
      branches={[branch, fooBranch]}
      component={component}
      currentBranch={branch}
    />,
    { context: { branchesEnabled: true } }
  );
  expect(wrapper.find('ComponentNavBranchesMenu')).toHaveLength(0);
  click(wrapper.find('a'));
  expect(wrapper.find('ComponentNavBranchesMenu')).toHaveLength(1);
});

it('renders single branch popup', () => {
  const branch: MainBranch = { isMain: true, name: 'master' };
  const component = {} as Component;
  const wrapper = shallow(
    <ComponentNavBranch branches={[branch]} component={component} currentBranch={branch} />,
    { context: { branchesEnabled: true } }
  );
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('BubblePopupHelper').prop('isOpen')).toBe(false);
  click(wrapper.find('a'));
  expect(wrapper.find('BubblePopupHelper').prop('isOpen')).toBe(true);
});

it('renders no branch support popup', () => {
  const branch: MainBranch = { isMain: true, name: 'master' };
  const component = {} as Component;
  const wrapper = shallow(
    <ComponentNavBranch
      branches={[branch, fooBranch]}
      component={component}
      currentBranch={branch}
    />,
    { context: { branchesEnabled: false } }
  );
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('BubblePopupHelper').prop('isOpen')).toBe(false);
  click(wrapper.find('a'));
  expect(wrapper.find('BubblePopupHelper').prop('isOpen')).toBe(true);
});

it('renders nothing on SonarCloud without branch support', () => {
  const branch: MainBranch = { isMain: true, name: 'master' };
  const component = {} as Component;
  const wrapper = shallow(
    <ComponentNavBranch branches={[branch]} component={component} currentBranch={branch} />,
    { context: { branchesEnabled: false, onSonarCloud: true } }
  );
  expect(wrapper.type()).toBeNull();
});
