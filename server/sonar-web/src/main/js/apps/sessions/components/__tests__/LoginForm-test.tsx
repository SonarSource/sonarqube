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
import * as React from 'react';
import { shallow } from 'enzyme';
import LoginForm from '../LoginForm';
import { change, click, submit } from '../../../../helpers/testUtils';

const identityProvider = {
  backgroundColor: '#000',
  iconPath: '/some/path',
  key: 'foo',
  name: 'foo'
};

it('logs in with simple credentials', () => {
  const onSubmit = jest.fn();
  const wrapper = shallow(
    <LoginForm onSonarCloud={false} identityProviders={[]} onSubmit={onSubmit} returnTo="" />
  );
  expect(wrapper).toMatchSnapshot();

  change(wrapper.find('#login'), 'admin');
  change(wrapper.find('#password'), 'admin');
  submit(wrapper.find('form'));

  expect(onSubmit).toBeCalledWith('admin', 'admin');
});

it('logs in with identity provider', () => {
  const wrapper = shallow(
    <LoginForm
      onSonarCloud={false}
      identityProviders={[identityProvider]}
      onSubmit={jest.fn()}
      returnTo=""
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('expands more options', () => {
  const wrapper = shallow(
    <LoginForm
      onSonarCloud={false}
      identityProviders={[identityProvider]}
      onSubmit={jest.fn()}
      returnTo=""
    />
  );
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('.js-more-options'));
  expect(wrapper).toMatchSnapshot();
});
