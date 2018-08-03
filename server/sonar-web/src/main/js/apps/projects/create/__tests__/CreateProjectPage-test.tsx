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
import { Location } from 'history';
import { CreateProjectPage } from '../CreateProjectPage';
import { getIdentityProviders } from '../../../../api/users';
import { LoggedInUser } from '../../../../app/types';
import { click, waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/users', () => ({
  getIdentityProviders: jest.fn().mockResolvedValue({
    identityProviders: [
      {
        backgroundColor: 'blue',
        iconPath: 'icon/path',
        key: 'github',
        name: 'GitHub'
      }
    ]
  })
}));

const user: LoggedInUser = {
  externalProvider: 'github',
  isLoggedIn: true,
  login: 'foo',
  name: 'Foo'
};

beforeEach(() => {
  (getIdentityProviders as jest.Mock<any>).mockClear();
});

it('should render correctly', async () => {
  const wrapper = getWrapper();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render with Manual creation only', () => {
  expect(getWrapper({ currentUser: { ...user, externalProvider: 'microsoft' } })).toMatchSnapshot();
});

it('should switch tabs', async () => {
  const replace = jest.fn();
  const wrapper = getWrapper({ router: { replace } });
  replace.mockImplementation(location => {
    wrapper.setProps({ location }).update();
  });

  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('.js-manual'));
  expect(wrapper.find('Connect(ManualProjectCreate)').exists()).toBeTruthy();
  click(wrapper.find('.js-auto'));
  expect(wrapper.find('AutoProjectCreate').exists()).toBeTruthy();
});

function getWrapper(props = {}) {
  return shallow(
    <CreateProjectPage
      currentUser={user}
      location={{ pathname: 'foo', query: { manual: 'false' } } as Location}
      router={{ push: jest.fn(), replace: jest.fn() }}
      skipOnboardingAction={jest.fn()}
      {...props}
    />
  );
}
