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
import { removeUser } from '../../../../api/quality-profiles';
import ProfilePermissionsUser from '../ProfilePermissionsUser';

jest.mock('../../../../api/quality-profiles', () => ({
  removeUser: jest.fn(() => Promise.resolve())
}));

const profile = { language: 'js', name: 'Sonar way' };
const user: T.UserSelected = { login: 'luke', name: 'Luke Skywalker', selected: true };

beforeEach(() => {
  jest.clearAllMocks();
});

it('renders', () => {
  expect(
    shallow(<ProfilePermissionsUser onDelete={jest.fn()} profile={profile} user={user} />)
  ).toMatchSnapshot();
});

it('removes user', async () => {
  const onDelete = jest.fn();
  const wrapper = shallow(
    <ProfilePermissionsUser onDelete={onDelete} organization="org" profile={profile} user={user} />
  );
  (wrapper.instance() as ProfilePermissionsUser).mounted = true;
  expect(wrapper.find('SimpleModal').exists()).toBeFalsy();

  click(wrapper.find('DeleteButton'));
  expect(wrapper.find('SimpleModal').exists()).toBeTruthy();

  wrapper.find('SimpleModal').prop<Function>('onSubmit')();
  expect(removeUser).toBeCalledWith({
    language: 'js',
    login: 'luke',
    organization: 'org',
    qualityProfile: 'Sonar way'
  });

  await new Promise(setImmediate);
  expect(onDelete).toBeCalledWith(user);
});
