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
import { ButtonLink } from '../../../components/controls/buttons';
import { mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
import { click, waitAndUpdate } from '../../../helpers/testUtils';
import { HomePage } from '../../../types/types';
import { DEFAULT_HOMEPAGE, HomePageSelect } from '../HomePageSelect';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('unchecked');
  expect(
    shallowRender({ currentUser: mockLoggedInUser({ homepage: { type: 'MY_PROJECTS' } }) })
  ).toMatchSnapshot('checked');
  expect(
    shallowRender({
      currentUser: mockLoggedInUser({ homepage: DEFAULT_HOMEPAGE }),
      currentPage: DEFAULT_HOMEPAGE
    })
  ).toMatchSnapshot('checked, and on default');
  expect(shallowRender({ currentUser: mockCurrentUser() }).type()).toBeNull();
});

it('should correctly call webservices', async () => {
  const setHomePage = jest.fn();
  const currentPage: HomePage = { type: 'MY_ISSUES' };
  const wrapper = shallowRender({ setHomePage, currentPage });

  // Set homepage.
  click(wrapper.find(ButtonLink));
  await waitAndUpdate(wrapper);
  expect(setHomePage).toHaveBeenLastCalledWith(currentPage);

  // Reset.
  wrapper.setProps({ currentUser: mockLoggedInUser({ homepage: currentPage }) });
  click(wrapper.find(ButtonLink));
  await waitAndUpdate(wrapper);
  expect(setHomePage).toHaveBeenLastCalledWith(DEFAULT_HOMEPAGE);
});

function shallowRender(props: Partial<HomePageSelect['props']> = {}) {
  return shallow<HomePageSelect>(
    <HomePageSelect
      currentPage={{ type: 'MY_PROJECTS' }}
      currentUser={mockLoggedInUser()}
      setHomePage={jest.fn()}
      {...props}
    />
  );
}
