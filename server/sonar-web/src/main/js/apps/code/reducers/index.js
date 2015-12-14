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


const rootReducer = combineReducers({
  fetching,
  baseComponent,
  components,
  breadcrumbs,
  sourceViewer
});


export default rootReducer;
