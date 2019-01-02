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
import { Location } from 'history';
import { OrganizationPage } from '../OrganizationPage';

const fetchOrganization = jest.fn().mockResolvedValue(undefined);

beforeEach(() => {
  fetchOrganization.mockClear();
});

it('smoke test', () => {
  const wrapper = getWrapper();
  expect(wrapper.type()).toBeNull();

  const organization = { actions: { admin: false }, key: 'foo', name: 'Foo', isDefault: false };
  wrapper.setProps({ organization });
  expect(wrapper).toMatchSnapshot();
});

it('not found', () => {
  const wrapper = getWrapper();
  wrapper.setState({ loading: false });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly update when the organization changes', () => {
  const wrapper = getWrapper();
  wrapper.setProps({ params: { organizationKey: 'bar' } });
  expect(fetchOrganization).toHaveBeenCalledTimes(2);
  expect(fetchOrganization.mock.calls).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <OrganizationPage
      currentUser={{ isLoggedIn: false }}
      fetchOrganization={fetchOrganization}
      location={{ pathname: 'foo' } as Location}
      params={{ organizationKey: 'foo' }}
      userOrganizations={[]}
      {...props}>
      <div>hello</div>
    </OrganizationPage>
  );
}
