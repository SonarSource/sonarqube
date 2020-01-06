/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { getIdentityProviders } from '../../../../api/users';
import { mockLocation } from '../../../../helpers/testMocks';
import { LoginContainer } from '../LoginContainer';

jest.mock('../../../../api/users', () => {
  const { mockIdentityProvider } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getIdentityProviders: jest
      .fn()
      .mockResolvedValue({ identityProviders: [mockIdentityProvider()] })
  };
});

beforeEach(jest.clearAllMocks);

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
  expect(getIdentityProviders).toBeCalled();
});

it('should not provide any options if no IdPs are present', async () => {
  (getIdentityProviders as jest.Mock).mockResolvedValue({});
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.type()).toBeNull();
  expect(getIdentityProviders).toBeCalled();
});

it('should handle submission', () => {
  const doLogin = jest.fn().mockResolvedValue(null);
  const wrapper = shallowRender({ doLogin });
  wrapper.instance().handleSubmit('user', 'pass');
  expect(doLogin).toBeCalledWith('user', 'pass');
});

function shallowRender(props: Partial<LoginContainer['props']> = {}) {
  return shallow<LoginContainer>(
    <LoginContainer doLogin={jest.fn()} location={mockLocation()} {...props} />
  );
}
