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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { searchGroups, searchUsers } from '../../../../api/quality-profiles';
import { click, waitAndUpdate } from '../../../../helpers/testUtils';
import ProfilePermissions from '../ProfilePermissions';

jest.mock('../../../../api/quality-profiles', () => ({
  searchUsers: jest.fn(() => Promise.resolve([])),
  searchGroups: jest.fn(() => Promise.resolve([])),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('renders', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({
    groups: [{ name: 'Lambda' }],
    loading: false,
    users: [{ login: 'luke', name: 'Luke Skywalker', selected: false }],
  });
  expect(wrapper).toMatchSnapshot();
});

it('should update correctly', () => {
  const wrapper = shallowRender();

  wrapper.setProps({ profile: { key: 'otherKey', name: 'new profile', language: 'js' } });

  expect(searchGroups).toHaveBeenCalledTimes(2);
  expect(searchUsers).toHaveBeenCalledTimes(2);
});

it('opens add users form', async () => {
  (searchUsers as jest.Mock).mockImplementationOnce(() =>
    Promise.resolve({ users: [{ login: 'luke', name: 'Luke Skywalker' }] })
  );
  const wrapper = shallowRender();
  expect(searchUsers).toHaveBeenCalled();
  await waitAndUpdate(wrapper);
  expect(wrapper.find('ProfilePermissionsForm').exists()).toBe(false);

  click(wrapper.find('Button'));
  expect(wrapper.find('ProfilePermissionsForm').exists()).toBe(true);

  wrapper.find('ProfilePermissionsForm').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('ProfilePermissionsForm').exists()).toBe(false);
});

it('removes user', () => {
  const wrapper = shallowRender();
  (wrapper.instance() as ProfilePermissions).mounted = true;

  const joda = { login: 'joda', name: 'Joda', selected: false };
  wrapper.setState({
    loading: false,
    users: [{ login: 'luke', name: 'Luke Skywalker', selected: false }, joda],
  });
  expect(wrapper.find('ProfilePermissionsUser')).toHaveLength(2);

  wrapper.find('ProfilePermissionsUser').first().prop<Function>('onDelete')(joda);
  wrapper.update();
  expect(wrapper.find('ProfilePermissionsUser')).toHaveLength(1);
});

it('removes group', () => {
  const wrapper = shallowRender();
  (wrapper.instance() as ProfilePermissions).mounted = true;

  const lambda = { name: 'Lambda' };
  wrapper.setState({ loading: false, groups: [{ name: 'Atlas' }, lambda] });
  expect(wrapper.find('ProfilePermissionsGroup')).toHaveLength(2);

  wrapper.find('ProfilePermissionsGroup').first().prop<Function>('onDelete')(lambda);
  wrapper.update();
  expect(wrapper.find('ProfilePermissionsGroup')).toHaveLength(1);
});

it('fetches users and groups on mount', () => {
  mount(<ProfilePermissions profile={{ key: 'sonar-way', name: 'Sonar way', language: 'js' }} />);
  expect(searchUsers).toHaveBeenCalledWith({
    language: 'js',
    qualityProfile: 'Sonar way',
    selected: 'selected',
  });
  expect(searchGroups).toHaveBeenCalledWith({
    language: 'js',
    qualityProfile: 'Sonar way',
    selected: 'selected',
  });
});

function shallowRender(overrides: Partial<{ key: string; name: string; language: string }> = {}) {
  const profile = { key: 'sonar-way', name: 'Sonar way', language: 'js', ...overrides };
  return shallow<ProfilePermissions>(<ProfilePermissions profile={profile} />);
}
