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
// @flow
import { uniq, without } from 'lodash';

/*::
type Favorite = { key: string };
*/

/*::
type ReceiveFavoritesAction = {
  type: 'RECEIVE_FAVORITES',
  favorites: Array<Favorite>,
  notFavorites: Array<Favorite>
};
*/

/*::
type AddFavoriteAction = {
  type: 'ADD_FAVORITE',
  componentKey: string
};
*/

/*::
type RemoveFavoriteAction = {
  type: 'REMOVE_FAVORITE',
  componentKey: string
};
*/

/*::
type Action = ReceiveFavoritesAction | AddFavoriteAction | RemoveFavoriteAction;
*/

/*::
type State = Array<string>;
*/

export const actions = {
  RECEIVE_FAVORITES: 'RECEIVE_FAVORITES',
  ADD_FAVORITE: 'ADD_FAVORITE',
  REMOVE_FAVORITE: 'REMOVE_FAVORITE'
};

export function receiveFavorites(
  favorites /*: Array<Favorite> */,
  notFavorites /*: Array<Favorite> */ = []
) /*: ReceiveFavoritesAction */ {
  return {
    type: actions.RECEIVE_FAVORITES,
    favorites,
    notFavorites
  };
}

export function addFavorite(componentKey /*: string */) /*: AddFavoriteAction */ {
  return {
    type: actions.ADD_FAVORITE,
    componentKey
  };
}

export function removeFavorite(componentKey /*: string */) /*: RemoveFavoriteAction */ {
  return {
    type: actions.REMOVE_FAVORITE,
    componentKey
  };
}

export default function(state /*: State */ = [], action /*: Action */) /*: State */ {
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
}

export function isFavorite(state /*: State */, componentKey /*: string */) {
  return state.includes(componentKey);
}
