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
import { PrivacyBadge } from '../PrivacyBadgeContainer';
import { Visibility, OrganizationSubscription } from '../../../app/types';
import { isSonarCloud } from '../../../helpers/system';

jest.mock('../../../helpers/system', () => ({ isSonarCloud: jest.fn().mockReturnValue(false) }));

const organization = { key: 'foo', name: 'Foo' };
const loggedInUser = { isLoggedIn: true, login: 'luke', name: 'Skywalker' };

it('renders', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('do not render', () => {
  expect(getWrapper({ visibility: Visibility.Public })).toMatchSnapshot();
});

it('renders public', () => {
  (isSonarCloud as jest.Mock<any>).mockReturnValueOnce(true);
  expect(getWrapper({ visibility: Visibility.Public })).toMatchSnapshot();
});

it('renders public with icon', () => {
  (isSonarCloud as jest.Mock<any>).mockReturnValueOnce(true);
  expect(
    getWrapper({
      organization: {
        ...organization,
        actions: { admin: true },
        subscription: OrganizationSubscription.Paid
      },
      visibility: Visibility.Public
    })
  ).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <PrivacyBadge
      currentUser={loggedInUser}
      organization={organization}
      qualifier="TRK"
      userOrganizations={[organization]}
      visibility={Visibility.Private}
      {...props}
    />
  );
}
