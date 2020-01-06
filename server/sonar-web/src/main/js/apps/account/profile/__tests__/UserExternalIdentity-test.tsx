/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import UserExternalIdentity, { UserExternalIdentityProps } from '../UserExternalIdentity';

jest.mock('../../../../api/users', () => ({
  getIdentityProviders: jest.fn().mockResolvedValue({
    identityProviders: [
      {
        backgroundColor: '#444444',
        iconPath: '/images/github.svg',
        key: 'github',
        name: 'GitHub'
      }
    ]
  })
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render a fallback when idp is not listed', async () => {
  const wrapper = shallowRender({ externalProvider: 'ggithub' });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(userOverrides?: Partial<UserExternalIdentityProps['user']>) {
  return shallow(
    <UserExternalIdentity
      user={{
        ...mockLoggedInUser({
          email: 'john@doe.com',
          externalProvider: 'github',
          local: false,
          groups: ['G1', 'G2'],
          scmAccounts: ['SCM1', 'SCM2'],
          ...userOverrides
        })
      }}
    />
  );
}
