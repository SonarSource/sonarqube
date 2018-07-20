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
import { CreateProjectOnboarding } from '../CreateProjectOnboarding';
import { LoggedInUser } from '../../../../app/types';
import { click } from '../../../../helpers/testUtils';

const user: LoggedInUser = {
  externalProvider: 'github',
  isLoggedIn: true,
  login: 'foo',
  name: 'Foo'
};

it('should render correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should render with Manual creation only', () => {
  expect(getWrapper({ currentUser: { ...user, externalProvider: 'vsts' } })).toMatchSnapshot();
});

it('should switch tabs', () => {
  const wrapper = getWrapper();
  click(wrapper.find('.js-manual'));
  expect(wrapper.find('Connect(ManualProjectCreate)').exists()).toBeTruthy();
  click(wrapper.find('.js-auto'));
  expect(wrapper.find('AutoProjectCreate').exists()).toBeTruthy();
});

function getWrapper(props = {}) {
  return shallow(
    <CreateProjectOnboarding
      currentUser={user}
      onFinishOnboarding={jest.fn()}
      skipOnboarding={jest.fn()}
      {...props}
    />
  );
}
