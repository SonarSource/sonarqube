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
import * as React from 'react';
import { shallow } from 'enzyme';
import GlobalNavUser from '../GlobalNavUser';
import { Visibility } from '../../../../types';

const currentUser = { avatar: 'abcd1234', isLoggedIn: true, name: 'foo', email: 'foo@bar.baz' };
const organizations = [
  { key: 'myorg', name: 'MyOrg', projectVisibility: Visibility.Public },
  { key: 'foo', name: 'Foo', projectVisibility: Visibility.Public },
  { key: 'bar', name: 'bar', projectVisibility: Visibility.Public }
];
const appState = { organizationsEnabled: true };

it('should render the right interface for anonymous user', () => {
  const currentUser = { isLoggedIn: false };
  const wrapper = shallow(
    <GlobalNavUser appState={appState} currentUser={currentUser} organizations={[]} />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should render the right interface for logged in user', () => {
  const wrapper = shallow(
    <GlobalNavUser appState={appState} currentUser={currentUser} organizations={[]} />
  );
  wrapper.setState({ open: true });
  expect(wrapper.find('Dropdown').dive()).toMatchSnapshot();
});

it('should render user organizations', () => {
  const wrapper = shallow(
    <GlobalNavUser appState={appState} currentUser={currentUser} organizations={organizations} />
  );
  wrapper.setState({ open: true });
  expect(wrapper.find('Dropdown').dive()).toMatchSnapshot();
});

it('should not render user organizations when they are not activated', () => {
  const wrapper = shallow(
    <GlobalNavUser
      appState={{ organizationsEnabled: false }}
      currentUser={currentUser}
      organizations={organizations}
    />
  );
  wrapper.setState({ open: true });
  expect(wrapper.find('Dropdown').dive()).toMatchSnapshot();
});
