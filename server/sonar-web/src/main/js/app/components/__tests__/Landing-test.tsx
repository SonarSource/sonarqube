/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { mockCurrentUser, mockLoggedInUser, mockRouter } from '../../../helpers/testMocks';
import { CurrentUser } from '../../../types/types';
import { Landing } from '../Landing';

it.each([
  [mockCurrentUser(), '/projects'],
  [mockLoggedInUser(), '/projects'],
  [
    mockLoggedInUser({ homepage: { type: 'ISSUES' } }),
    expect.objectContaining({ pathname: '/issues' })
  ]
])('should render correctly', (currentUser: CurrentUser, homepageUrl: string) => {
  const router = mockRouter();
  shallowRender({ router, currentUser });
  expect(router.replace).toHaveBeenCalledWith(homepageUrl);
});

function shallowRender(props: Partial<Landing['props']> = {}) {
  return shallow<Landing>(
    <Landing currentUser={mockCurrentUser()} router={mockRouter()} {...props} />
  );
}
