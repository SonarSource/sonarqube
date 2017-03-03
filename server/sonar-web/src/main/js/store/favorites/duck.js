/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import uniq from 'lodash/uniq';
import without from 'lodash/without';

export const actions = {
  RECEIVE_FAVORITES: 'RECEIVE_FAVORITES',
  ADD_FAVORITE: 'ADD_FAVORITE',
  REMOVE_FAVORITE: 'REMOVE_FAVORITE'
};

export const receiveFavorites = (favorites, notFavorites = []) => ({
  type: actions.RECEIVE_FAVORITES,
  favorites,
  notFavorites
});

export const addFavorite = componentKey => ({
  type: actions.ADD_FAVORITE,
  componentKey
});

export const removeFavorite = componentKey => ({
  type: actions.REMOVE_FAVORITE,
  componentKey
});

export default (state = [], action = {}) => {
  if (action.type === actions.RECEIVE_FAVORITES) {
    const toAdd = action.favorites.map(f => f.key);
    const toRemove = action.notFavorites.map(f => f.key);
    return without(uniq([...state, ...toAdd]), ...toRemove);
  }

  if (action.type === actions.ADD_FAVORITE) {
    return uniq([...state, action.componentKey]);
  }

  if (action.type === actions.REMOVE_FAVORITE) {
    return without(state, action.componentKey);
  }

  return state;
};

export const isFavorite = (state, componentKey) => (
    state.includes(componentKey)
);

