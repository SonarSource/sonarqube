import _ from 'underscore';

import { INIT, BROWSE, SEARCH, UPDATE_QUERY, START_FETCHING, STOP_FETCHING } from '../actions';


function hasSourceCode (component) {
  return component.qualifier === 'FIL' || component.qualifier === 'UTS';
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

function merge (components, candidate) {
  const found = _.findWhere(components, { key: candidate.key });
  const newEntry = Object.assign({}, found, candidate);
  return [...(_.without(components, found)), newEntry];
}


export const initialState = {
  fetching: false,
  baseComponent: null,
  components: null,
  breadcrumbs: null,
  sourceViewer: null,
  searchResults: null,
  searchQuery: '',
  coverageMetric: null,
  baseBreadcrumbs: []
};


export function current (state = initialState, action) {
  switch (action.type) {
    case INIT:
      const coverageMetric = selectCoverageMetric(action.component);
      const baseBreadcrumbs = action.breadcrumbs.length > 1 ? _.initial(action.breadcrumbs) : [];

      return { ...state, coverageMetric, baseBreadcrumbs };
    case BROWSE:
      const baseComponent = hasSourceCode(action.component) ? null : action.component;
      const components = hasSourceCode(action.component) ? null : _.sortBy(action.children, 'name');
      const baseBreadcrumbsLength = state.baseBreadcrumbs.length;
      const breadcrumbs = action.breadcrumbs.slice(baseBreadcrumbsLength);
      const sourceViewer = hasSourceCode(action.component) ? action.component : null;

      return { ...state, baseComponent, components, breadcrumbs, sourceViewer, searchResults: null, searchQuery: '' };
    case SEARCH:
      return { ...state, searchResults: action.components };
    case UPDATE_QUERY:
      return { ...state, searchQuery: action.query };
    case START_FETCHING:
      return { ...state, fetching: true };
    case STOP_FETCHING:
      return { ...state, fetching: false };
    default:
      return state;
  }
}


export function bucket (state = [], action) {
  switch (action.type) {
    case INIT:
      return merge(state, action.component);
    case BROWSE:
      const candidate = Object.assign({}, action.component, {
        children: action.children,
        breadcrumbs: action.breadcrumbs
      });
      const nextState = merge(state, candidate);
      return action.children.reduce((currentState, nextComponent) => {
        const nextComponentWidthBreadcrumbs = Object.assign({}, nextComponent, {
          breadcrumbs: [...action.breadcrumbs, nextComponent]
        });
        return merge(currentState, nextComponentWidthBreadcrumbs);
      }, nextState);
    default:
      return state;
  }
}
