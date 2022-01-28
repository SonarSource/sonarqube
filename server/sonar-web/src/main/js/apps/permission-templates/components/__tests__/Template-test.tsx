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
  grantTemplatePermissionToGroup,
  grantTemplatePermissionToUser,
  revokeTemplatePermissionFromGroup,
  revokeTemplatePermissionFromUser
} from '../../../../api/permissions';
import Template from '../Template';

jest.mock('../../../../api/permissions', () => ({
  revokeTemplatePermissionFromUser: jest.fn().mockResolvedValue({}),
  grantTemplatePermissionToUser: jest.fn().mockResolvedValue({}),
  grantTemplatePermissionToGroup: jest.fn().mockResolvedValue({}),
  revokeTemplatePermissionFromGroup: jest.fn().mockResolvedValue({}),
  getPermissionTemplateGroups: jest.fn().mockResolvedValue([]),
  getPermissionTemplateUsers: jest.fn().mockResolvedValue([])
}));

it('render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('revoke group permission if granted', async () => {
  const wrapper = shallowRender();
  const group = {
    name: 'foo',
    permissions: ['bar']
  };
  wrapper.setState({
    groups: [group]
  });
  await wrapper.instance().handleToggleGroup(group, 'bar');
  expect(revokeTemplatePermissionFromGroup).toHaveBeenCalledWith({
    groupName: 'foo',
    templateId: '1',
    permission: 'bar'
  });
});

it('grant group permission', async () => {
  const wrapper = shallowRender();
  const group = {
    name: 'foo',
    permissions: []
  };
  wrapper.setState({
    groups: [group]
  });
  await wrapper.instance().handleToggleGroup(group, 'bar');
  expect(grantTemplatePermissionToGroup).toHaveBeenCalledWith({
    groupName: 'foo',
    templateId: '1',
    permission: 'bar'
  });
});

it('revoke user permission if granted', async () => {
  const wrapper = shallowRender();
  const user = {
    login: 'foo',
    name: 'foo',
    permissions: ['bar']
  };
  wrapper.setState({
    users: [user]
  });
  await wrapper.instance().handleToggleUser(user, 'bar');
  expect(revokeTemplatePermissionFromUser).toHaveBeenCalledWith({
    templateId: '1',
    login: 'foo',
    permission: 'bar'
  });
});

it('grant user permission', async () => {
  const wrapper = shallowRender();
  const user = {
    login: 'foo',
    name: 'foo',
    permissions: []
  };
  wrapper.setState({
    users: [user]
  });
  await wrapper.instance().handleToggleUser(user, 'bar');
  expect(grantTemplatePermissionToUser).toHaveBeenCalledWith({
    templateId: '1',
    login: 'foo',
    permission: 'bar'
  });
});

function shallowRender() {
  return shallow<Template>(
    <Template
      refresh={async () => {}}
      template={{
        id: '1',
        createdAt: '2020-01-01',
        name: 'test',
        defaultFor: [],
        permissions: []
      }}
      topQualifiers={[]}
    />
  );
}
