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
import Favorite from '../Favorite';

jest.mock('../../../api/favorites', () => ({
  addFavorite: jest.fn(() => Promise.resolve()),
  removeFavorite: jest.fn(() => Promise.resolve())
}));

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('calls handleFavorite when given', async () => {
  const handleFavorite = jest.fn();
  const wrapper = shallowRender(handleFavorite);
  const favoriteBase = wrapper.find('FavoriteBase');
  const addFavorite = favoriteBase.prop<Function>('addFavorite');
  const removeFavorite = favoriteBase.prop<Function>('removeFavorite');

  removeFavorite();
  await new Promise(setImmediate);
  expect(handleFavorite).toHaveBeenCalledWith('foo', false);

  addFavorite();
  await new Promise(setImmediate);
  expect(handleFavorite).toHaveBeenCalledWith('foo', true);
});

function shallowRender(handleFavorite?: () => void) {
  return shallow(
    <Favorite component="foo" favorite={true} handleFavorite={handleFavorite} qualifier="TRK" />
  );
}
