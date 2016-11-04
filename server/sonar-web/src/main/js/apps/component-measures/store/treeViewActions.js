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
import initial from 'lodash/initial';

import { getComponentTree } from '../../../api/components';
import { enhanceWithMeasure } from '../utils';
import { startFetching, stopFetching } from './statusActions';
import complementary from '../config/complementary';
import { getMeasuresAppTree } from '../../../app/store/rootReducer';

/*
 * Actions
 */

export const UPDATE_STORE = 'measuresApp/drilldown/tree/UPDATE_STORE';
export const INIT = 'measuresApp/drilldown/tree/INIT';

/*
 * Action Creators
 */

/**
 * Internal
 * Update store
 * @param state
 * @returns {{type: string, state: *}}
 */
function updateStore (state) {
  return { type: UPDATE_STORE, state };
}

/**
 * Init tree view drilldown for the given root component and given metric
 * @param rootComponent
 * @param metric
 * @param periodIndex
 * @returns {{type: string, rootComponent: *, metric: *}}
 */
function init (rootComponent, metric, periodIndex = 1) {
  return { type: INIT, rootComponent, metric, periodIndex };
}

/*
 * Workflow
 */

function getComplementary (metric) {
  const comp = complementary[metric] || [];
  return [metric, ...comp];
}

function makeRequest (rootComponent, baseComponent, metric, options, periodIndex = 1) {
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

  if (rootComponent.qualifier === 'DEV' && baseComponent.qualifier !== 'DEV') {
    Object.assign(options, { developerId: rootComponent.id });
  }

  Object.assign(finalOptions, options);

  const finalKey = baseComponent.refKey || baseComponent.key;

  return getComponentTree('children', finalKey, getComplementary(metric.key), finalOptions);
}

function fetchComponents (rootComponent, baseComponent, metric, pageIndex = 1, periodIndex = 1) {
  const options = { p: pageIndex };

  return makeRequest(rootComponent, baseComponent, metric, options, periodIndex).then(r => {
    const nextComponents = enhanceWithMeasure(r.components, metric.key, periodIndex);

    return {
      baseComponent,
      components: nextComponents,
      pageIndex: r.paging.pageIndex,
      total: r.paging.total
    };
  });
}

/**
 * Fetch the first page of components for a given base component
 * @param baseComponent
 */
function fetchList (baseComponent) {
  return (dispatch, getState) => {
    const { metric, periodIndex, rootComponent } = getMeasuresAppTree(getState());

    dispatch(startFetching());
    return fetchComponents(rootComponent, baseComponent, metric, 1, periodIndex).then(r => {
      dispatch(updateStore({
        ...r,
        baseComponent,
        breadcrumbs: [baseComponent]
      }));
      dispatch(stopFetching());
    });
  };
}

/**
 * Init tree view with root component and metric.
 * Fetch the first page of components if needed.
 * @param rootComponent
 * @param metric
 * @param periodIndex
 * @returns {function()}
 */
export function start (rootComponent, metric, periodIndex = 1) {
  return (dispatch, getState) => {
    const tree = getMeasuresAppTree(getState());
    if (rootComponent === tree.rootComponent && metric === tree.metric) {
      return Promise.resolve();
    }

    dispatch(init(rootComponent, metric, periodIndex));
    dispatch(fetchList(rootComponent));
  };
}

/**
 * Drilldown to the component
 * @param component
 */
export function drilldown (component) {
  return (dispatch, getState) => {
    const { metric, rootComponent, breadcrumbs, periodIndex } = getMeasuresAppTree(getState());
    dispatch(startFetching());
    return fetchComponents(rootComponent, component, metric, 1, periodIndex).then(r => {
      dispatch(updateStore({
        ...r,
        breadcrumbs: [...breadcrumbs, component],
        selected: undefined
      }));
      dispatch(stopFetching());
    });
  };
}

/**
 * Go up using breadcrumbs
 * @param component
 */
export function useBreadcrumbs (component) {
  return (dispatch, getState) => {
    const { metric, rootComponent, breadcrumbs, periodIndex } = getMeasuresAppTree(getState());
    const index = breadcrumbs.indexOf(component);
    dispatch(startFetching());
    return fetchComponents(rootComponent, component, metric, 1, periodIndex).then(r => {
      dispatch(updateStore({
        ...r,
        breadcrumbs: breadcrumbs.slice(0, index + 1),
        selected: undefined
      }));
      dispatch(stopFetching());
    });
  };
}

export function fetchMore () {
  return (dispatch, getState) => {
    const { rootComponent, baseComponent, metric, pageIndex, components, periodIndex } = getMeasuresAppTree(getState());
    dispatch(startFetching());
    return fetchComponents(rootComponent, baseComponent, metric, pageIndex + 1, periodIndex).then(r => {
      const nextComponents = [...components, ...r.components];
      dispatch(updateStore({ ...r, components: nextComponents }));
      dispatch(stopFetching());
    });
  };
}

/**
 * Select given component from the list
 * @param component
 */
export function selectComponent (component) {
  return (dispatch, getState) => {
    const { breadcrumbs } = getMeasuresAppTree(getState());
    const nextBreadcrumbs = [...breadcrumbs, component];
    dispatch(updateStore({
      selected: component,
      breadcrumbs: nextBreadcrumbs
    }));
  };
}

/**
 * Select next element from the list of components
 */
export function selectNext () {
  return (dispatch, getState) => {
    const { components, selected, breadcrumbs } = getMeasuresAppTree(getState());
    const selectedIndex = components.indexOf(selected);
    if (selectedIndex < components.length - 1) {
      const nextSelected = components[selectedIndex + 1];
      const nextBreadcrumbs = [...initial(breadcrumbs), nextSelected];
      dispatch(updateStore({
        selected: nextSelected,
        breadcrumbs: nextBreadcrumbs
      }));
    }
  };
}

/**
 * Select previous element from the list of components
 */
export function selectPrevious () {
  return (dispatch, getState) => {
    const { components, selected, breadcrumbs } = getMeasuresAppTree(getState());
    const selectedIndex = components.indexOf(selected);
    if (selectedIndex > 0) {
      const nextSelected = components[selectedIndex - 1];
      const nextBreadcrumbs = [...initial(breadcrumbs), nextSelected];
      dispatch(updateStore({
        selected: nextSelected,
        breadcrumbs: nextBreadcrumbs
      }));
    }
  };
}
