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
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { createUser, updateUser } from '../../../../api/users';
import { mockUser } from '../../../../helpers/testMocks';
import UserForm from '../UserForm';

jest.mock('../../../../api/users', () => ({
  createUser: jest.fn().mockResolvedValue({}),
  updateUser: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender().dive()).toMatchSnapshot();
  expect(shallowRender({ user: undefined }).dive()).toMatchSnapshot();
});

it('should correctly show errors', async () => {
  const response = new Response(null, { status: 400 });
  response.json = jest.fn().mockRejectedValue(undefined);

  (updateUser as jest.Mock).mockRejectedValue(response);

  const wrapper = shallowRender();
  submit(wrapper.dive().find('form'));
  await waitAndUpdate(wrapper);

  expect(
    wrapper
      .dive()
      .find(Alert)
      .children()
      .text()
  ).toMatch('default_error_message');
});

it('should correctly disable name and email fields for non-local users', () => {
  const wrapper = shallowRender({ user: mockUser({ local: false }) }).dive();
  expect(wrapper.find('#create-user-name').prop('disabled')).toBe(true);
  expect(wrapper.find('#create-user-email').prop('disabled')).toBe(true);
  expect(wrapper.find('Alert').exists()).toBe(true);
  expect(
    wrapper
      .find(Alert)
      .children()
      .text()
  ).toMatch('users.cannot_update_delegated_user');
});

it('should correctly create a new user', () => {
  const email = 'foo@bar.ch';
  const login = 'foo';
  const name = 'Foo';
  const password = 'bar';
  const scmAccounts = ['gh', 'gh', 'bitbucket'];
  const wrapper = shallowRender({ user: undefined });

  wrapper.setState({ email, login, name, password, scmAccounts });

  submit(wrapper.dive().find('form'));

  expect(createUser).toBeCalledWith({
    email,
    login,
    name,
    password,
    scmAccount: ['gh', 'bitbucket']
  });
});

it('should correctly update a local user', () => {
  const email = 'foo@bar.ch';
  const login = 'foo';
  const name = 'Foo';
  const scmAccounts = ['gh', 'gh', 'bitbucket'];
  const wrapper = shallowRender({ user: mockUser({ email, login, name, scmAccounts }) }).dive();

  submit(wrapper.find('form'));

  expect(updateUser).toBeCalledWith({
    email,
    login,
    name,
    scmAccount: ['gh', 'bitbucket']
  });
});

it('should correctly update a non-local user', () => {
  const email = 'foo@bar.ch';
  const login = 'foo';
  const name = 'Foo';
  const scmAccounts = ['gh', 'bitbucket'];
  const wrapper = shallowRender({
    user: mockUser({ email, local: false, login, name, scmAccounts })
  }).dive();

  submit(wrapper.find('form'));

  expect(updateUser).toBeCalledWith(
    expect.not.objectContaining({
      email,
      name
    })
  );
});

function shallowRender(props: Partial<UserForm['props']> = {}) {
  return shallow(
    <UserForm onClose={jest.fn()} onUpdateUsers={jest.fn()} user={mockUser()} {...props} />
  );
}
