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
import { isSonarCloud } from '../../../helpers/system';
import { mockCurrentUser, mockLocation, mockLoggedInUser } from '../../../helpers/testMocks';
import { IssuesPage, Props } from '../IssuesPageSelector';

jest.mock('../../../helpers/system', () => ({ isSonarCloud: jest.fn().mockReturnValue(false) }));

it('should render normal issues page', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({ currentUser: mockLoggedInUser() })
      .find('Connect(IssuesAppContainer)')
      .prop('myIssues')
  ).toBeFalsy();
  (isSonarCloud as jest.Mock).mockReturnValueOnce(true);
  expect(
    shallowRender()
      .find('Connect(IssuesAppContainer)')
      .prop('myIssues')
  ).toBeFalsy();
});

it('should render my issues page', () => {
  (isSonarCloud as jest.Mock).mockReturnValueOnce(true);
  expect(
    shallowRender({ currentUser: mockLoggedInUser() })
      .find('Connect(IssuesAppContainer)')
      .prop('myIssues')
  ).toBeTruthy();
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <IssuesPage currentUser={mockCurrentUser()} location={mockLocation()} {...props} />
  );
}
