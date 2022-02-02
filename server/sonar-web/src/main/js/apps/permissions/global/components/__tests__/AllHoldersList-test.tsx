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
import { mockPermissionGroup, mockPermissionUser } from '../../../../../helpers/mocks/permissions';
import { mockAppState } from '../../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../../types/component';
import { AllHoldersList } from '../AllHoldersList';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ filter: 'users' })).toMatchSnapshot('filter users');
  expect(shallowRender({ filter: 'groups' })).toMatchSnapshot('filter groups');
  expect(
    shallowRender({
      appState: mockAppState({
        qualifiers: [ComponentQualifier.Project, ComponentQualifier.Application]
      })
    })
  ).toMatchSnapshot('applications available');
  expect(
    shallowRender({
      appState: mockAppState({
        qualifiers: [ComponentQualifier.Project, ComponentQualifier.Portfolio]
      })
    })
  ).toMatchSnapshot('portfolios available');
});

it('should correctly toggle user permissions', () => {
  const grantPermissionToUser = jest.fn();
  const revokePermissionFromUser = jest.fn();
  const grantPermission = 'applicationcreator';
  const revokePermission = 'provisioning';
  const user = mockPermissionUser();
  const wrapper = shallowRender({ grantPermissionToUser, revokePermissionFromUser });
  const instance = wrapper.instance();

  instance.handleToggleUser(user, grantPermission);
  expect(grantPermissionToUser).toBeCalledWith(user.login, grantPermission);

  instance.handleToggleUser(user, revokePermission);
  expect(revokePermissionFromUser).toBeCalledWith(user.login, revokePermission);
});

it('should correctly toggle group permissions', () => {
  const grantPermissionToGroup = jest.fn();
  const revokePermissionFromGroup = jest.fn();
  const grantPermission = 'applicationcreator';
  const revokePermission = 'provisioning';
  const group = mockPermissionGroup();
  const wrapper = shallowRender({ grantPermissionToGroup, revokePermissionFromGroup });
  const instance = wrapper.instance();

  instance.handleToggleGroup(group, grantPermission);
  expect(grantPermissionToGroup).toBeCalledWith(group.name, grantPermission);

  instance.handleToggleGroup(group, revokePermission);
  expect(revokePermissionFromGroup).toBeCalledWith(group.name, revokePermission);
});

function shallowRender(props: Partial<AllHoldersList['props']> = {}) {
  return shallow<AllHoldersList>(
    <AllHoldersList
      appState={mockAppState({ qualifiers: [ComponentQualifier.Project] })}
      filter=""
      grantPermissionToGroup={jest.fn()}
      grantPermissionToUser={jest.fn()}
      groups={[mockPermissionGroup()]}
      loadHolders={jest.fn()}
      onLoadMore={jest.fn()}
      onFilter={jest.fn()}
      onSearch={jest.fn()}
      query=""
      revokePermissionFromGroup={jest.fn()}
      revokePermissionFromUser={jest.fn()}
      users={[mockPermissionUser()]}
      {...props}
    />
  );
}
