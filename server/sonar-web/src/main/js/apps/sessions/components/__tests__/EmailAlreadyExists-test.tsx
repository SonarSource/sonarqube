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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import EmailAlreadyExists from '../EmailAlreadyExists';

jest.mock('../../../../api/users', () => ({
  getIdentityProviders: () =>
    Promise.resolve({
      identityProviders: [
        {
          key: 'bitbucket',
          name: 'Bitbucket',
          iconPath: '/static/authbitbucket/bitbucket.svg',
          backgroundColor: '#0052cc'
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

jest.mock('sonar-ui-common/helpers/cookies', () => ({
  getCookie: jest
    .fn()
    .mockReturnValue(
      '%7B%22email%22%3A%22mail%40example.com%22%2C%22login%22%3A%22foo%22%2C%22provider%22%3A%22github%22%2C%22existingLogin%22%3A%22bar%22%2C%22existingProvider%22%3A%22bitbucket%22%7D'
    )
}));

it('render', async () => {
  const wrapper = shallow(<EmailAlreadyExists />);
  (wrapper.instance() as EmailAlreadyExists).mounted = true;
  (wrapper.instance() as EmailAlreadyExists).fetchIdentityProviders();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});
