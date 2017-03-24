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
import OrganizationMembers from '../OrganizationMembers';

const organization = { key: 'foo', name: 'Foo' };
const members = [
  { login: 'admin', name: 'Admin Istrator', avatar: '', groupCount: 3 },
  { login: 'john', name: 'John Doe', avatar: '7daf6c79d4802916d83f6266e24850af', groupCount: 1 }
];
const status = { total: members.length };
const fetchOrganizationMembers = jest.fn();
const fetchMoreOrganizationMembers = jest.fn();

it('should not render actions for non admin', () => {
  const wrapper = shallow(
    <OrganizationMembers
      organization={organization}
      members={members}
      status={status}
      fetchOrganizationMembers={fetchOrganizationMembers}
      fetchMoreOrganizationMembers={fetchMoreOrganizationMembers}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should render actions for admin', () => {
  const wrapper = shallow(
    <OrganizationMembers
      organization={{ ...organization, canAdmin: true }}
      members={members}
      status={{ ...status, loading: true }}
      fetchOrganizationMembers={fetchOrganizationMembers}
      fetchMoreOrganizationMembers={fetchMoreOrganizationMembers}
    />
  );
  expect(wrapper).toMatchSnapshot();
});
