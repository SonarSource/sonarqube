/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import components, * as fromComponents from './components/reducer';
import users, * as fromUsers from './users/reducer';
import favorites, * as fromFavorites from './favorites/duck';
import issues, * as fromIssues from './issues/duck';
import languages, * as fromLanguages from './languages/reducer';
import measures, * as fromMeasures from './measures/reducer';
import notifications, * as fromNotifications from './notifications/duck';
import organizations, * as fromOrganizations from './organizations/duck';
import organizationsMembers, * as fromOrganizationsMembers from './organizationsMembers/reducer';
import globalMessages, * as fromGlobalMessages from './globalMessages/duck';
import projectActivity from './projectActivity/duck';
import measuresApp, * as fromMeasuresApp from '../apps/component-measures/store/rootReducer';
import permissionsApp, * as fromPermissionsApp from '../apps/permissions/shared/store/rootReducer';
import projectAdminApp, * as fromProjectAdminApp from '../apps/project-admin/store/rootReducer';
import projectsApp, * as fromProjectsApp from '../apps/projects/store/reducer';
import qualityGatesApp from '../apps/quality-gates/store/rootReducer';
import settingsApp, * as fromSettingsApp from '../apps/settings/store/rootReducer';

export default combineReducers({
  appState,
  components,
  globalMessages,
  favorites,
  issues,
  languages,
  measures,
  notifications,
  organizations,
  organizationsMembers,
  projectActivity,
  users,

  // apps
  measuresApp,
  permissionsApp,
  projectAdminApp,
  projectsApp,
  qualityGatesApp,
  settingsApp
});

export const getAppState = state => state.appState;

export const getComponent = (state, key) => fromComponents.getComponent(state.components, key);

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

export const getIssueByKey = (state, key) => fromIssues.getIssueByKey(state.issues, key);

export const getComponentMeasure = (state, componentKey, metricKey) =>
  fromMeasures.getComponentMeasure(state.measures, componentKey, metricKey);

export const getComponentMeasures = (state, componentKey) =>
  fromMeasures.getComponentMeasures(state.measures, componentKey);

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

export const getProjectActivity = state => state.projectActivity;

export const getProjects = state => fromProjectsApp.getProjects(state.projectsApp);

export const getProjectsAppState = state => fromProjectsApp.getState(state.projectsApp);

export const getProjectsAppFacetByProperty = (state, property) =>
  fromProjectsApp.getFacetByProperty(state.projectsApp, property);

export const getProjectsAppMaxFacetValue = state =>
  fromProjectsApp.getMaxFacetValue(state.projectsApp);

export const getQualityGatesAppState = state => state.qualityGatesApp;

export const getPermissionsAppUsers = state => fromPermissionsApp.getUsers(state.permissionsApp);

export const getPermissionsAppGroups = state => fromPermissionsApp.getGroups(state.permissionsApp);

export const isPermissionsAppLoading = state => fromPermissionsApp.isLoading(state.permissionsApp);

export const getPermissionsAppQuery = state => fromPermissionsApp.getQuery(state.permissionsApp);

export const getPermissionsAppFilter = state => fromPermissionsApp.getFilter(state.permissionsApp);

export const getPermissionsAppSelectedPermission = state =>
  fromPermissionsApp.getSelectedPermission(state.permissionsApp);

export const getPermissionsAppError = state => fromPermissionsApp.getError(state.permissionsApp);

export const getSettingValue = (state, key) => fromSettingsApp.getValue(state.settingsApp, key);

export const getSettingsAppDefinition = (state, key) =>
  fromSettingsApp.getDefinition(state.settingsApp, key);

export const getSettingsAppAllCategories = state =>
  fromSettingsApp.getAllCategories(state.settingsApp);

export const getSettingsAppDefaultCategory = state =>
  fromSettingsApp.getDefaultCategory(state.settingsApp);

export const getSettingsAppSettingsForCategory = (state, category) =>
  fromSettingsApp.getSettingsForCategory(state.settingsApp, category);

export const getSettingsAppChangedValue = (state, key) =>
  fromSettingsApp.getChangedValue(state.settingsApp, key);

export const isSettingsAppLoading = (state, key) =>
  fromSettingsApp.isLoading(state.settingsApp, key);

export const getSettingsAppLicenseByKey = (state, key) =>
  fromSettingsApp.getLicenseByKey(state.settingsApp, key);

export const getSettingsAppAllLicenseKeys = state =>
  fromSettingsApp.getAllLicenseKeys(state.settingsApp);

export const getSettingsAppValidationMessage = (state, key) =>
  fromSettingsApp.getValidationMessage(state.settingsApp, key);

export const getSettingsAppEncryptionState = state =>
  fromSettingsApp.getEncryptionState(state.settingsApp);

export const getSettingsAppGlobalMessages = state =>
  fromSettingsApp.getGlobalMessages(state.settingsApp);

export const getProjectAdminProfileByKey = (state, profileKey) =>
  fromProjectAdminApp.getProfileByKey(state.projectAdminApp, profileKey);

export const getProjectAdminAllProfiles = state =>
  fromProjectAdminApp.getAllProfiles(state.projectAdminApp);

export const getProjectAdminProjectProfiles = (state, projectKey) =>
  fromProjectAdminApp.getProjectProfiles(state.projectAdminApp, projectKey);

export const getProjectAdminGateById = (state, gateId) =>
  fromProjectAdminApp.getGateById(state.projectAdminApp, gateId);

export const getProjectAdminAllGates = state =>
  fromProjectAdminApp.getAllGates(state.projectAdminApp);

export const getProjectAdminProjectGate = (state, projectKey) =>
  fromProjectAdminApp.getProjectGate(state.projectAdminApp, projectKey);

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

export const getMeasuresAppComponent = state => fromMeasuresApp.getComponent(state.measuresApp);

export const getMeasuresAppAllMetrics = state => fromMeasuresApp.getAllMetrics(state.measuresApp);

export const getMeasuresAppDetailsMetric = state =>
  fromMeasuresApp.getDetailsMetric(state.measuresApp);

export const getMeasuresAppDetailsMeasure = state =>
  fromMeasuresApp.getDetailsMeasure(state.measuresApp);

export const getMeasuresAppDetailsSecondaryMeasure = state =>
  fromMeasuresApp.getDetailsSecondaryMeasure(state.measuresApp);

export const getMeasuresAppDetailsPeriods = state =>
  fromMeasuresApp.getDetailsPeriods(state.measuresApp);

export const isMeasuresAppFetching = state => fromMeasuresApp.isFetching(state.measuresApp);

export const getMeasuresAppList = state => fromMeasuresApp.getList(state.measuresApp);

export const getMeasuresAppListComponents = state =>
  fromMeasuresApp.getListComponents(state.measuresApp);

export const getMeasuresAppListSelected = state =>
  fromMeasuresApp.getListSelected(state.measuresApp);

export const getMeasuresAppListTotal = state => fromMeasuresApp.getListTotal(state.measuresApp);

export const getMeasuresAppListPageIndex = state =>
  fromMeasuresApp.getListPageIndex(state.measuresApp);

export const getMeasuresAppTree = state => fromMeasuresApp.getTree(state.measuresApp);

export const getMeasuresAppTreeComponents = state =>
  fromMeasuresApp.getTreeComponents(state.measuresApp);

export const getMeasuresAppTreeBreadcrumbs = state =>
  fromMeasuresApp.getTreeBreadcrumbs(state.measuresApp);

export const getMeasuresAppTreeSelected = state =>
  fromMeasuresApp.getTreeSelected(state.measuresApp);

export const getMeasuresAppTreeTotal = state => fromMeasuresApp.getTreeTotal(state.measuresApp);

export const getMeasuresAppTreePageIndex = state =>
  fromMeasuresApp.getTreePageIndex(state.measuresApp);

export const getMeasuresAppHomeDomains = state => fromMeasuresApp.getHomeDomains(state.measuresApp);

export const getMeasuresAppHomePeriods = state => fromMeasuresApp.getHomePeriods(state.measuresApp);
