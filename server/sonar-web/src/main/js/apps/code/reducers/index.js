import _ from 'underscore';
import { combineReducers } from 'redux';

import { BROWSE, RECEIVE_COMPONENTS, SHOW_SOURCE } from '../actions';


export function fetching (state = false, action) {
  switch (action.type) {
    case BROWSE:
      return true;
    case RECEIVE_COMPONENTS:
      return false;
    default:
      return state;
  }
}


export function baseComponent (state = null, action) {
  switch (action.type) {
    case RECEIVE_COMPONENTS:
      return action.baseComponent;
    default:
      return state;
  }
}


export function components (state = null, action) {
  switch (action.type) {
    case RECEIVE_COMPONENTS:
      return action.components;
    default:
      return state;
  }
}


export function breadcrumbs (state = [], action) {
  switch (action.type) {
    case BROWSE:
      const existedIndex = state.findIndex(b => b.key === action.baseComponent.key);
      let nextBreadcrumbs;

      if (existedIndex === -1) {
        // browse deeper
        nextBreadcrumbs = [...state, action.baseComponent];
      } else {
        // use breadcrumbs
        nextBreadcrumbs = [...state.slice(0, existedIndex + 1)];
      }

      return nextBreadcrumbs;
    case SHOW_SOURCE:
      return [...state, action.component];
    default:
      return state;
  }
}


export function sourceViewer (state = null, action) {
  switch (action.type) {
    case BROWSE:
      return null;
    case SHOW_SOURCE:
      return action.component;
    default:
      return state;
  }
}


function selectCoverageMetric (component) {
  const coverage = _.findWhere(component.msr, { key: 'coverage' });
  const itCoverage = _.findWhere(component.msr, { key: 'it_coverage' });
  const overallCoverage = _.findWhere(component.msr, { key: 'overall_coverage' });

  if (coverage != null && itCoverage != null && overallCoverage != null) {
    return 'overall_coverage';
  } else if (coverage != null) {
    return 'coverage';
  } else {
    return 'it_coverage';
  }
}


export function coverageMetric (state = null, action) {
  switch (action.type) {
    case BROWSE:
      return state !== null ? state : selectCoverageMetric(action.baseComponent);
    default:
      return state;
  }
}


const rootReducer = combineReducers({
  fetching,
  baseComponent,
  components,
  breadcrumbs,
  sourceViewer,
  coverageMetric
});


export default rootReducer;
