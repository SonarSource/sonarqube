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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import {
  grantPermissionToGroup,
  grantPermissionToUser,
  revokePermissionFromGroup,
  revokePermissionFromUser
} from '../../../../../api/permissions';
import { mockComponent, mockOrganization } from '../../../../../helpers/testMocks';
import App from '../App';

jest.mock('../../../../../api/permissions', () => ({
  getPermissionsGroupsForComponent: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 100, total: 2 },
    groups: [
      { name: 'Anyone', permissions: ['admin', 'codeviewer', 'issueadmin'] },
      { id: '1', name: 'SonarSource', description: 'SonarSource team', permissions: [] }
    ]
  }),
  getPermissionsUsersForComponent: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 100, total: 3 },
    users: [
      {
        avatar: 'admin-avatar',
        email: 'admin@gmail.com',
        login: 'admin',
        name: 'Admin Admin',
        permissions: ['admin']
      },
      {
        avatar: 'user-avatar-1',
        email: 'user1@gmail.com',
        login: 'user1',
        name: 'User Number 1',
        permissions: []
      },
      {
        avatar: 'user-avatar-2',
        email: 'user2@gmail.com',
        login: 'user2',
        name: 'User Number 2',
        permissions: []
      }
    ]
  }),
  grantPermissionToGroup: jest.fn().mockResolvedValue({}),
  grantPermissionToUser: jest.fn().mockResolvedValue({}),
  revokePermissionFromGroup: jest.fn().mockResolvedValue({}),
  revokePermissionFromUser: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

describe('should manage state correctly', () => {
  it('should handle permission select', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const instance = wrapper.instance();
    instance.handlePermissionSelect('foo');
    expect(wrapper.state('selectedPermission')).toBe('foo');
    instance.handlePermissionSelect('foo');
    expect(wrapper.state('selectedPermission')).toBe(undefined);
  });

  it('should add and remove permission to a group', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const instance = wrapper.instance();
    const apiPayload = {
      projectKey: 'my-project',
      groupName: 'Anyone',
      permission: 'foo',
      organization: 'foo'
    };

    instance.grantPermissionToGroup('Anyone', 'foo');
    const groupState = wrapper.state('groups');
    expect(groupState[0].permissions).toHaveLength(4);
    expect(groupState[0].permissions).toContain('foo');
    await waitAndUpdate(wrapper);
    expect(grantPermissionToGroup).toHaveBeenCalledWith(apiPayload);
    expect(wrapper.state('groups')).toBe(groupState);

    (grantPermissionToGroup as jest.Mock).mockRejectedValueOnce({});
    instance.grantPermissionToGroup('Anyone', 'bar');
    expect(wrapper.state('groups')[0].permissions).toHaveLength(5);
    expect(wrapper.state('groups')[0].permissions).toContain('bar');
    await waitAndUpdate(wrapper);
    expect(wrapper.state('groups')[0].permissions).toHaveLength(4);
    expect(wrapper.state('groups')[0].permissions).not.toContain('bar');

    instance.revokePermissionFromGroup('Anyone', 'foo');
    expect(wrapper.state('groups')[0].permissions).toHaveLength(3);
    expect(wrapper.state('groups')[0].permissions).not.toContain('foo');
    await waitAndUpdate(wrapper);
    expect(revokePermissionFromGroup).toHaveBeenCalledWith(apiPayload);
  });

  it('should add and remove permission to a user', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const instance = wrapper.instance();
    const apiPayload = {
      projectKey: 'my-project',
      login: 'user1',
      permission: 'foo',
      organization: 'foo'
    };

    instance.grantPermissionToUser('user1', 'foo');
    expect(wrapper.state('users')[1].permissions).toHaveLength(1);
    expect(wrapper.state('users')[1].permissions).toContain('foo');
    await waitAndUpdate(wrapper);
    expect(grantPermissionToUser).toHaveBeenCalledWith(apiPayload);

    instance.revokePermissionFromUser('user1', 'foo');
    expect(wrapper.state('users')[1].permissions).toHaveLength(0);
    await waitAndUpdate(wrapper);
    expect(revokePermissionFromUser).toHaveBeenCalledWith(apiPayload);
  });
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      component={mockComponent()}
      fetchOrganization={jest.fn()}
      onComponentChange={jest.fn()}
      organization={mockOrganization()}
      {...props}
    />
  );
}
