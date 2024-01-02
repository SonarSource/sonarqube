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
import { addGroup, addUser } from '../../../../api/quality-profiles';
import { mockGroup, mockUser } from '../../../../helpers/testMocks';
import { submit, waitAndUpdate } from '../../../../helpers/testUtils';
import { UserSelected } from '../../../../types/types';
import ProfilePermissionsForm from '../ProfilePermissionsForm';

jest.mock('../../../../api/quality-profiles', () => ({
  addUser: jest.fn().mockResolvedValue(null),
  addGroup: jest.fn().mockResolvedValue(null),
  searchGroups: jest.fn().mockResolvedValue({ groups: [] }),
  searchUsers: jest.fn().mockResolvedValue({ users: [] }),
}));

const PROFILE = { language: 'js', name: 'Sonar way' };

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender().setState({ submitting: true })).toMatchSnapshot('submitting');
});

it('correctly adds users', async () => {
  const onUserAdd = jest.fn();
  const wrapper = shallowRender({ onUserAdd });

  const user: UserSelected = { ...mockUser(), name: 'John doe', active: true, selected: true };
  wrapper.instance().handleValueChange(user);
  expect(wrapper.state().selected).toBe(user);

  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot();
  expect(addUser).toHaveBeenCalledWith(
    expect.objectContaining({
      language: PROFILE.language,
      qualityProfile: PROFILE.name,
      login: user.login,
    })
  );

  await waitAndUpdate(wrapper);
  expect(onUserAdd).toHaveBeenCalledWith(user);
});

it('correctly adds groups', async () => {
  const onGroupAdd = jest.fn();
  const wrapper = shallowRender({ onGroupAdd });

  const group = mockGroup();
  wrapper.instance().handleValueChange(group);
  expect(wrapper.state().selected).toBe(group);

  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot();
  expect(addGroup).toHaveBeenCalledWith(
    expect.objectContaining({
      language: PROFILE.language,
      qualityProfile: PROFILE.name,
      group: group.name,
    })
  );

  await waitAndUpdate(wrapper);
  expect(onGroupAdd).toHaveBeenCalledWith(group);
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
