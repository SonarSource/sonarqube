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
import { combineReducers } from 'redux';
import definitions, * as fromDefinitions from './definitions';
import values, * as fromValues from './values';
import settingsPage, * as fromSettingsPage from './settingsPage';
import globalMessages, * as fromGlobalMessages from '../../../store/globalMessages';

interface State {
  definitions: fromDefinitions.State;
  globalMessages: fromGlobalMessages.State;
  settingsPage: fromSettingsPage.State;
  values: fromValues.State;
}

export default combineReducers({ definitions, values, settingsPage, globalMessages });

export function getDefinition(state: State, key: string) {
  return fromDefinitions.getDefinition(state.definitions, key);
}

export function getAllCategories(state: State) {
  return fromDefinitions.getAllCategories(state.definitions);
}

export function getDefaultCategory(state: State) {
  return fromDefinitions.getDefaultCategory(state.definitions);
}

export function getValue(state: State, key: string, component?: string) {
  return fromValues.getValue(state.values, key, component);
}

export function getSettingsForCategory(state: State, category: string, component?: string) {
  return fromDefinitions.getDefinitionsForCategory(state.definitions, category).map(definition => ({
    key: definition.key,
    ...getValue(state, definition.key, component),
    definition
  }));
}

export function getChangedValue(state: State, key: string) {
  return fromSettingsPage.getChangedValue(state.settingsPage, key);
}

export function isLoading(state: State, key: string) {
  return fromSettingsPage.isLoading(state.settingsPage, key);
}

export function getValidationMessage(state: State, key: string) {
  return fromSettingsPage.getValidationMessage(state.settingsPage, key);
}

export function getGlobalMessages(state: State) {
  return fromGlobalMessages.getGlobalMessages(state.globalMessages);
}
