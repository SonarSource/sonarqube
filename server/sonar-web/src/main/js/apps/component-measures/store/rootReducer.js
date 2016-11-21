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

import appReducer from './../app/reducer';
import statusReducer from './statusReducer';
import homeReducer from '../home/reducer';
import detailsReducer from '../details/reducer';
import listViewReducer from './listViewReducer';
import treeViewReducer from './treeViewReducer';

export default combineReducers({
  app: appReducer,
  home: homeReducer,
  details: detailsReducer,
  list: listViewReducer,
  tree: treeViewReducer,
  status: statusReducer
});

export const getComponent = state => (
    state.app.component
);

export const getAllMetrics = state => (
    state.app.metrics
);

export const getDetailsMetric = state => (
    state.details.metric
);

export const getDetailsMeasure = state => (
    state.details.measure
);

export const getDetailsSecondaryMeasure = state => (
    state.details.secondaryMeasure
);

export const getDetailsPeriods = state => (
    state.details.periods
);

export const isFetching = state => (
    state.status.fetching
);

export const getList = state => (
    state.list
);

export const getListComponents = state => (
    state.list.components
);

export const getListSelected = state => (
    state.list.selected
);

export const getListTotal = state => (
    state.list.total
);

export const getListPageIndex = state => (
    state.list.pageIndex
);

export const getTree = state => (
    state.tree
);

export const getTreeComponents = state => (
    state.tree.components
);

export const getTreeBreadcrumbs = state => (
    state.tree.breadcrumbs
);

export const getTreeSelected = state => (
    state.tree.selected
);

export const getTreeTotal = state => (
    state.tree.total
);

export const getTreePageIndex = state => (
    state.tree.pageIndex
);

export const getHomeDomains = state => (
    state.home.domains
);

export const getHomePeriods = state => (
    state.home.periods
);
