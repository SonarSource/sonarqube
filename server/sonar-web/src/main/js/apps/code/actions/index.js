import _ from 'underscore';

import { getChildren, getComponent } from '../../../api/components';


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
export const RECEIVE_COMPONENTS = 'RECEIVE_COMPONENTS';
export const SHOW_SOURCE = 'SHOW_SOURCE';


export function requestComponents (baseComponent) {
  return {
    type: BROWSE,
    baseComponent
  };
}


export function receiveComponents (baseComponent, components) {
  return {
    type: RECEIVE_COMPONENTS,
    baseComponent,
    components
  };
}


export function showSource (component) {
  return {
    type: SHOW_SOURCE,
    component
  };
}


function fetchChildren (dispatch, getState, baseComponent) {
  dispatch(requestComponents(baseComponent));

  const { coverageMetric } = getState();
  const metrics = [...METRICS, coverageMetric];

  return getChildren(baseComponent.key, metrics)
      .then(components => _.sortBy(components, 'name'))
      .then(components => dispatch(receiveComponents(baseComponent, components)));
}


export function initComponent (baseComponent) {
  return (dispatch, getState) => {
    return getComponent(baseComponent.key, METRICS_WITH_COVERAGE)
        .then(component => fetchChildren(dispatch, getState, component));
  };
}


export function fetchComponents (baseComponent) {
  return (dispatch, getState) => {
    const { fetching } = getState();
    if (!fetching) {
      return fetchChildren(dispatch, getState, baseComponent);
    }
  };
}
