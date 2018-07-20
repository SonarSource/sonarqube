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
import AutoProjectCreate from '../AutoProjectCreate';
import { getIdentityProviders } from '../../../../api/users';
import { getRepositories } from '../../../../api/alm-integration';
import { LoggedInUser } from '../../../../app/types';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/users', () => ({
  getIdentityProviders: jest.fn().mockResolvedValue({
    identityProviders: [
      {
        backgroundColor: 'blue',
        iconPath: 'icon/path',
        key: 'foo',
        name: 'Foo Provider'
      }
    ]
  })
}));

jest.mock('../../../../api/alm-integration', () => ({
  getRepositories: jest.fn().mockResolvedValue({
    installation: {
      installationUrl: 'https://alm.foo.com/install',
      enabled: false
    }
  })
}));

const user: LoggedInUser = { isLoggedIn: true, login: 'foo', name: 'Foo', externalProvider: 'foo' };

beforeEach(() => {
  (getIdentityProviders as jest.Mock<any>).mockClear();
  (getRepositories as jest.Mock<any>).mockClear();
});

it('should display the provider app install button', async () => {
  const wrapper = getWrapper();
  expect(wrapper).toMatchSnapshot();
  expect(getIdentityProviders).toHaveBeenCalled();
  expect(getRepositories).toHaveBeenCalled();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(<AutoProjectCreate currentUser={user} {...props} />);
}
