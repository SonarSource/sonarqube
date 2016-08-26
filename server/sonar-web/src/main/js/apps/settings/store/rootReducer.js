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
import definitions, * as fromDefinitions from './definitions/reducer';
import values, * as fromValues from './values/reducer';
import settingsPage, * as fromSettingsPage from './settingsPage/reducer';

const rootReducer = combineReducers({
  definitions,
  values,
  settingsPage
});

export default rootReducer;

export const getAllCategories = state => fromDefinitions.getAllCategories(state.definitions);

export const getDefaultCategory = state => fromDefinitions.getDefaultCategory(state.definitions);

export const getValue = (state, key) => fromValues.getValue(state.values, key);

export const getSettingsForCategory = (state, category) =>
    fromDefinitions.getDefinitionsForCategory(state.definitions, category).map(definition => ({
      ...getValue(state, definition.key),
      definition
    }));

export const isLoading = (state, key) => fromSettingsPage.isLoading(state.settingsPage, key);

export const getValidationMessage = (state, key) => fromSettingsPage.getValidationMessage(state.settingsPage, key);
