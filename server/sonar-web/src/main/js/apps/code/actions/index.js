import _ from 'underscore';

import { getChildren, getComponent } from '../../../api/components';


const METRICS = [
  'ncloc',
  'sqale_index',
  'violations',
  // TODO handle other types of coverage
  'coverage',
  'duplicated_lines_density'
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


function fetchChildren (dispatch, baseComponent) {
  dispatch(requestComponents(baseComponent));
  return getChildren(baseComponent.key, METRICS)
      .then(components => _.sortBy(components, 'name'))
      .then(components => dispatch(receiveComponents(baseComponent, components)));
}


export function initComponent (baseComponent) {
  return dispatch => {
    return getComponent(baseComponent.key, METRICS)
        .then(component => fetchChildren(dispatch, component));
  };
}


export function fetchComponents (baseComponent) {
  return (dispatch, getState) => {
    const { fetching } = getState();
    if (!fetching) {
      return fetchChildren(dispatch, baseComponent);
    }
  };
}
