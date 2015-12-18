import _ from 'underscore';
import { pushPath } from 'redux-simple-router';

import { getChildren, getComponent } from '../../../api/components';
import { getComponentNavigation } from '../../../api/nav';


const METRICS = [
  'ncloc',
  'sqale_index',
  'violations',
  'duplicated_lines_density'
];

const METRICS_WITH_COVERAGE = [
  ...METRICS,
  'coverage',
  'it_coverage',
  'overall_coverage'
];


export const INIT = 'INIT';
export const BROWSE = 'BROWSE';
export const START_FETCHING = 'START_FETCHING';
export const STOP_FETCHING = 'STOP_FETCHING';


export function initComponentAction (component, breadcrumbs = []) {
  return {
    type: INIT,
    component,
    breadcrumbs
  };
}

export function browseAction (component, children = [], breadcrumbs = []) {
  return {
    type: BROWSE,
    component,
    children,
    breadcrumbs
  };
}

export function startFetching () {
  return { type: START_FETCHING };
}

export function stopFetching () {
  return { type: STOP_FETCHING };
}


function getPath (componentKey) {
  return '/' + encodeURIComponent(componentKey);
}

function retrieveComponentBase (componentKey, candidate) {
  return candidate ?
      Promise.resolve(candidate) :
      getComponent(componentKey, METRICS_WITH_COVERAGE);
}

function retrieveComponentChildren (componentKey, candidate) {
  return candidate && candidate.children ?
      Promise.resolve(candidate.children) :
      getChildren(componentKey, METRICS_WITH_COVERAGE);
}

function retrieveComponentBreadcrumbs (componentKey, candidate) {
  return candidate && candidate.breadcrumbs ?
      Promise.resolve(candidate.breadcrumbs) :
      getComponentNavigation(componentKey).then(navigation => navigation.breadcrumbs);
}

function retrieveComponent (componentKey, bucket) {
  const candidate = _.findWhere(bucket, { key: componentKey });
  return Promise.all([
    retrieveComponentBase(componentKey, candidate),
    retrieveComponentChildren(componentKey, candidate),
    retrieveComponentBreadcrumbs(componentKey, candidate)
  ]);
}

export function initComponent (componentKey, breadcrumbs) {
  return dispatch => {
    dispatch(startFetching());
    return getComponent(componentKey, METRICS_WITH_COVERAGE)
        .then(component => dispatch(initComponentAction(component, breadcrumbs)))
        .then(() => dispatch(stopFetching()));
  };
}

export function browse (componentKey) {
  return (dispatch, getState) => {
    const { bucket } = getState();
    dispatch(startFetching());
    return retrieveComponent(componentKey, bucket)
        .then(([component, children, breadcrumbs]) => {
          dispatch(browseAction(component, children, breadcrumbs));
        })
        .then(() => dispatch(pushPath(getPath(componentKey))))
        .then(() => dispatch(stopFetching()));
  };
}
