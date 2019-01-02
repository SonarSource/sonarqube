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
import FavoriteBase, { Props } from '../FavoriteBase';
import { click } from '../../../helpers/testUtils';

it('should render favorite', () => {
  const favorite = renderFavoriteBase({ favorite: true });
  expect(favorite).toMatchSnapshot();
});

it('should render not favorite', () => {
  const favorite = renderFavoriteBase({ favorite: false });
  expect(favorite).toMatchSnapshot();
});

it('should add favorite', () => {
  const addFavorite = jest.fn(() => Promise.resolve());
  const favorite = renderFavoriteBase({ favorite: false, addFavorite });
  click(favorite.find('a'));
  expect(addFavorite).toBeCalled();
});

it('should remove favorite', () => {
  const removeFavorite = jest.fn(() => Promise.resolve());
  const favorite = renderFavoriteBase({ favorite: true, removeFavorite });
  click(favorite.find('a'));
  expect(removeFavorite).toBeCalled();
});

function renderFavoriteBase(props: Partial<Props> = {}) {
  return shallow(
    <FavoriteBase
      addFavorite={jest.fn()}
      favorite={true}
      qualifier="TRK"
      removeFavorite={jest.fn()}
      {...props}
    />
  );
}
