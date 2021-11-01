/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import React from 'react';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { PromotionNotification, PromotionNotificationProps } from '../PromotionNotification';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('anonymous');
  expect(
    shallowRender({ currentUser: mockLoggedInUser({ sonarLintAdSeen: true }) })
  ).toMatchSnapshot('adAlreadySeen');
  expect(shallowRender({ currentUser: mockLoggedInUser() })).toMatchSnapshot('loggedIn');
});

function shallowRender(props: Partial<PromotionNotificationProps> = {}) {
  return shallow(<PromotionNotification currentUser={mockCurrentUser()} {...props} />);
}
