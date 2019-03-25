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
import { keyBy, sortBy, uniqBy } from 'lodash';
import { ActionType } from '../../../store/utils/actions';
import { DEFAULT_CATEGORY, getCategoryName } from '../utils';

const enum Actions {
  ReceiveDefinitions = 'RECEIVE_DEFINITIONS'
}

type Action = ActionType<typeof receiveDefinitions, Actions.ReceiveDefinitions>;

export type State = T.Dict<T.SettingCategoryDefinition>;

export function receiveDefinitions(definitions: T.SettingCategoryDefinition[]) {
  return { type: Actions.ReceiveDefinitions, definitions };
}

export default function components(state: State = {}, action: Action) {
  if (action.type === Actions.ReceiveDefinitions) {
    return keyBy(action.definitions, 'key');
  }
  return state;
}

export function getDefinition(state: State, key: string) {
  return state[key];
}

export function getAllDefinitions(state: State) {
  return Object.keys(state).map(key => state[key]);
}

export function getDefinitionsForCategory(state: State, category: string) {
  return getAllDefinitions(state).filter(
    definition => definition.category.toLowerCase() === category.toLowerCase()
  );
}

export function getAllCategories(state: State) {
  return uniqBy(getAllDefinitions(state).map(definition => definition.category), category =>
    category.toLowerCase()
  );
}

export function getDefaultCategory(state: State) {
  const categories = getAllCategories(state);
  if (categories.includes(DEFAULT_CATEGORY)) {
    return DEFAULT_CATEGORY;
  } else {
    const sortedCategories = sortBy(categories, category =>
      getCategoryName(category).toLowerCase()
    );
    return sortedCategories[0];
  }
}
