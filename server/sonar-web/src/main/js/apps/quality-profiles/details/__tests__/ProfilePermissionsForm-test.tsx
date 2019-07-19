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
/* eslint-disable import/first */
jest.mock('../../../../api/quality-profiles', () => ({
  addUser: jest.fn(() => Promise.resolve()),
  addGroup: jest.fn(() => Promise.resolve())
}));

import { shallow } from 'enzyme';
import * as React from 'react';
import { submit } from 'sonar-ui-common/helpers/testUtils';
import ProfilePermissionsForm from '../ProfilePermissionsForm';

const addUser = require('../../../../api/quality-profiles').addUser as jest.Mock<any>;
const addGroup = require('../../../../api/quality-profiles').addGroup as jest.Mock<any>;

const profile = { language: 'js', name: 'Sonar way' };

it('adds user', async () => {
  const onUserAdd = jest.fn();
  const wrapper = shallow(
    <ProfilePermissionsForm
      onClose={jest.fn()}
      onGroupAdd={jest.fn()}
      onUserAdd={onUserAdd}
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
    login: 'luke',
    organization: 'org',
    qualityProfile: 'Sonar way'
  });

  await new Promise(setImmediate);
  expect(onUserAdd).toBeCalledWith({ login: 'luke' });
});

it('adds group', async () => {
  const onGroupAdd = jest.fn();
  const wrapper = shallow(
    <ProfilePermissionsForm
      onClose={jest.fn()}
      onGroupAdd={onGroupAdd}
      onUserAdd={jest.fn()}
      organization="org"
      profile={profile}
    />
  );
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({ selected: { name: 'lambda' } });
  expect(wrapper).toMatchSnapshot();

  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot();
  expect(addGroup).toBeCalledWith({
    group: 'lambda',
    language: 'js',
    organization: 'org',
    qualityProfile: 'Sonar way'
  });

  await new Promise(setImmediate);
  expect(onGroupAdd).toBeCalledWith({ name: 'lambda' });
});
