/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { addGroup, addUser, searchGroups, searchUsers } from '../../../../api/quality-profiles';
import { mockGroup, mockUser } from '../../../../helpers/testMocks';
import ProfilePermissionsForm from '../ProfilePermissionsForm';
import ProfilePermissionsFormSelect from '../ProfilePermissionsFormSelect';

jest.mock('../../../../api/quality-profiles', () => ({
  addUser: jest.fn().mockResolvedValue(null),
  addGroup: jest.fn().mockResolvedValue(null),
  searchGroups: jest.fn().mockResolvedValue({ groups: [] }),
  searchUsers: jest.fn().mockResolvedValue({ users: [] })
}));

const PROFILE = { language: 'js', name: 'Sonar way' };

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender().setState({ submitting: true })).toMatchSnapshot('submitting');
});

it('correctly adds users', async () => {
  const onUserAdd = jest.fn();
  const wrapper = shallowRender({ onUserAdd });

  const user: T.UserSelected = { ...mockUser(), name: 'John doe', active: true, selected: true };
  wrapper.instance().handleValueChange(user);
  expect(wrapper.find(ProfilePermissionsFormSelect).prop('selected')).toBe(user);

  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot();
  expect(addUser).toBeCalledWith(
    expect.objectContaining({
      language: PROFILE.language,
      qualityProfile: PROFILE.name,
      login: user.login
    })
  );

  await waitAndUpdate(wrapper);
  expect(onUserAdd).toBeCalledWith(user);
});

it('correctly adds groups', async () => {
  const onGroupAdd = jest.fn();
  const wrapper = shallowRender({ onGroupAdd });

  const group = mockGroup();
  wrapper.instance().handleValueChange(group);
  expect(wrapper.find(ProfilePermissionsFormSelect).prop('selected')).toBe(group);

  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot();
  expect(addGroup).toBeCalledWith(
    expect.objectContaining({
      language: PROFILE.language,
      qualityProfile: PROFILE.name,
      group: group.name
    })
  );

  await waitAndUpdate(wrapper);
  expect(onGroupAdd).toBeCalledWith(group);
});

it('correctly handles search', () => {
  const wrapper = shallowRender();
  wrapper.instance().handleSearch('foo');

  const parameters = {
    language: PROFILE.language,
    q: 'foo',
    qualityProfile: PROFILE.name,
    selected: 'deselected'
  };

  expect(searchUsers).toBeCalledWith(parameters);
  expect(searchGroups).toBeCalledWith(parameters);
});

function shallowRender(props: Partial<ProfilePermissionsForm['props']> = {}) {
  return shallow<ProfilePermissionsForm>(
    <ProfilePermissionsForm
      onClose={jest.fn()}
      onGroupAdd={jest.fn()}
      onUserAdd={jest.fn()}
      profile={PROFILE}
      {...props}
    />
  );
}
