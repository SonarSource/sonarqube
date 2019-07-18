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
import { change, click, submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import TokenStep from '../TokenStep';

jest.mock('../../../../api/user-tokens', () => ({
  getTokens: () => Promise.resolve([{ name: 'foo' }]),
  generateToken: () => Promise.resolve({ token: 'abcd1234' }),
  revokeToken: () => Promise.resolve()
}));

const currentUser: Pick<T.LoggedInUser, 'login'> = { login: 'user' };

it('generates token', async () => {
  const wrapper = shallow(
    <TokenStep
      currentUser={currentUser}
      finished={false}
      onContinue={jest.fn()}
      onOpen={jest.fn()}
      open={true}
      stepNumber={1}
    />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper.dive()).toMatchSnapshot();
  change(wrapper.dive().find('input'), 'my token');
  submit(wrapper.dive().find('form'));
  expect(wrapper.dive()).toMatchSnapshot(); // spinner
  await waitAndUpdate(wrapper);
  expect(wrapper.dive()).toMatchSnapshot();
});

it('revokes token', async () => {
  const wrapper = shallow(
    <TokenStep
      currentUser={currentUser}
      finished={false}
      onContinue={jest.fn()}
      onOpen={jest.fn()}
      open={true}
      stepNumber={1}
    />
  );
  await new Promise(setImmediate);
  wrapper.setState({ token: 'abcd1234', tokenName: 'my token' });
  expect(wrapper.dive()).toMatchSnapshot();
  (wrapper
    .dive()
    .find('DeleteButton')
    .prop('onClick') as Function)();
  wrapper.update();
  expect(wrapper.dive()).toMatchSnapshot(); // spinner
  await waitAndUpdate(wrapper);
  expect(wrapper.dive()).toMatchSnapshot();
});

it('continues', async () => {
  const onContinue = jest.fn();
  const wrapper = shallow(
    <TokenStep
      currentUser={currentUser}
      finished={false}
      onContinue={onContinue}
      onOpen={jest.fn()}
      open={true}
      stepNumber={1}
    />
  );
  await new Promise(setImmediate);
  wrapper.setState({ token: 'abcd1234', tokenName: 'my token' });
  click(wrapper.dive().find('[className="js-continue"]'));
  expect(onContinue).toBeCalledWith('abcd1234');
});

it('uses existing token', async () => {
  const onContinue = jest.fn();
  const wrapper = shallow(
    <TokenStep
      currentUser={currentUser}
      finished={false}
      onContinue={onContinue}
      onOpen={jest.fn()}
      open={true}
      stepNumber={1}
    />
  );
  await new Promise(setImmediate);
  wrapper.setState({ existingToken: 'abcd1234', selection: 'use-existing' });
  click(wrapper.dive().find('[className="js-continue"]'));
  expect(onContinue).toBeCalledWith('abcd1234');
});
