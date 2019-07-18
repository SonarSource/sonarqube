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
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getOrganizationsThatPreventDeletion } from '../../../../api/organizations';
import { mockLoggedInUser, mockOrganization } from '../../../../helpers/testMocks';
import { UserDeleteAccount } from '../UserDeleteAccount';

jest.mock('../../../../api/organizations', () => ({
  getOrganizationsThatPreventDeletion: jest.fn().mockResolvedValue({ organizations: [] })
}));

beforeEach(() => {
  jest.clearAllMocks();
});

const organizationToTransferOrDelete = {
  key: 'luke-leia',
  name: 'Luke and Leia'
};

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('Button'));
  expect(wrapper).toMatchSnapshot();
});

it('should get some organizations', async () => {
  (getOrganizationsThatPreventDeletion as jest.Mock).mockResolvedValue({
    organizations: [organizationToTransferOrDelete]
  });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state('loading')).toBeFalsy();
  expect(wrapper.state('organizationsToTransferOrDelete')).toEqual([
    organizationToTransferOrDelete
  ]);
  expect(getOrganizationsThatPreventDeletion).toBeCalled();
  expect(wrapper.find('Button').prop('disabled')).toBe(true);
});

it('should toggle modal', () => {
  const wrapper = shallowRender();
  wrapper.setState({ loading: false });
  expect(wrapper.find('Connect(withRouter(UserDeleteAccountModal))').exists()).toBe(false);
  click(wrapper.find('Button'));
  expect(wrapper.find('Connect(withRouter(UserDeleteAccountModal))').exists()).toBe(true);
});

function shallowRender(props: Partial<UserDeleteAccount['props']> = {}) {
  const user = mockLoggedInUser({ externalIdentity: 'luke' });

  const userOrganizations = [
    mockOrganization({ key: 'luke-leia', name: 'Luke and Leia' }),
    mockOrganization({ key: 'luke', name: 'Luke Skywalker' })
  ];

  return shallow<UserDeleteAccount>(
    <UserDeleteAccount user={user} userOrganizations={userOrganizations} {...props} />
  );
}
