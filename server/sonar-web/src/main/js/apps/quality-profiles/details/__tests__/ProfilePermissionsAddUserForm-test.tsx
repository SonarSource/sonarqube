/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
jest.mock('../../../../api/quality-profiles', () => ({
  addUser: jest.fn(() => Promise.resolve())
}));

import * as React from 'react';
import { shallow } from 'enzyme';
import ProfilePermissionsAddUserForm from '../ProfilePermissionsAddUserForm';
import { submit } from '../../../../helpers/testUtils';

const addUser = require('../../../../api/quality-profiles').addUser as jest.Mock<any>;

const profile = { language: 'js', name: 'Sonar way' };

it('adds user', async () => {
  const onAdd = jest.fn();
  const wrapper = shallow(
    <ProfilePermissionsAddUserForm
      onAdd={onAdd}
      onClose={jest.fn()}
      organization="org"
      profile={profile}
    />
  );
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({ selected: { login: 'luke' } });
  expect(wrapper).toMatchSnapshot();

  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot();
  expect(addUser).toBeCalledWith({
    language: 'js',
    organization: 'org',
    profile: 'Sonar way',
    user: 'luke'
  });

  await new Promise(setImmediate);
  expect(onAdd).toBeCalledWith({ login: 'luke' });
});
