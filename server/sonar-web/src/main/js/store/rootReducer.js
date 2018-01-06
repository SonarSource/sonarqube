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
import marketplace, * as fromMarketplace from './marketplace/reducer';
import users, * as fromUsers from './users/reducer';
import favorites, * as fromFavorites from './favorites/duck';
import languages, * as fromLanguages from './languages/reducer';
import metrics, * as fromMetrics from './metrics/reducer';
import notifications, * as fromNotifications from './notifications/duck';
import organizations, * as fromOrganizations from './organizations/duck';
import organizationsMembers, * as fromOrganizationsMembers from './organizationsMembers/reducer';
import globalMessages, * as fromGlobalMessages from './globalMessages/duck';
import permissionsApp, * as fromPermissionsApp from '../apps/permissions/shared/store/rootReducer';
import projectAdminApp, * as fromProjectAdminApp from '../apps/project-admin/store/rootReducer';
import qualityGatesApp from '../apps/quality-gates/store/rootReducer';
import settingsApp, * as fromSettingsApp from '../apps/settings/store/rootReducer';

export default combineReducers({
  appState,
  globalMessages,
  favorites,
  languages,
  marketplace,
  metrics,
  notifications,
  organizations,
  organizationsMembers,
  users,

  // apps
  permissionsApp,
  projectAdminApp,
  qualityGatesApp,
  settingsApp
});

export const getAppState = state => state.appState;

export const getGlobalMessages = state =>
  fromGlobalMessages.getGlobalMessages(state.globalMessages);

export const getLanguages = state => fromLanguages.getLanguages(state.languages);

export const getLanguageByKey = (state, key) =>
  fromLanguages.getLanguageByKey(state.languages, key);

export const getCurrentUser = state => fromUsers.getCurrentUser(state.users);

export const getUserLogins = state => fromUsers.getUserLogins(state.users);

export const getUserByLogin = (state, login) => fromUsers.getUserByLogin(state.users, login);

export const getUsersByLogins = (state, logins) => fromUsers.getUsersByLogins(state.users, logins);

export const getUsers = state => fromUsers.getUsers(state.users);

export const isFavorite = (state, componentKey) =>
  fromFavorites.isFavorite(state.favorites, componentKey);

export const getMarketplaceState = state => state.marketplace;

export const getMarketplaceEditions = state => fromMarketplace.getEditions(state.marketplace);

export const getMarketplaceEditionStatus = state =>
  fromMarketplace.getEditionStatus(state.marketplace);

export const getMetrics = state => fromMetrics.getMetrics(state.metrics);

export const getMetricByKey = (state, key) => fromMetrics.getMetricByKey(state.metrics, key);

export const getMetricsKey = state => fromMetrics.getMetricsKey(state.metrics);

export const getGlobalNotifications = state => fromNotifications.getGlobal(state.notifications);

export const getProjectsWithNotifications = state =>
  fromNotifications.getProjects(state.notifications);

export const getProjectNotifications = (state, project) =>
  fromNotifications.getForProject(state.notifications, project);

export const getNotificationChannels = state => fromNotifications.getChannels(state.notifications);

export const getNotificationGlobalTypes = state =>
  fromNotifications.getGlobalTypes(state.notifications);

export const getNotificationPerProjectTypes = state =>
  fromNotifications.getPerProjectTypes(state.notifications);

export const getOrganizationByKey = (state, key) =>
  fromOrganizations.getOrganizationByKey(state.organizations, key);

export const getOrganizationGroupsByKey = (state, key) =>
  fromOrganizations.getOrganizationGroupsByKey(state.organizations, key);

export const getMyOrganizations = state =>
  fromOrganizations.getMyOrganizations(state.organizations);

export const areThereCustomOrganizations = state => getAppState(state).organizationsEnabled;

export const getOrganizationMembersLogins = (state, organization) =>
  fromOrganizationsMembers.getOrganizationMembersLogins(state.organizationsMembers, organization);

export const getOrganizationMembersState = (state, organization) =>
  fromOrganizationsMembers.getOrganizationMembersState(state.organizationsMembers, organization);

export const getQualityGatesAppState = state => state.qualityGatesApp;

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

export const getSettingsAppGlobalMessages = state =>
  fromSettingsApp.getGlobalMessages(state.settingsApp);

export const getProjectAdminLinkById = (state, linkId) =>
  fromProjectAdminApp.getLinkById(state.projectAdminApp, linkId);

export const getProjectAdminProjectLinks = (state, projectKey) =>
  fromProjectAdminApp.getProjectLinks(state.projectAdminApp, projectKey);

export const getProjectAdminComponentByKey = (state, componentKey) =>
  fromProjectAdminApp.getComponentByKey(state.projectAdminApp, componentKey);

export const getProjectAdminProjectModules = (state, projectKey) =>
  fromProjectAdminApp.getProjectModules(state.projectAdminApp, projectKey);

export const getProjectAdminGlobalMessages = state =>
  fromProjectAdminApp.getGlobalMessages(state.projectAdminApp);
