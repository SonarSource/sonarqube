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
import { combineReducers } from 'redux';
import appState from './appState/duck';
import users, * as fromUsers from './users/reducer';
import languages, * as fromLanguages from './languages/reducer';
import metrics, * as fromMetrics from './metrics/reducer';
import organizations, * as fromOrganizations from './organizations/duck';
import globalMessages, * as fromGlobalMessages from './globalMessages/duck';
import permissionsApp, * as fromPermissionsApp from '../apps/permissions/shared/store/rootReducer';
import projectAdminApp, * as fromProjectAdminApp from '../apps/project-admin/store/rootReducer';
import settingsApp, * as fromSettingsApp from '../apps/settings/store/rootReducer';

export default combineReducers({
  appState,
  globalMessages,
  languages,
  metrics,
  organizations,
  users,

  // apps
  permissionsApp,
  projectAdminApp,
  settingsApp
});

export const getAppState = state => state.appState;

export const getGlobalMessages = state =>
  fromGlobalMessages.getGlobalMessages(state.globalMessages);

export const getLanguages = state => fromLanguages.getLanguages(state.languages);

export const getCurrentUser = state => fromUsers.getCurrentUser(state.users);

export const getUsersByLogins = (state, logins) => fromUsers.getUsersByLogins(state.users, logins);

export const getMetrics = state => fromMetrics.getMetrics(state.metrics);

export const getMetricsKey = state => fromMetrics.getMetricsKey(state.metrics);

export const getOrganizationByKey = (state, key) =>
  fromOrganizations.getOrganizationByKey(state.organizations, key);

export const getOrganizationGroupsByKey = (state, key) =>
  fromOrganizations.getOrganizationGroupsByKey(state.organizations, key);

export const getMyOrganizations = state =>
  fromOrganizations.getMyOrganizations(state.organizations);

export const areThereCustomOrganizations = state => getAppState(state).organizationsEnabled;

export const getPermissionsAppUsers = state => fromPermissionsApp.getUsers(state.permissionsApp);

export const getPermissionsAppGroups = state => fromPermissionsApp.getGroups(state.permissionsApp);

export const isPermissionsAppLoading = state => fromPermissionsApp.isLoading(state.permissionsApp);

export const getPermissionsAppQuery = state => fromPermissionsApp.getQuery(state.permissionsApp);

export const getPermissionsAppFilter = state => fromPermissionsApp.getFilter(state.permissionsApp);

export const getPermissionsAppSelectedPermission = state =>
  fromPermissionsApp.getSelectedPermission(state.permissionsApp);

export const getPermissionsAppError = state => fromPermissionsApp.getError(state.permissionsApp);

export const getGlobalSettingValue = (state, key) =>
  fromSettingsApp.getValue(state.settingsApp, key);

export const getSettingsAppDefinition = (state, key) =>
  fromSettingsApp.getDefinition(state.settingsApp, key);

export const getSettingsAppAllCategories = state =>
  fromSettingsApp.getAllCategories(state.settingsApp);

export const getSettingsAppDefaultCategory = state =>
  fromSettingsApp.getDefaultCategory(state.settingsApp);

export const getSettingsAppSettingsForCategory = (state, category, componentKey) =>
  fromSettingsApp.getSettingsForCategory(state.settingsApp, category, componentKey);

export const getSettingsAppChangedValue = (state, key) =>
  fromSettingsApp.getChangedValue(state.settingsApp, key);

export const isSettingsAppLoading = (state, key) =>
  fromSettingsApp.isLoading(state.settingsApp, key);

export const getSettingsAppValidationMessage = (state, key) =>
  fromSettingsApp.getValidationMessage(state.settingsApp, key);

export const getSettingsAppEncryptionState = state =>
  fromSettingsApp.getEncryptionState(state.settingsApp);

export const getProjectAdminProjectModules = (state, projectKey) =>
  fromProjectAdminApp.getProjectModules(state.projectAdminApp, projectKey);
