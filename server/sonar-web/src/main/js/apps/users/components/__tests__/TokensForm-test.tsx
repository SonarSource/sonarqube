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
import TokensForm from '../TokensForm';
import { change, submit, waitAndUpdate } from '../../../../helpers/testUtils';
import { generateToken, getTokens } from '../../../../api/user-tokens';

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
  ])
}));

beforeEach(() => {
  (generateToken as jest.Mock).mockClear();
  (getTokens as jest.Mock).mockClear();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(getTokens).toHaveBeenCalledWith('luke');

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should create new tokens', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  expect(wrapper.find('TokensFormItem')).toHaveLength(2);
  change(wrapper.find('input'), 'baz');
  submit(wrapper.find('form'));

  await waitAndUpdate(wrapper);
  expect(generateToken).toHaveBeenCalledWith({ name: 'baz', login: 'luke' });
  expect(wrapper.find('TokensFormItem')).toHaveLength(3);
});

it('should revoke tokens', async () => {
  const updateTokensCount = jest.fn();
  const wrapper = shallowRender({ updateTokensCount });

  await waitAndUpdate(wrapper);
  expect(wrapper.find('TokensFormItem')).toHaveLength(2);
  wrapper.instance().handleRevokeToken({ createdAt: '2019-01-15T15:06:33+0100', name: 'foo' });
  expect(updateTokensCount).toHaveBeenCalledWith('luke', 1);
  expect(wrapper.find('TokensFormItem')).toHaveLength(1);
});

function shallowRender(props: Partial<TokensForm['props']> = {}) {
  return shallow<TokensForm>(<TokensForm login="luke" updateTokensCount={jest.fn()} {...props} />);
}
