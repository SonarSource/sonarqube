/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import {
  addGroup,
  addUser,
  removeGroup,
  removeUser,
  searchGroups,
  searchUsers
} from '../../../../api/quality-gates';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import { mockUserBase } from '../../../../helpers/mocks/users';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import QualityGatePermissions from '../QualityGatePermissions';

jest.mock('../../../../api/quality-gates', () => ({
  addUser: jest.fn().mockResolvedValue(undefined),
  removeUser: jest.fn().mockResolvedValue(undefined),
  searchUsers: jest.fn().mockResolvedValue({ users: [] }),
  addGroup: jest.fn().mockResolvedValue(undefined),
  removeGroup: jest.fn().mockResolvedValue(undefined),
  searchGroups: jest.fn().mockResolvedValue({ groups: [] })
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should fetch users and groups', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(searchUsers).toBeCalledWith({ gateName: 'qualitygate', selected: 'selected' });
  expect(searchGroups).toBeCalledWith({ gateName: 'qualitygate', selected: 'selected' });
});

it('should handle errors when fetching users and groups', async () => {
  (searchUsers as jest.Mock).mockRejectedValueOnce('nope');
  (searchGroups as jest.Mock).mockRejectedValueOnce('nope');

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().groups).toHaveLength(0);
  expect(wrapper.state().users).toHaveLength(0);
});

it('should fetch users and groups on update', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  (searchUsers as jest.Mock).mockClear();

  wrapper.setProps({ qualityGate: mockQualityGate({ id: '2', name: 'qg2' }) });
  expect(searchUsers).toBeCalledWith({ gateName: 'qg2', selected: 'selected' });
});

it('should handleCloseAddPermission', () => {
  const wrapper = shallowRender();
  wrapper.setState({ showAddModal: true });
  wrapper.instance().handleCloseAddPermission();
  expect(wrapper.state().showAddModal).toBe(false);
});

it('should handleClickAddPermission', () => {
  const wrapper = shallowRender();
  wrapper.setState({ showAddModal: false });
  wrapper.instance().handleClickAddPermission();
  expect(wrapper.state().showAddModal).toBe(true);
});

it('should handleSubmitAddPermission for a user', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().users).toHaveLength(0);
  expect(wrapper.state().groups).toHaveLength(0);

  wrapper.instance().handleSubmitAddPermission(mockUserBase({ login: 'user1', name: 'User One' }));
  expect(wrapper.state().submitting).toBe(true);

  expect(addUser).toBeCalledWith({ gateName: 'qualitygate', login: 'user1' });
  expect(addGroup).not.toBeCalled();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().submitting).toBe(false);
  expect(wrapper.state().showAddModal).toBe(false);
  expect(wrapper.state().users).toHaveLength(1);
  expect(wrapper.state().groups).toHaveLength(0);
});

it('should handleSubmitAddPermission for a group', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().users).toHaveLength(0);
  expect(wrapper.state().groups).toHaveLength(0);

  wrapper.instance().handleSubmitAddPermission({ name: 'group' });
  expect(wrapper.state().submitting).toBe(true);

  expect(addUser).not.toBeCalled();
  expect(addGroup).toBeCalledWith({ gateName: 'qualitygate', groupName: 'group' });

  await waitAndUpdate(wrapper);

  expect(wrapper.state().submitting).toBe(false);
  expect(wrapper.state().showAddModal).toBe(false);
  expect(wrapper.state().users).toHaveLength(0);
  expect(wrapper.state().groups).toHaveLength(1);
});

it('should handleSubmitAddPermission if it returns an error', async () => {
  (addUser as jest.Mock).mockRejectedValueOnce(undefined);
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().users).toHaveLength(0);

  wrapper.setState({ showAddModal: true });
  wrapper.instance().handleSubmitAddPermission(mockUserBase({ login: 'user1', name: 'User One' }));
  expect(wrapper.state().submitting).toBe(true);

  expect(addUser).toBeCalledWith({ gateName: 'qualitygate', login: 'user1' });

  await waitAndUpdate(wrapper);

  expect(wrapper.state().submitting).toBe(false);
  expect(wrapper.state().showAddModal).toBe(true);
  expect(wrapper.state().users).toHaveLength(0);
});

it('should handleCloseDeletePermission', () => {
  const wrapper = shallowRender();
  wrapper.setState({ permissionToDelete: mockUserBase() });
  wrapper.instance().handleCloseDeletePermission();
  expect(wrapper.state().permissionToDelete).toBeUndefined();
});

it('should handleClickDeletePermission', () => {
  const user = mockUserBase();

  const wrapper = shallowRender();
  wrapper.setState({ permissionToDelete: undefined });
  wrapper.instance().handleClickDeletePermission(user);
  expect(wrapper.state().permissionToDelete).toBe(user);
});

it('should handleConfirmDeletePermission for a user', async () => {
  const deleteThisUser = mockUserBase();
  const deleteThisGroup = { name: 'deletableGroup' };
  (searchUsers as jest.Mock).mockResolvedValueOnce({ users: [deleteThisUser] });
  (searchGroups as jest.Mock).mockResolvedValueOnce({ groups: [deleteThisGroup] });
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().users).toHaveLength(1);
  expect(wrapper.state().groups).toHaveLength(1);

  wrapper.instance().handleConfirmDeletePermission(deleteThisUser);

  expect(removeUser).toBeCalledWith({ gateName: 'qualitygate', login: deleteThisUser.login });
  expect(removeGroup).not.toBeCalled();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().permissionToDelete).toBeUndefined();
  expect(wrapper.state().users).toHaveLength(0);
  expect(wrapper.state().groups).toHaveLength(1);
});

it('should handleConfirmDeletePermission for a group', async () => {
  const deleteThisUser = mockUserBase();
  const deleteThisGroup = { name: 'deletableGroup' };
  (searchUsers as jest.Mock).mockResolvedValueOnce({ users: [deleteThisUser] });
  (searchGroups as jest.Mock).mockResolvedValueOnce({ groups: [deleteThisGroup] });
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().users).toHaveLength(1);
  expect(wrapper.state().groups).toHaveLength(1);

  wrapper.instance().handleConfirmDeletePermission(deleteThisGroup);

  expect(removeUser).not.toBeCalled();
  expect(removeGroup).toBeCalledWith({ gateName: 'qualitygate', groupName: deleteThisGroup.name });

  await waitAndUpdate(wrapper);

  expect(wrapper.state().permissionToDelete).toBeUndefined();
  expect(wrapper.state().users).toHaveLength(1);
  expect(wrapper.state().groups).toHaveLength(0);
});

it('should handleConfirmDeletePermission if it returns an error', async () => {
  const deleteThisUser = mockUserBase();
  (searchUsers as jest.Mock).mockResolvedValueOnce({ users: [deleteThisUser] });
  (removeUser as jest.Mock).mockRejectedValueOnce(undefined);
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().users).toHaveLength(1);

  wrapper.instance().handleConfirmDeletePermission(deleteThisUser);

  expect(removeUser).toBeCalledWith({ gateName: 'qualitygate', login: deleteThisUser.login });

  await waitAndUpdate(wrapper);

  expect(wrapper.state().permissionToDelete).toBeUndefined();
  expect(wrapper.state().users).toHaveLength(1);
});

function shallowRender(overrides: Partial<QualityGatePermissions['props']> = {}) {
  return shallow<QualityGatePermissions>(
    <QualityGatePermissions qualityGate={mockQualityGate()} {...overrides} />
  );
}
