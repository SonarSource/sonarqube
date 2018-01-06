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
import React from 'react';
import { shallow } from 'enzyme';
import { OrganizationPage } from '../OrganizationPage';

const fetchOrganization = () => Promise.resolve();

it('smoke test', () => {
  const wrapper = shallow(
    <OrganizationPage fetchOrganization={fetchOrganization} params={{ organizationKey: 'foo' }}>
      <div>hello</div>
    </OrganizationPage>
  );
  expect(wrapper.type()).toBeNull();

  const organization = { key: 'foo', name: 'Foo', isDefault: false, canAdmin: false };
  wrapper.setProps({ organization });
  expect(wrapper).toMatchSnapshot();
});

it('not found', () => {
  const wrapper = shallow(
    <OrganizationPage fetchOrganization={fetchOrganization} params={{ organizationKey: 'foo' }}>
      <div>hello</div>
    </OrganizationPage>
  );
  wrapper.setState({ loading: false });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly update when the organization changes', () => {
  const fetchOrganization = jest.fn(() => Promise.resolve());
  const wrapper = shallow(
    <OrganizationPage params={{ organizationKey: 'foo' }} fetchOrganization={fetchOrganization}>
      <div>hello</div>
    </OrganizationPage>
  );
  wrapper.setProps({ params: { organizationKey: 'bar' } });
  expect(fetchOrganization).toHaveBeenCalledTimes(2);
  expect(fetchOrganization.mock.calls).toMatchSnapshot();
});
