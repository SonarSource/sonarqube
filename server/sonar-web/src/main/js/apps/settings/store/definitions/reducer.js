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
import keyBy from 'lodash/keyBy';
import sortBy from 'lodash/sortBy';
import uniqBy from 'lodash/uniqBy';
import { RECEIVE_DEFINITIONS } from './actions';
import { DEFAULT_CATEGORY, getCategoryName } from '../../utils';

const reducer = (state = {}, action = {}) => {
  if (action.type === RECEIVE_DEFINITIONS) {
    const definitionsByKey = keyBy(action.definitions, 'key');
    return { ...state, ...definitionsByKey };
  }

  return state;
};

export default reducer;

export const getAllDefinitions = state => Object.values(state);

export const getDefinitionsForCategory = (state, category) =>
    getAllDefinitions(state).filter(definition => definition.category.toLowerCase() === category.toLowerCase());

export const getAllCategories = state => uniqBy(
    getAllDefinitions(state).map(definition => definition.category),
    category => category.toLowerCase());

export const getDefaultCategory = state => {
  const categories = getAllCategories(state);
  if (categories.includes(DEFAULT_CATEGORY)) {
    return DEFAULT_CATEGORY;
  } else {
    const sortedCategories = sortBy(categories, category => getCategoryName(category).toLowerCase());
    return sortedCategories[0];
  }
};
