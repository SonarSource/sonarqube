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
  mockOrganization,
  mockOrganizationWithAdminActions,
  mockOrganizationWithAlm
} from '../../../helpers/testMocks';
import MembersPageHeader, { Props } from '../MembersPageHeader';

it('should render correctly', () => {
  expect(shallowRender({ loading: true })).toMatchSnapshot();
});

it('should render for admin', () => {
  expect(
    shallowRender({ organization: mockOrganization({ actions: { admin: true } }) })
  ).toMatchSnapshot();
});

it('should render for Bitbucket bound organization', () => {
  const organization = mockOrganizationWithAlm(mockOrganizationWithAdminActions(), {
    key: 'bitbucket'
  });
  expect(shallowRender({ organization })).toMatchSnapshot();
});

it('should render for GitHub bound organization without sync', () => {
  const organization = mockOrganizationWithAlm(mockOrganizationWithAdminActions());
  expect(shallowRender({ organization })).toMatchSnapshot();
});

it('should render for personal GitHub bound organization without sync', () => {
  const organization = mockOrganizationWithAlm(mockOrganizationWithAdminActions(), {
    personal: true
  });
  expect(shallowRender({ organization })).toMatchSnapshot();
});

it('should render for bound organization with sync', () => {
  const organization = mockOrganizationWithAlm(mockOrganizationWithAdminActions(), {
    membersSync: true
  });
  const wrapper = shallowRender({ organization });
  expect(wrapper.find('Connect(SyncMemberForm)').exists()).toBe(true);
  expect(wrapper.find('AddMemberForm').exists()).toBe(false);
  expect(wrapper.find('Alert').exists()).toBe(false);
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <MembersPageHeader
      handleAddMember={jest.fn()}
      loading={false}
      members={[]}
      organization={mockOrganization()}
      refreshMembers={jest.fn()}
      {...props}
    />
  );
}
