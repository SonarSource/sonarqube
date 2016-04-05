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
import { enhanceWithSingleMeasure } from '../utils';
import { startFetching, stopFetching } from './statusActions';

/*
 * Actions
 */

export const UPDATE_STORE = 'drilldown/tree/UPDATE_STORE';
export const INIT = 'drilldown/tree/INIT';

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

function makeRequest (baseComponent, metric, options, periodIndex = 1) {
  const asc = metric.direction === 1;
  const ps = 100;
  const finalOptions = { asc, ps };

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
  return getComponentTree('children', baseComponent.key, [metric.key], finalOptions);
}

function fetchComponents (baseComponent, metric, pageIndex = 1, periodIndex = 1) {
  const options = { p: pageIndex };

  return makeRequest(baseComponent, metric, options, periodIndex).then(r => {
    const nextComponents = enhanceWithSingleMeasure(r.components, periodIndex);

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
    const { metric, periodIndex } = getState().tree;

    dispatch(startFetching());
    return fetchComponents(baseComponent, metric, 1, periodIndex).then(r => {
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
    const { tree } = getState();
    if (rootComponent === tree.rootComponent && metric === tree.metric) {
      return Promise.resolve();
    }

    dispatch(init(rootComponent, metric, periodIndex));
    dispatch(fetchList(rootComponent));
  };
}

/**
 * Fetch next page of components
 */
export function fetchMore () {
  return (dispatch, getState) => {
    const { metric, baseComponent, components, pageIndex, periodIndex } = getState().tree;
    dispatch(startFetching());
    return fetchComponents(baseComponent, metric, pageIndex + 1, periodIndex).then(r => {
      dispatch(updateStore({
        ...r,
        components: [...components, ...r.components]
      }));
      dispatch(stopFetching());
    });
  };
}

/**
 * Drilldown to the component
 * @param component
 */
export function drilldown (component) {
  return (dispatch, getState) => {
    const { metric, breadcrumbs, periodIndex } = getState().tree;
    dispatch(startFetching());
    return fetchComponents(component, metric, 1, periodIndex).then(r => {
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
    const { metric, breadcrumbs, periodIndex } = getState().tree;
    const index = breadcrumbs.indexOf(component);
    dispatch(startFetching());
    return fetchComponents(component, metric, 1, periodIndex).then(r => {
      dispatch(updateStore({
        ...r,
        breadcrumbs: breadcrumbs.slice(0, index + 1),
        selected: undefined
      }));
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
    const { breadcrumbs } = getState().tree;
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
    const { components, selected, breadcrumbs } = getState().tree;
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
    const { components, selected, breadcrumbs } = getState().tree;
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
