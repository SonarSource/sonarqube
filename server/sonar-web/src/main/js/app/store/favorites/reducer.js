/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { combineReducers } from 'redux';
import keyBy from 'lodash/keyBy';
import { RECEIVE_FAVORITES } from './actions';

const favoritesByKey = (state = {}, action = {}) => {
  if (action.type === RECEIVE_FAVORITES) {
    const byKey = keyBy(action.favorites, 'key');
    return { ...state, ...byKey };
  }

  return state;
};

const favoriteKeys = (state = null, action = {}) => {
  if (action.type === RECEIVE_FAVORITES) {
    return action.favorites.map(f => f.key);
  }

  return state;
};

export default combineReducers({ favoritesByKey, favoriteKeys });

export const getFavorites = state => (
    state.favoriteKeys ?
        state.favoriteKeys.map(key => state.favoritesByKey[key]) :
        null
);
