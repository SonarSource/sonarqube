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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { deactivateUser } from '../../../../api/users';
import { mockLoggedInUser, mockRouter } from '../../../../helpers/testMocks';
import { UserDeleteAccountModal } from '../UserDeleteAccountModal';

jest.mock('../../../../api/users', () => ({
  deactivateUser: jest.fn()
}));

const organizationSafeToDelete = {
  key: 'luke',
  name: 'Luke Skywalker'
};

const organizationToTransferOrDelete = {
  key: 'luke-leia',
  name: 'Luke and Leia'
};

it('should render modal correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle submit', async () => {
  (deactivateUser as jest.Mock).mockResolvedValue(true);
  window.location.replace = jest.fn();

  const wrapper = shallowRender();
  const instance = wrapper.instance();

  instance.handleSubmit();
  await waitAndUpdate(wrapper);

  expect(deactivateUser).toBeCalled();
  expect(window.location.replace).toHaveBeenCalledWith('/account-deleted');
});

it('should validate user input', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  const { handleValidate } = instance;

  expect(handleValidate({ login: '' }).login).toBe('my_profile.delete_account.login.required');
  expect(handleValidate({ login: 'abc' }).login).toBe(
    'my_profile.delete_account.login.wrong_value'
  );
  expect(handleValidate({ login: 'luke' }).login).toBeUndefined();
});

function shallowRender(props: Partial<UserDeleteAccountModal['props']> = {}) {
  const user = mockLoggedInUser({ externalIdentity: 'luke' });

  return shallow<UserDeleteAccountModal>(
    <UserDeleteAccountModal
      doLogout={jest.fn().mockResolvedValue(true)}
      label="label"
      organizationsSafeToDelete={[organizationSafeToDelete]}
      organizationsToTransferOrDelete={[organizationToTransferOrDelete]}
      router={mockRouter()}
      toggleModal={jest.fn()}
      user={user}
      {...props}
    />
  );
}
