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
import _ from 'underscore';
import { pushPath, replacePath } from 'redux-simple-router';

import { getChildren, getComponent, getTree, getBreadcrumbs } from '../../../api/components';
import { translate } from '../../../helpers/l10n';
import { getComponentUrl } from '../../../helpers/urls';

const VIEW_METRICS = [
  'releasability_rating',
  'alert_status',
  'reliability_rating',
  'security_rating',
  'sqale_rating',
  'ncloc'
];

const METRICS = [
  'ncloc',
  'code_smells',
  'bugs',
  'vulnerabilities',
  'duplicated_lines_density',
  'alert_status'
];

const METRICS_WITH_COVERAGE = [
  ...METRICS,
  'coverage',
  'it_coverage',
  'overall_coverage'
];

const PAGE_SIZE = 100;

export const INIT = 'INIT';
export const BROWSE = 'BROWSE';
export const LOAD_MORE = 'LOAD_MORE';
export const SEARCH = 'SEARCH';
export const SELECT_NEXT = 'SELECT_NEXT';
export const SELECT_PREV = 'SELECT_PREV';
export const UPDATE_QUERY = 'UPDATE_QUERY';
export const START_FETCHING = 'START_FETCHING';
export const STOP_FETCHING = 'STOP_FETCHING';
export const RAISE_ERROR = 'RAISE_ERROR';

export function initComponentAction (component, breadcrumbs = []) {
  return {
    type: INIT,
    component,
    breadcrumbs
  };
}

export function browseAction (component, children = [], breadcrumbs = [], total = 0) {
  return {
    type: BROWSE,
    component,
    children,
    breadcrumbs,
    total
  };
}

export function loadMoreAction (children, page) {
  return {
    type: LOAD_MORE,
    children,
    page
  };
}

export function searchAction (components) {
  return {
    type: SEARCH,
    components
  };
}

export function updateQueryAction (query) {
  return {
    type: UPDATE_QUERY,
    query
  };
}

export function selectNext () {
  return { type: SELECT_NEXT };
}

export function selectPrev () {
  return { type: SELECT_PREV };
}

export function startFetching () {
  return { type: START_FETCHING };
}

export function stopFetching () {
  return { type: STOP_FETCHING };
}

export function raiseError (message) {
  return {
    type: RAISE_ERROR,
    message
  };
}

function getPath (componentKey) {
  return '/' + encodeURIComponent(componentKey);
}

function expandRootDir (metrics) {
  return function ({ children, total, ...other }) {
    const rootDir = children.find(component => component.qualifier === 'DIR' && component.name === '/');
    if (rootDir) {
      return getChildren(rootDir.key, metrics).then(r => {
        const nextChildren = _.without([...children, ...r.components], rootDir);
        const nextTotal = total + r.components.length - /* root dir */ 1;
        return { children: nextChildren, total: nextTotal, ...other };
      });
    } else {
      return { children, total, ...other };
    }
  }
}

function prepareChildren (r) {
  return { children: r.components, total: r.paging.total, page: r.paging.pageIndex };
}

function skipRootDir (breadcrumbs) {
  return breadcrumbs.filter(component => {
    return !(component.qualifier === 'DIR' && component.name === '/');
  });
}

function retrieveComponentBase (componentKey, candidate, metrics) {
  return candidate ?
      Promise.resolve(candidate) :
      getComponent(componentKey, metrics);
}

function retrieveComponentChildren (componentKey, candidate, metrics) {
  return candidate && candidate.children ?
      Promise.resolve({ children: candidate.children, total: candidate.total }) :
      getChildren(componentKey, metrics, { ps: PAGE_SIZE }).then(prepareChildren).then(expandRootDir(metrics));
}

function retrieveComponentBreadcrumbs (componentKey, candidate) {
  return candidate && candidate.breadcrumbs ?
      Promise.resolve(candidate.breadcrumbs) :
      getBreadcrumbs({ key: componentKey }).then(skipRootDir);
}

function retrieveComponent (componentKey, bucket, isView) {
  const candidate = _.findWhere(bucket, { key: componentKey });
  const metrics = isView ? VIEW_METRICS : METRICS_WITH_COVERAGE;
  return Promise.all([
    retrieveComponentBase(componentKey, candidate, metrics),
    retrieveComponentChildren(componentKey, candidate, metrics),
    retrieveComponentBreadcrumbs(componentKey, candidate)
  ]);
}

function requestTree (query, baseComponent, dispatch) {
  dispatch(startFetching());
  return getTree(baseComponent.key, { q: query, s: 'qualifier,name' })
      .then(r => dispatch(searchAction(r.components)))
      .then(() => dispatch(stopFetching()));
}

async function getErrorMessage (response) {
  switch (response.status) {
    case 401:
      return translate('not_authorized');
    default:
      try {
        const json = await response.json();
        return json['err_msg'] ||
            (json.errors && _.pluck(json.errors, 'msg').join('. ')) ||
            translate('default_error_message');
      } catch (e) {
        return translate('default_error_message');
      }
  }
}

export function initComponent (component, breadcrumbs) {
  return dispatch => {
    const componentKey = component.key;
    const isView = component.qualifier === 'VW' || component.qualifier === 'SVW';
    const metrics = isView ? VIEW_METRICS : METRICS_WITH_COVERAGE;
    dispatch(startFetching());
    return getComponent(componentKey, metrics)
        .then(component => dispatch(initComponentAction(component, breadcrumbs)))
        .then(() => dispatch(replacePath(getPath(componentKey))))
        .then(() => dispatch(stopFetching()));
  };
}

export function browse (componentKey) {
  return (dispatch, getState) => {
    const { bucket, current } = getState();
    dispatch(startFetching());
    return retrieveComponent(componentKey, bucket, current.isView)
        .then(([component, children, breadcrumbs]) => {
          if (component.refKey) {
            window.location = getComponentUrl(component.refKey);
            return new Promise();
          } else {
            dispatch(browseAction(component, children.children, breadcrumbs, children.total));
          }
        })
        .then(() => dispatch(pushPath(getPath(componentKey))))
        .then(() => dispatch(stopFetching()))
        .catch(e => {
          getErrorMessage(e.response)
              .then(message => dispatch(raiseError(message)));
        });
  };
}

export function loadMore () {
  return (dispatch, getState) => {
    const { baseComponent, page } = getState().current;
    return getChildren(baseComponent.key, METRICS_WITH_COVERAGE, { p: page + 1, ps: PAGE_SIZE })
        .then(prepareChildren)
        .then(({ children }) => {
          dispatch(loadMoreAction(children, page + 1));
          dispatch(stopFetching());
        })
        .catch(e => {
          getErrorMessage(e.response)
              .then(message => dispatch(raiseError(message)));
        });
  };
}

let debouncedSearch = function (query, baseComponent, dispatch) {
  if (query) {
    requestTree(query, baseComponent, dispatch);
  } else {
    dispatch(searchAction(null));
  }
};
debouncedSearch = _.debounce(debouncedSearch, 250);

export function search (query, baseComponent) {
  return dispatch => {
    dispatch(updateQueryAction(query));

    if (query.length > 2 || !query.length) {
      debouncedSearch(query, baseComponent, dispatch);
    }
  };
}

export function selectCurrent () {
  return (dispatch, getState) => {
    const { searchResults } = getState().current;
    if (searchResults) {
      const componentKey = getState().current.searchSelectedItem.key;
      dispatch(browse(componentKey));
    }
  };
}
