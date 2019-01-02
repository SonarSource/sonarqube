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
import * as React from 'react';
import { shallow } from 'enzyme';
import OrganizationNavigationHeader from '../OrganizationNavigationHeader';

it('renders', () => {
  expect(
    shallow(
      <OrganizationNavigationHeader
        organization={{ key: 'foo', name: 'Foo', projectVisibility: 'public' }}
        organizations={[]}
      />
    )
  ).toMatchSnapshot();
});

it('renders with alm integration', () => {
  expect(
    shallow(
      <OrganizationNavigationHeader
        organization={{
          alm: { key: 'github', url: 'https://github.com/foo' },
          key: 'foo',
          name: 'Foo',
          projectVisibility: 'public'
        }}
        organizations={[]}
      />
    )
  ).toMatchSnapshot();
});

it('renders dropdown', () => {
  const organizations: T.Organization[] = [
    { actions: { admin: true }, key: 'org1', name: 'org1', projectVisibility: 'public' },
    { actions: { admin: false }, key: 'org2', name: 'org2', projectVisibility: 'public' }
  ];
  const wrapper = shallow(
    <OrganizationNavigationHeader
      organization={{
        key: 'foo',
        name: 'Foo',
        projectVisibility: 'public'
      }}
      organizations={organizations}
    />
  );
  expect(wrapper.find('Dropdown')).toMatchSnapshot();
});
