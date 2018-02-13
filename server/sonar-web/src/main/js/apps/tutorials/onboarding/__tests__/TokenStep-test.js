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
// @flow
import React from 'react';
import { mount } from 'enzyme';
import TokenStep from '../TokenStep';
import { change, click, submit, waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/user-tokens', () => ({
  getTokens: () => Promise.resolve([{ name: 'foo' }]),
  generateToken: () => Promise.resolve({ token: 'abcd1234' }),
  revokeToken: () => Promise.resolve()
}));

jest.mock('../../../../components/icons-components/ClearIcon');

const currentUser = { login: 'user' };

it('generates token', async () => {
  const wrapper = mount(
    <TokenStep
      currentUser={currentUser}
      finished={false}
      open={true}
      onContinue={jest.fn()}
      onOpen={jest.fn()}
      stepNumber={1}
    />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  change(wrapper.find('input'), 'my token');
  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot(); // spinner
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('revokes token', async () => {
  const wrapper = mount(
    <TokenStep
      currentUser={currentUser}
      finished={false}
      open={true}
      onContinue={jest.fn()}
      onOpen={jest.fn()}
      stepNumber={1}
    />
  );
  await new Promise(setImmediate);
  wrapper.setState({ token: 'abcd1234', tokenName: 'my token' });
  expect(wrapper).toMatchSnapshot();
  wrapper.find('DeleteButton').prop('onClick')();
  wrapper.update();
  expect(wrapper).toMatchSnapshot(); // spinner
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('continues', async () => {
  const onContinue = jest.fn();
  const wrapper = mount(
    <TokenStep
      currentUser={currentUser}
      finished={false}
      open={true}
      onContinue={onContinue}
      onOpen={jest.fn()}
      stepNumber={1}
    />
  );
  await new Promise(setImmediate);
  wrapper.setState({ token: 'abcd1234', tokenName: 'my token' });
  click(wrapper.find('.js-continue'));
  expect(onContinue).toBeCalledWith('abcd1234');
});

it('uses existing token', async () => {
  const onContinue = jest.fn();
  const wrapper = mount(
    <TokenStep
      currentUser={currentUser}
      finished={false}
      open={true}
      onContinue={onContinue}
      onOpen={jest.fn()}
      stepNumber={1}
    />
  );
  await new Promise(setImmediate);
  wrapper.setState({ existingToken: 'abcd1234', selection: 'use-existing' });
  click(wrapper.find('.js-continue'));
  expect(onContinue).toBeCalledWith('abcd1234');
});
