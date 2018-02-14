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
import EmailAlreadyExists from '../EmailAlreadyExists';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/users', () => ({
  getIdentityProviders: () =>
    Promise.resolve({
      identityProviders: [
        {
          key: 'bitbucket',
          name: 'Bitbucket',
          iconPath: '/static/authbitbucket/bitbucket.svg',
          backgroundColor: '#205081'
        },
        {
          key: 'github',
          name: 'GitHub',
          iconPath: '/static/authgithub/github.svg',
          backgroundColor: '#444444'
        }
      ]
    })
}));

it('render', async () => {
  const query = {
    email: 'mail@example.com',
    login: 'foo',
    provider: 'github',
    existingLogin: 'bar',
    existingProvider: 'bitbucket'
  };
  const wrapper = shallow(<EmailAlreadyExists location={{ query }} />);
  (wrapper.instance() as EmailAlreadyExists).mounted = true;
  (wrapper.instance() as EmailAlreadyExists).fetchIdentityProviders();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});
