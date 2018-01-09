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
/* eslint-disable import/first */
jest.mock('../../../../helpers/storage', () => ({
  saveAll: jest.fn(),
  saveFavorite: jest.fn()
}));

import * as React from 'react';
import { shallow } from 'enzyme';
import FavoriteFilter from '../FavoriteFilter';
import { saveAll, saveFavorite } from '../../../../helpers/storage';
import { click } from '../../../../helpers/testUtils';

const currentUser = { isLoggedIn: true };
const query = { size: 1 };

beforeEach(() => {
  (saveAll as jest.Mock<any>).mockClear();
  (saveFavorite as jest.Mock<any>).mockClear();
});

it('renders for logged in user', () => {
  expect(shallow(<FavoriteFilter currentUser={currentUser} query={query} />)).toMatchSnapshot();
});

it('saves last selection', () => {
  const wrapper = shallow(<FavoriteFilter currentUser={currentUser} query={query} />);
  click(wrapper.find('#favorite-projects'));
  expect(saveFavorite).toBeCalled();
  click(wrapper.find('#all-projects'));
  expect(saveAll).toBeCalled();
});

it('handles organization', () => {
  expect(
    shallow(
      <FavoriteFilter currentUser={currentUser} organization={{ key: 'org' }} query={query} />
    )
  ).toMatchSnapshot();
});

it('does not save last selection with organization', () => {
  const wrapper = shallow(
    <FavoriteFilter currentUser={currentUser} organization={{ key: 'org' }} query={query} />
  );
  click(wrapper.find('#favorite-projects'));
  expect(saveFavorite).not.toBeCalled();
  click(wrapper.find('#all-projects'));
  expect(saveAll).not.toBeCalled();
});

it('does not render for anonymous', () => {
  expect(
    shallow(<FavoriteFilter currentUser={{ isLoggedIn: false }} query={query} />).type()
  ).toBeNull();
});
