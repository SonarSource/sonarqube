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
import { GlobalNavUser } from '../GlobalNavUser';

const currentUser = { avatar: 'abcd1234', isLoggedIn: true, name: 'foo', email: 'foo@bar.baz' };
const organizations: T.Organization[] = [
  { key: 'myorg', name: 'MyOrg', projectVisibility: 'public' },
  { key: 'foo', name: 'Foo', projectVisibility: 'public' },
  { key: 'bar', name: 'bar', projectVisibility: 'public' }
];
const appState = { organizationsEnabled: true };

it('should render the right interface for anonymous user', () => {
  const currentUser = { isLoggedIn: false };
  const wrapper = shallow(
    <GlobalNavUser
      appState={appState}
      currentUser={currentUser}
      organizations={[]}
      router={{ push: jest.fn() }}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should render the right interface for logged in user', () => {
  const wrapper = shallow(
    <GlobalNavUser
      appState={appState}
      currentUser={currentUser}
      organizations={[]}
      router={{ push: jest.fn() }}
    />
  );
  wrapper.setState({ open: true });
  expect(wrapper.find('Dropdown')).toMatchSnapshot();
});

it('should render user organizations', () => {
  const wrapper = shallow(
    <GlobalNavUser
      appState={appState}
      currentUser={currentUser}
      organizations={organizations}
      router={{ push: jest.fn() }}
    />
  );
  wrapper.setState({ open: true });
  expect(wrapper.find('Dropdown')).toMatchSnapshot();
});

it('should not render user organizations when they are not activated', () => {
  const wrapper = shallow(
    <GlobalNavUser
      appState={{ organizationsEnabled: false }}
      currentUser={currentUser}
      organizations={organizations}
      router={{ push: jest.fn() }}
    />
  );
  wrapper.setState({ open: true });
  expect(wrapper.find('Dropdown')).toMatchSnapshot();
});
