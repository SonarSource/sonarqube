/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import { flatMap, sumBy } from 'lodash';
import { createMap } from '../../../store/utils/generalReducers';
import { actions } from './projectsDuck';
import { mapMetricToProperty } from './utils';

const CUMULATIVE_FACETS = [
  'reliability',
  'new_reliability',
  'security',
  'new_security',
  'maintainability',
  'new_maintainability',
  'coverage',
  'new_coverage',
  'duplications',
  'new_duplications',
  'size',
  'new_lines'
];

const REVERSED_FACETS = ['coverage', 'new_coverage'];

const mapFacetValues = values => {
  const map = {};
  values.forEach(value => {
    map[value.val] = value.count;
  });
  return map;
};

const cumulativeMapFacetValues = values => {
  const map = {};
  let sum = sumBy(values, value => value.count);
  values.forEach((value, index) => {
    map[value.val] = index > 0 && index < values.length - 1 ? sum : value.count;
    sum -= value.count;
  });
  return map;
};

const getFacetsMap = facets => {
  const map = {};
  facets.forEach(facet => {
    const property = mapMetricToProperty(facet.property);
    const { values } = facet;
    if (REVERSED_FACETS.includes(property)) {
      values.reverse();
    }
    map[property] = CUMULATIVE_FACETS.includes(property)
      ? cumulativeMapFacetValues(values)
      : mapFacetValues(values);
  });
  return map;
};

const reducer = createMap(
  (state, action) => action.type === actions.RECEIVE_PROJECTS,
  () => false,
  (state, action) => getFacetsMap(action.facets)
);

export default reducer;

export const getFacetByProperty = (state, property) => state[property];

export const getMaxFacetValue = state => {
  const allValues = flatMap(Object.values(state), facet => Object.values(facet));
  return Math.max.apply(null, allValues);
};
