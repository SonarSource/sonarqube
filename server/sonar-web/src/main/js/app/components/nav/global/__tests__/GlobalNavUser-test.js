/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import GlobalNavUser from '../GlobalNavUser';

const currentUser = { isLoggedIn: true, name: 'foo', email: 'foo@bar.baz' };
const organizations = [
  { key: 'myorg', name: 'MyOrg' },
  { key: 'foo', name: 'Foo' },
  { key: 'bar', name: 'bar' }
];
const appState = { organizationsEnabled: true };

it('should render the right interface for anonymous user', () => {
  const currentUser = { isLoggedIn: false };
  const wrapper = shallow(
    <GlobalNavUser
      appState={appState}
      currentUser={currentUser}
      fetchMyOrganizations={() => {}}
      organizations={[]}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should render the right interface for logged in user', () => {
  const wrapper = shallow(
    <GlobalNavUser
      appState={appState}
      currentUser={currentUser}
      fetchMyOrganizations={() => {}}
      organizations={[]}
    />
  );
  wrapper.setState({ open: true });
  expect(wrapper).toMatchSnapshot();
});

it('should render the users organizations', () => {
  const wrapper = shallow(
    <GlobalNavUser
      appState={appState}
      currentUser={currentUser}
      fetchMyOrganizations={() => {}}
      organizations={organizations}
    />
  );
  wrapper.setState({ open: true });
  expect(wrapper).toMatchSnapshot();
});

it('should not render the users organizations when they are not activated', () => {
  const wrapper = shallow(
    <GlobalNavUser
      appState={{ organizationsEnabled: false }}
      currentUser={currentUser}
      fetchMyOrganizations={() => {}}
      organizations={organizations}
    />
  );
  wrapper.setState({ open: true });
  expect(wrapper).toMatchSnapshot();
});

it('should update the component correctly when the user changes to anonymous', () => {
  const fetchMyOrganizations = jest.fn();
  const wrapper = shallow(
    <GlobalNavUser
      appState={appState}
      currentUser={currentUser}
      fetchMyOrganizations={fetchMyOrganizations}
      organizations={[]}
    />
  );
  wrapper.setState({ open: true });
  expect(wrapper).toMatchSnapshot();
  wrapper.setProps({ currentUser: { isLoggedIn: false } });
  expect(fetchMyOrganizations.mock.calls.length).toBe(0);
  expect(wrapper).toMatchSnapshot();
});

it('should lazyload the organizations when opening the dropdown', () => {
  const fetchMyOrganizations = jest.fn(() => Promise.resolve());
  const wrapper = shallow(
    <GlobalNavUser
      appState={appState}
      currentUser={currentUser}
      fetchMyOrganizations={fetchMyOrganizations}
      organizations={organizations}
    />
  );
  expect(fetchMyOrganizations.mock.calls.length).toBe(0);
  wrapper.instance().openDropdown();
  expect(fetchMyOrganizations.mock.calls.length).toBe(1);
  wrapper.instance().openDropdown();
  expect(fetchMyOrganizations.mock.calls.length).toBe(2);
});

it('should update the organizations when the user changes', () => {
  const fetchMyOrganizations = jest.fn(() => Promise.resolve());
  const wrapper = shallow(
    <GlobalNavUser
      appState={appState}
      currentUser={currentUser}
      fetchMyOrganizations={fetchMyOrganizations}
      organizations={organizations}
    />
  );
  wrapper.instance().openDropdown();
  expect(fetchMyOrganizations.mock.calls.length).toBe(1);
  wrapper.setProps({
    currentUser: { isLoggedIn: true, name: 'test', email: 'test@sonarsource.com' }
  });
  wrapper.instance().openDropdown();
  expect(fetchMyOrganizations.mock.calls.length).toBe(2);
});
