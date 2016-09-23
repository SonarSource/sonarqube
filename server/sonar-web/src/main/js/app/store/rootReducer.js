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
import users, * as fromUsers from './users/reducer';
import favorites, * as fromFavorites from './favorites/reducer';
import measures, * as fromMeasures from './measures/reducer';

import issuesActivity, * as fromIssuesActivity from '../../apps/account/home/store/reducer';

export default combineReducers({ users, favorites, issuesActivity, measures });

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
