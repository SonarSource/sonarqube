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
import { keyBy, sortBy, uniqBy } from 'lodash';
import { RECEIVE_DEFINITIONS } from './actions';
import { DEFAULT_CATEGORY, getCategoryName } from '../../utils';
/*:: import type { Definition } from '../../types'; */

/*::
type State = { [key: string]: Definition };
*/

/*::
type Action = { type: string, definitions: Definition[] };
*/

const reducer = (state /*: State */ = {}, action /*: Action */) => {
  if (action.type === RECEIVE_DEFINITIONS) {
    return keyBy(action.definitions, 'key');
  }

  return state;
};

export default reducer;

export function getDefinition(state /*: State */, key /*: string */) /*: Definition */ {
  return state[key];
}

export function getAllDefinitions(state /*: State */) /*: Definition[] */ {
  return Object.keys(state).map(key => state[key]);
}

export const getDefinitionsForCategory = (state /*: State */, category /*: string */) =>
  getAllDefinitions(state).filter(
    definition => definition.category.toLowerCase() === category.toLowerCase()
  );

export const getAllCategories = (state /*: State */) =>
  uniqBy(getAllDefinitions(state).map(definition => definition.category), category =>
    category.toLowerCase()
  );

export const getDefaultCategory = (state /*: State */) => {
  const categories = getAllCategories(state);
  if (categories.includes(DEFAULT_CATEGORY)) {
    return DEFAULT_CATEGORY;
  } else {
    const sortedCategories = sortBy(categories, category =>
      getCategoryName(category).toLowerCase()
    );
    return sortedCategories[0];
  }
};
