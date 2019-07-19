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

import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import ProfilePermissions from '../ProfilePermissions';

jest.mock('../../../../api/quality-profiles', () => ({
  searchUsers: jest.fn(() => Promise.resolve([])),
  searchGroups: jest.fn(() => Promise.resolve([]))
}));

const searchUsers = require('../../../../api/quality-profiles').searchUsers as jest.Mock<any>;
const searchGroups = require('../../../../api/quality-profiles').searchGroups as jest.Mock<any>;

const profile = { key: 'sonar-way', name: 'Sonar way', language: 'js' };

beforeEach(() => {
  searchUsers.mockClear();
  searchGroups.mockClear();
});

it('renders', () => {
  const wrapper = shallow(<ProfilePermissions profile={profile} />);
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({
    groups: [{ name: 'Lambda' }],
    loading: false,
    users: [{ login: 'luke', name: 'Luke Skywalker' }]
  });
  expect(wrapper).toMatchSnapshot();
});

it('opens add users form', async () => {
  searchUsers.mockImplementationOnce(() =>
    Promise.resolve({ users: [{ login: 'luke', name: 'Luke Skywalker' }] })
  );
  const wrapper = shallow(<ProfilePermissions profile={profile} />);
  expect(searchUsers).toHaveBeenCalled();
  await waitAndUpdate(wrapper);
  expect(wrapper.find('ProfilePermissionsForm').exists()).toBeFalsy();

  click(wrapper.find('Button'));
  expect(wrapper.find('ProfilePermissionsForm').exists()).toBeTruthy();

  wrapper.find('ProfilePermissionsForm').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('ProfilePermissionsForm').exists()).toBeFalsy();
});

it('removes user', () => {
  const wrapper = shallow(<ProfilePermissions profile={profile} />);
  (wrapper.instance() as ProfilePermissions).mounted = true;

  const joda = { login: 'joda', name: 'Joda' };
  wrapper.setState({ loading: false, users: [{ login: 'luke', name: 'Luke Skywalker' }, joda] });
  expect(wrapper.find('ProfilePermissionsUser')).toHaveLength(2);

  wrapper
    .find('ProfilePermissionsUser')
    .first()
    .prop<Function>('onDelete')(joda);
  wrapper.update();
  expect(wrapper.find('ProfilePermissionsUser')).toHaveLength(1);
});

it('removes group', () => {
  const wrapper = shallow(<ProfilePermissions profile={profile} />);
  (wrapper.instance() as ProfilePermissions).mounted = true;

  const lambda = { name: 'Lambda' };
  wrapper.setState({ loading: false, groups: [{ name: 'Atlas' }, lambda] });
  expect(wrapper.find('ProfilePermissionsGroup')).toHaveLength(2);

  wrapper
    .find('ProfilePermissionsGroup')
    .first()
    .prop<Function>('onDelete')(lambda);
  wrapper.update();
  expect(wrapper.find('ProfilePermissionsGroup')).toHaveLength(1);
});

it('fetches users and groups on mount', () => {
  mount(<ProfilePermissions organization="org" profile={profile} />);
  expect(searchUsers).toBeCalledWith({
    language: 'js',
    organization: 'org',
    qualityProfile: 'Sonar way',
    selected: 'selected'
  });
  expect(searchGroups).toBeCalledWith({
    language: 'js',
    organization: 'org',
    qualityProfile: 'Sonar way',
    selected: 'selected'
  });
});
