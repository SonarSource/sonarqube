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
import components, * as fromComponents from './components/reducer';
import users, * as fromUsers from './users/reducer';
import favorites, * as fromFavorites from './favorites/reducer';
import languages, * as fromLanguages from './languages/reducer';
import measures, * as fromMeasures from './measures/reducer';
import globalMessages, * as fromGlobalMessages from '../../components/store/globalMessages';

import issuesActivity, * as fromIssuesActivity from '../../apps/account/home/store/reducer';
import projectsApp, * as fromProjectsApp from '../../apps/projects/store/reducer';

export default combineReducers({
  components,
  globalMessages,
  favorites,
  languages,
  measures,
  users,

  // apps
  issuesActivity,
  projectsApp
});

export const getComponent = (state, key) => (
    fromComponents.getComponent(state.components, key)
);

export const getGlobalMessages = state => (
    fromGlobalMessages.getGlobalMessages(state.globalMessages)
);

export const getLanguages = (state, key) => (
    fromLanguages.getLanguages(state.languages, key)
);

export const getCurrentUser = state => (
    fromUsers.getCurrentUser(state.users)
);

export const getFavorites = state => (
    fromFavorites.getFavorites(state.favorites)
);

export const getIssuesActivity = state => (
    fromIssuesActivity.getIssuesActivity(state.issuesActivity)
);

export const getComponentMeasure = (state, componentKey, metricKey) => (
    fromMeasures.getComponentMeasure(state.measures, componentKey, metricKey)
);

export const getComponentMeasures = (state, componentKey) => (
    fromMeasures.getComponentMeasures(state.measures, componentKey)
);

export const getProjects = state => (
    fromProjectsApp.getProjects(state.projectsApp)
);

export const getProjectsAppState = state => (
    fromProjectsApp.getState(state.projectsApp)
);

export const getProjectsAppFacetByProperty = (state, property) => (
    fromProjectsApp.getFacetByProperty(state.projectsApp, property)
);

export const getProjectsAppMaxFacetValue = state => (
    fromProjectsApp.getMaxFacetValue(state.projectsApp)
);
