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
// @flow
import React from 'react';
import { mount } from 'enzyme';
import TokenStep from '../TokenStep';
import { change, click, doAsync, submit } from '../../../../helpers/testUtils';

jest.mock('../../../../api/user-tokens', () => ({
  generateToken: () => Promise.resolve({ token: 'abcd1234' }),
  revokeToken: () => Promise.resolve()
}));

it('generates token', () => {
  const wrapper = mount(
    <TokenStep
      finished={false}
      open={true}
      onContinue={jest.fn()}
      onOpen={jest.fn()}
      stepNumber={1}
    />
  );
  expect(wrapper).toMatchSnapshot();
  change(wrapper.find('input'), 'my token');
  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot(); // spinner
  return doAsync(() => expect(wrapper).toMatchSnapshot());
});

it('revokes token', () => {
  const wrapper = mount(
    <TokenStep
      finished={false}
      open={true}
      onContinue={jest.fn()}
      onOpen={jest.fn()}
      stepNumber={1}
    />
  );
  wrapper.setState({ token: 'abcd1234', tokenName: 'my token' });
  expect(wrapper).toMatchSnapshot();
  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot(); // spinner
  return doAsync(() => expect(wrapper).toMatchSnapshot());
});

it('continues', () => {
  const onContinue = jest.fn();
  const wrapper = mount(
    <TokenStep
      finished={false}
      open={true}
      onContinue={onContinue}
      onOpen={jest.fn()}
      stepNumber={1}
    />
  );
  wrapper.setState({ token: 'abcd1234', tokenName: 'my token' });
  click(wrapper.find('.js-continue'));
  expect(onContinue).toBeCalledWith('abcd1234');
});
