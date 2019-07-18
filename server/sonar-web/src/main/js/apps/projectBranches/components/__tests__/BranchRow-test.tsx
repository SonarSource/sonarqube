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
import { shallow } from 'enzyme';
import * as React from 'react';
import { click } from 'sonar-ui-common/helpers/testUtils';
import { mockPullRequest, mockShortLivingBranch } from '../../../../helpers/testMocks';
import BranchRow from '../BranchRow';

const mainBranch: T.MainBranch = { isMain: true, name: 'master' };

const shortBranch = mockShortLivingBranch();

const pullRequest = mockPullRequest();

it('renders main branch', () => {
  expect(shallowRender(mainBranch)).toMatchSnapshot();
});

it('renders short-living branch', () => {
  expect(shallowRender(shortBranch)).toMatchSnapshot();
});

it('renders pull request', () => {
  expect(shallowRender(pullRequest)).toMatchSnapshot();
});

it('renames main branch', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender(mainBranch, onChange);

  click(wrapper.find('.js-rename'));
  (wrapper.find('RenameBranchModal').prop('onRename') as Function)();
  expect(onChange).toBeCalled();
});

it('deletes short-living branch', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender(shortBranch, onChange);

  click(wrapper.find('.js-delete'));
  (wrapper.find('DeleteBranchModal').prop('onDelete') as Function)();
  expect(onChange).toBeCalled();
});

it('deletes pull request', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender(pullRequest, onChange);

  click(wrapper.find('.js-delete'));
  (wrapper.find('DeleteBranchModal').prop('onDelete') as Function)();
  expect(onChange).toBeCalled();
});

function shallowRender(branchLike: T.BranchLike, onChange: () => void = jest.fn()) {
  const wrapper = shallow(
    <BranchRow branchLike={branchLike} component="foo" isOrphan={false} onChange={onChange} />
  );
  (wrapper.instance() as any).mounted = true;
  return wrapper;
}
