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
import * as React from 'react';
import { shallow } from 'enzyme';
import { Profile, Props } from '../Profile';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { isSonarCloud } from '../../../../helpers/system';

jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn().mockReturnValue(false) }));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render email', () => {
  expect(
    shallowRender(mockLoggedInUser({ email: 'john@doe.com' }))
      .find('#email')
      .exists()
  ).toBe(true);
});

it('should render external identity', () => {
  expect(
    shallowRender(mockLoggedInUser({ local: false, externalProvider: 'github' }))
      .find('UserExternalIdentity')
      .exists()
  ).toBe(true);
});

it('should not display user groups', () => {
  (isSonarCloud as jest.Mock).mockReturnValueOnce(true);
  expect(
    shallowRender()
      .find('UserGroups')
      .exists()
  ).toBe(false);
});

function shallowRender(currentUser: Props['currentUser'] = mockLoggedInUser()) {
  return shallow(<Profile currentUser={currentUser} />);
}
