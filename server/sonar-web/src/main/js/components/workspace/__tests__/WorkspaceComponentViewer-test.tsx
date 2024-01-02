/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { getParents } from '../../../api/components';
import { mockPullRequest } from '../../../helpers/mocks/branch-like';
import { mockIssue } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { Props, WorkspaceComponentViewer } from '../WorkspaceComponentViewer';

jest.mock('../../../api/components', () => ({
  getParents: jest.fn().mockResolvedValue([{ key: 'bar' }]),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should close', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose });
  wrapper.find('WorkspaceHeader').prop<Function>('onClose')();
  expect(onClose).toHaveBeenCalledWith('foo');
});

it('should call back after load', () => {
  const onLoad = jest.fn();
  const wrapper = shallowRender({ onLoad });
  wrapper.find('[onLoaded]').prop<Function>('onLoaded')({
    key: 'foo',
    path: 'src/foo.js',
    q: 'FIL',
  });
  expect(onLoad).toHaveBeenCalledWith({ key: 'foo', name: 'src/foo.js', qualifier: 'FIL' });
});

it('should refresh branch status if issues are updated', async () => {
  const fetchBranchStatus = jest.fn();
  const branchLike = mockPullRequest();
  const component = {
    branchLike,
    key: 'foo',
  };
  const wrapper = shallowRender({ component, fetchBranchStatus });
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  instance.handleIssueChange(mockIssue());
  expect(getParents).toHaveBeenCalledWith(component.key);
  await waitAndUpdate(wrapper);
  expect(fetchBranchStatus).toHaveBeenCalledWith(branchLike, 'bar');
});

function shallowRender(props?: Partial<Props>) {
  return shallow<WorkspaceComponentViewer>(
    <WorkspaceComponentViewer
      component={{ branchLike: undefined, key: 'foo' }}
      fetchBranchStatus={jest.fn()}
      height={300}
      onClose={jest.fn()}
      onCollapse={jest.fn()}
      onLoad={jest.fn()}
      onMaximize={jest.fn()}
      onMinimize={jest.fn()}
      onResize={jest.fn()}
      {...props}
    />
  );
}
