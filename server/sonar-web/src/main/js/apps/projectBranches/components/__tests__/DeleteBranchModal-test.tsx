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
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import { click, doAsync, submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { deleteBranch, deletePullRequest } from '../../../../api/branches';
import { mockPullRequest, mockShortLivingBranch } from '../../../../helpers/testMocks';
import DeleteBranchModal from '../DeleteBranchModal';

jest.mock('../../../../api/branches', () => ({
  deleteBranch: jest.fn(),
  deletePullRequest: jest.fn()
}));

const branch = mockShortLivingBranch();

beforeEach(() => {
  jest.clearAllMocks();
});

it('renders', () => {
  const wrapper = shallowRender(branch);
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ loading: true });
  expect(wrapper).toMatchSnapshot();
});

it('deletes branch', async () => {
  (deleteBranch as jest.Mock).mockImplementationOnce(() => Promise.resolve());
  const onDelete = jest.fn();
  const wrapper = shallowRender(branch, onDelete);

  submitForm(wrapper);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loading).toBe(false);
  expect(onDelete).toBeCalled();
  expect(deleteBranch).toBeCalledWith({ branch: 'feature/foo', project: 'foo' });
});

it('deletes pull request', async () => {
  (deletePullRequest as jest.Mock).mockImplementationOnce(() => Promise.resolve());
  const pullRequest = mockPullRequest();
  const onDelete = jest.fn();
  const wrapper = shallowRender(pullRequest, onDelete);

  submitForm(wrapper);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loading).toBe(false);
  expect(onDelete).toBeCalled();
  expect(deletePullRequest).toBeCalledWith({ project: 'foo', pullRequest: '1001' });
});

it('cancels', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender(branch, jest.fn(), onClose);

  click(wrapper.find('ResetButtonLink'));

  return doAsync().then(() => {
    expect(onClose).toBeCalled();
  });
});

it('stops loading on WS error', async () => {
  (deleteBranch as jest.Mock).mockImplementationOnce(() => Promise.reject(null));
  const onDelete = jest.fn();
  const wrapper = shallowRender(branch, onDelete);

  submitForm(wrapper);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loading).toBe(false);
  expect(onDelete).not.toBeCalled();
  expect(deleteBranch).toBeCalledWith({ branch: 'feature/foo', project: 'foo' });
});

function shallowRender(
  branchLike: T.BranchLike,
  onDelete: () => void = jest.fn(),
  onClose: () => void = jest.fn()
) {
  const wrapper = shallow<DeleteBranchModal>(
    <DeleteBranchModal
      branchLike={branchLike}
      component="foo"
      onClose={onClose}
      onDelete={onDelete}
    />
  );
  wrapper.instance().mounted = true;
  return wrapper;
}

function submitForm(wrapper: ShallowWrapper<any, any>) {
  submit(wrapper.find('form'));
  expect(wrapper.state().loading).toBe(true);
}
