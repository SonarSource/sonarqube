/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { generateToken, getTokens, revokeToken } from '../../../../api/user-tokens';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { mockEvent, waitAndUpdate } from '../../../../helpers/testUtils';
import { getUniqueTokenName } from '../../utils';
import EditTokenModal from '../EditTokenModal';

jest.mock('../../../../api/user-tokens', () => ({
  generateToken: jest.fn().mockResolvedValue({
    name: 'baz',
    createdAt: '2019-01-21T08:06:00+0100',
    login: 'luke',
    token: 'token_value'
  }),
  getTokens: jest.fn().mockResolvedValue([
    {
      name: 'foo',
      createdAt: '2019-01-15T15:06:33+0100',
      lastConnectionDate: '2019-01-18T15:06:33+0100'
    },
    { name: 'bar', createdAt: '2019-01-18T15:06:33+0100' }
  ]),
  revokeToken: jest.fn().mockResolvedValue(Promise.resolve())
}));

jest.mock('../../utils', () => ({
  getUniqueTokenName: jest.fn().mockReturnValue('lightsaber-9000')
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should get tokens and unique name', async () => {
  const wrapper = shallowRender();
  const { getTokensAndName } = wrapper.instance();

  getTokensAndName();
  await waitAndUpdate(wrapper);

  expect(getTokens).toHaveBeenCalled();
  expect(getUniqueTokenName).toHaveBeenCalled();
  expect(wrapper.state('tokenName')).toBe('lightsaber-9000');
});

it('should get a new token', async () => {
  const wrapper = shallowRender();
  const { getNewToken } = wrapper.instance();

  getNewToken();
  await waitAndUpdate(wrapper);

  expect(generateToken).toHaveBeenCalled();
  expect(wrapper.state('token')).toBe('token_value');
});

it('should handle token revocation', async () => {
  const wrapper = shallowRender();
  const { getTokensAndName, handleTokenRevoke } = wrapper.instance();

  getTokensAndName();
  await waitAndUpdate(wrapper);
  handleTokenRevoke();
  await waitAndUpdate(wrapper);

  expect(revokeToken).toHaveBeenCalled();
  expect(wrapper.state('token')).toBe('');
  expect(wrapper.state('tokenName')).toBe('');
});

it('should handle change on user input', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  instance.handleChange(mockEvent({ target: { value: 'my-token' } }));
  expect(wrapper.state('tokenName')).toBe('my-token');
});

function shallowRender(props: Partial<EditTokenModal['props']> = {}) {
  return shallow<EditTokenModal>(
    <EditTokenModal
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      onClose={jest.fn()}
      {...props}
    />
  );
}
