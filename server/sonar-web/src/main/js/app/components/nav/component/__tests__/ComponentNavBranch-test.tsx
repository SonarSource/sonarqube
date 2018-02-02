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
  LongLivingBranch,
  PullRequest
} from '../../../../types';
import { click } from '../../../../../helpers/testUtils';

const mainBranch: MainBranch = { isMain: true, name: 'master' };
const fooBranch: LongLivingBranch = { isMain: false, name: 'foo', type: BranchType.LONG };

it('renders main branch', () => {
  const component = {} as Component;
  expect(
    shallow(
      <ComponentNavBranch
        branchLikes={[mainBranch, fooBranch]}
        component={component}
        currentBranchLike={mainBranch}
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
        branchLikes={[branch, fooBranch]}
        component={component}
        currentBranchLike={branch}
      />,
      { context: { branchesEnabled: true } }
    )
  ).toMatchSnapshot();
});

it('renders pull request', () => {
  const pullRequest: PullRequest = {
    base: 'master',
    branch: 'feature',
    id: '1234',
    title: 'Feature PR',
    url: 'https://example.com/pull/1234'
  };
  const component = {} as Component;
  expect(
    shallow(
      <ComponentNavBranch
        branchLikes={[pullRequest, fooBranch]}
        component={component}
        currentBranchLike={pullRequest}
      />,
      { context: { branchesEnabled: true } }
    )
  ).toMatchSnapshot();
});

it('opens menu', () => {
  const component = {} as Component;
  const wrapper = shallow(
    <ComponentNavBranch
      branchLikes={[mainBranch, fooBranch]}
      component={component}
      currentBranchLike={mainBranch}
    />,
    { context: { branchesEnabled: true } }
  );
  expect(wrapper.find('ComponentNavBranchesMenu')).toHaveLength(0);
  click(wrapper.find('a'));
  expect(wrapper.find('ComponentNavBranchesMenu')).toHaveLength(1);
});

it('renders single branch popup', () => {
  const component = {} as Component;
  const wrapper = shallow(
    <ComponentNavBranch
      branchLikes={[mainBranch]}
      component={component}
      currentBranchLike={mainBranch}
    />,
    { context: { branchesEnabled: true } }
  );
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('BubblePopupHelper').prop('isOpen')).toBe(false);
  click(wrapper.find('a'));
  expect(wrapper.find('BubblePopupHelper').prop('isOpen')).toBe(true);
});

it('renders no branch support popup', () => {
  const component = {} as Component;
  const wrapper = shallow(
    <ComponentNavBranch
      branchLikes={[mainBranch, fooBranch]}
      component={component}
      currentBranchLike={mainBranch}
    />,
    { context: { branchesEnabled: false } }
  );
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('BubblePopupHelper').prop('isOpen')).toBe(false);
  click(wrapper.find('a'));
  expect(wrapper.find('BubblePopupHelper').prop('isOpen')).toBe(true);
});

it('renders nothing on SonarCloud without branch support', () => {
  const component = {} as Component;
  const wrapper = shallow(
    <ComponentNavBranch
      branchLikes={[mainBranch]}
      component={component}
      currentBranchLike={mainBranch}
    />,
    { context: { branchesEnabled: false, onSonarCloud: true } }
  );
  expect(wrapper.type()).toBeNull();
});
