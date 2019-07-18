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
import {
  mockCurrentUser,
  mockLoggedInUser,
  mockOrganizationWithAlm
} from '../../../../helpers/testMocks';
import OrganizationNavigationHeader, { Props } from '../OrganizationNavigationHeader';

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('renders with alm integration', () => {
  expect(
    shallowRender({ organization: mockOrganizationWithAlm({ projectVisibility: 'public' }) })
  ).toMatchSnapshot();
});

it('renders for external user w/o alm integration', () => {
  expect(
    shallowRender({ currentUser: mockLoggedInUser({ externalProvider: 'github' }) })
  ).toMatchSnapshot();
});

it('renders with the organization tooltip for an admin user of an organization', () => {
  expect(
    shallowRender({
      currentUser: mockLoggedInUser({
        externalProvider: 'github'
      }),
      organization: {
        actions: { admin: true },
        key: 'org1',
        name: 'org1',
        projectVisibility: 'public'
      }
    }).find('Tooltip')
  ).toMatchSnapshot();
});

it('renders without the organization tooltip for a non-admin user of an organization', () => {
  expect(
    shallowRender({
      currentUser: mockLoggedInUser({
        externalProvider: 'github'
      }),
      organization: {
        actions: { admin: false },
        key: 'org1',
        name: 'org1',
        projectVisibility: 'public'
      }
    }).find('Tooltip')
  ).toMatchSnapshot();
});

it('renders dropdown', () => {
  const organizations: T.Organization[] = [
    { actions: { admin: true }, key: 'org1', name: 'org1', projectVisibility: 'public' },
    { actions: { admin: false }, key: 'org2', name: 'org2', projectVisibility: 'public' }
  ];
  const wrapper = shallowRender({
    organizations
  });
  expect(wrapper.find('Dropdown')).toMatchSnapshot();
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <OrganizationNavigationHeader
      currentUser={mockCurrentUser()}
      organization={{
        key: 'foo',
        name: 'Foo',
        projectVisibility: 'public'
      }}
      organizations={[]}
      {...props}
    />
  );
}
