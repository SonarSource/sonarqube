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
import { uniq, without } from 'lodash';

interface Favorite {
  key: string;
}

interface ReceiveFavoritesAction {
  type: 'RECEIVE_FAVORITES';
  favorites: Array<Favorite>;
  notFavorites: Array<Favorite>;
}

interface AddFavoriteAction {
  type: 'ADD_FAVORITE';
  componentKey: string;
}

interface RemoveFavoriteAction {
  type: 'REMOVE_FAVORITE';
  componentKey: string;
}

type Action = ReceiveFavoritesAction | AddFavoriteAction | RemoveFavoriteAction;

type State = string[];

export function receiveFavorites(
  favorites: Favorite[],
  notFavorites: Favorite[] = []
): ReceiveFavoritesAction {
  return { type: 'RECEIVE_FAVORITES', favorites, notFavorites };
}

export function addFavorite(componentKey: string): AddFavoriteAction {
  return { type: 'ADD_FAVORITE', componentKey };
}

export function removeFavorite(componentKey: string): RemoveFavoriteAction {
  return { type: 'REMOVE_FAVORITE', componentKey };
}

export default function(state: State = [], action: Action): State {
  if (action.type === 'RECEIVE_FAVORITES') {
    const toAdd = action.favorites.map(f => f.key);
    const toRemove = action.notFavorites.map(f => f.key);
    return without(uniq([...state, ...toAdd]), ...toRemove);
  }

  if (action.type === 'ADD_FAVORITE') {
    return uniq([...state, action.componentKey]);
  }

  if (action.type === 'REMOVE_FAVORITE') {
    return without(state, action.componentKey);
  }

  return state;
}

export function isFavorite(state: State, componentKey: string) {
  return state.includes(componentKey);
}
