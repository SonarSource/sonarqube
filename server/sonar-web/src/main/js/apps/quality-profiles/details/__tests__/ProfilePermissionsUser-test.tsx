/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
/* eslint-disable import/first, import/order */
jest.mock('../../../../api/quality-profiles', () => ({
  removeUser: jest.fn(() => Promise.resolve())
}));

import * as React from 'react';
import { shallow } from 'enzyme';
import ProfilePermissionsUser from '../ProfilePermissionsUser';

const removeUser = require('../../../../api/quality-profiles').removeUser as jest.Mock<any>;

const profile = { language: 'js', name: 'Sonar way' };
const user = { login: 'luke', name: 'Luke Skywalker' };

beforeEach(() => {
  removeUser.mockClear();
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

  wrapper.find('DeleteButton').prop<Function>('onClick')();
  wrapper.update();
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
