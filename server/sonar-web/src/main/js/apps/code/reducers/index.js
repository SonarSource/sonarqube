/*
 * SonarQube :: Web
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

import { INIT, BROWSE, SEARCH, UPDATE_QUERY, SELECT_NEXT, SELECT_PREV, START_FETCHING, STOP_FETCHING,
    RAISE_ERROR } from '../actions';


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

function compare (a, b) {
  if (a === b) {
    return 0;
  }
  return a > b ? 1 : -1;
}

function sortChildren (children) {
  const QUALIFIERS_ORDER = ['DIR', 'FIL', 'UTS'];
  const temp = [...children];
  temp.sort((a, b) => {
    const qualifierA = QUALIFIERS_ORDER.indexOf(a.qualifier);
    const qualifierB = QUALIFIERS_ORDER.indexOf(b.qualifier);
    if (qualifierA !== qualifierB) {
      return compare(qualifierA, qualifierB);
    } else {
      return compare(a.name, b.name);
    }
  });
  return temp;
}

function getNext (element, list) {
  if (list) {
    const length = list.length;
    const index = list.indexOf(element);
    return index < length - 1 ? list[index + 1] : element;
  } else {
    return element;
  }
}

function getPrev (element, list) {
  if (list) {
    const index = list.indexOf(element);
    return index > 0 ? list[index - 1] : element;
  } else {
    return element;
  }
}


export const initialState = {
  fetching: false,
  baseComponent: null,
  components: null,
  breadcrumbs: null,
  sourceViewer: null,
  searchResults: null,
  searchQuery: '',
  searchSelectedItem: null,
  coverageMetric: null,
  baseBreadcrumbs: [],
  errorMessage: null
};


export function current (state = initialState, action) {
  switch (action.type) {
    case INIT:
      const coverageMetric = selectCoverageMetric(action.component);
      const baseBreadcrumbs = action.breadcrumbs.length > 1 ? _.initial(action.breadcrumbs) : [];

      return { ...state, coverageMetric, baseBreadcrumbs };
    case BROWSE:
      const baseComponent = hasSourceCode(action.component) ? null : action.component;
      const components = hasSourceCode(action.component) ? null : sortChildren(action.children);
      const baseBreadcrumbsLength = state.baseBreadcrumbs.length;
      const breadcrumbs = action.breadcrumbs.slice(baseBreadcrumbsLength);
      const sourceViewer = hasSourceCode(action.component) ? action.component : null;

      return {
        ...state,
        baseComponent,
        components,
        breadcrumbs,
        sourceViewer,
        searchResults: null,
        searchQuery: '',
        searchSelectedItem: null,
        errorMessage: null
      };
    case SEARCH:
      return {
        ...state,
        searchResults: action.components,
        searchSelectedItem: _.first(action.components),
        sourceViewer: null,
        errorMessage: null
      };
    case UPDATE_QUERY:
      return { ...state, searchQuery: action.query };
    case SELECT_NEXT:
      return {
        ...state,
        searchSelectedItem: getNext(state.searchSelectedItem, state.searchResults)
      };
    case SELECT_PREV:
      return {
        ...state,
        searchSelectedItem: getPrev(state.searchSelectedItem, state.searchResults)
      };
    case START_FETCHING:
      return { ...state, fetching: true };
    case STOP_FETCHING:
      return { ...state, fetching: false };
    case RAISE_ERROR:
      return {
        ...state,
        errorMessage: action.message,
        fetching: false
      };
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
