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
import { getComponentTree } from '../../../api/components';
import { enhanceWithMeasure } from '../utils';
import { startFetching, stopFetching } from './statusActions';
import complementary from '../config/complementary';
import { getMeasuresAppList } from '../../../app/store/rootReducer';

export const UPDATE_STORE = 'measuresApp/drilldown/list/UPDATE_STORE';

function updateStore (state) {
  return { type: UPDATE_STORE, state };
}

function getComplementary (metric) {
  const comp = complementary[metric] || [];
  return [metric, ...comp];
}

function makeRequest (baseComponent, metric, options, periodIndex = 1) {
  const asc = metric.direction === 1;
  const ps = 100;
  const finalOptions = { asc, ps, metricSortFilter: 'withMeasuresOnly' };

  if (metric.key.indexOf('new_') === 0) {
    Object.assign(options, {
      s: 'metricPeriod,name',
      metricSort: metric.key,
      metricPeriodSort: periodIndex
    });
  } else {
    Object.assign(options, {
      s: 'metric,name',
      metricSort: metric.key
    });
  }

  Object.assign(finalOptions, options);
  return getComponentTree('leaves', baseComponent.key, getComplementary(metric.key), finalOptions);
}

function fetchLeaves (baseComponent, metric, pageIndex = 1, periodIndex = 1) {
  const options = { p: pageIndex };

  return makeRequest(baseComponent, metric, options, periodIndex).then(r => {
    const nextComponents = enhanceWithMeasure(r.components, metric.key, periodIndex);

    return {
      components: nextComponents,
      pageIndex: r.paging.pageIndex,
      total: r.paging.total
    };
  });
}

/**
 * Fetch the first page of components for a given base component
 * @param baseComponent
 * @param metric
 * @param periodIndex
 */
export function fetchList (baseComponent, metric, periodIndex = 1) {
  return (dispatch, getState) => {
    const list = getMeasuresAppList(getState());
    if (list.baseComponent === baseComponent && list.metric === metric) {
      return Promise.resolve();
    }

    dispatch(startFetching());
    return fetchLeaves(baseComponent, metric, 1, periodIndex).then(r => {
      dispatch(updateStore({
        ...r,
        baseComponent,
        metric
      }));
      dispatch(stopFetching());
    });
  };
}

export function fetchMore (periodIndex) {
  return (dispatch, getState) => {
    const { baseComponent, metric, pageIndex, components } = getMeasuresAppList(getState());
    dispatch(startFetching());
    return fetchLeaves(baseComponent, metric, pageIndex + 1, periodIndex).then(r => {
      const nextComponents = [...components, ...r.components];
      dispatch(updateStore({ ...r, components: nextComponents }));
      dispatch(stopFetching());
    });
  };
}

/**
 * Select specified component from the list
 * @param component A component to select
 */
export function selectComponent (component) {
  return dispatch => {
    dispatch(updateStore({ selected: component }));
  };
}

/**
 * Select next element from the list of components
 */
export function selectNext () {
  return (dispatch, getState) => {
    const { components, selected } = getMeasuresAppList(getState());
    const selectedIndex = components.indexOf(selected);
    if (selectedIndex < components.length - 1) {
      const nextSelected = components[selectedIndex + 1];
      dispatch(selectComponent(nextSelected));
    }
  };
}

/**
 * Select previous element from the list of components
 */
export function selectPrevious () {
  return (dispatch, getState) => {
    const { components, selected } = getMeasuresAppList(getState());
    const selectedIndex = components.indexOf(selected);
    if (selectedIndex > 0) {
      const nextSelected = components[selectedIndex - 1];
      dispatch(selectComponent(nextSelected));
    }
  };
}
