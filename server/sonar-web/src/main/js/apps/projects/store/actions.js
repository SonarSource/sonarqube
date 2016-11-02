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
import groupBy from 'lodash/groupBy';
import { searchProjects } from '../../../api/components';
import { addGlobalErrorMessage } from '../../../components/store/globalMessages';
import { parseError } from '../../code/utils';
import { receiveComponents } from '../../../app/store/components/actions';
import { receiveProjects, receiveMoreProjects } from './projects/actions';
import { updateState } from './state/actions';
import { getProjectsAppState } from '../../../app/store/rootReducer';
import { getMeasuresForProjects } from '../../../api/measures';
import { receiveComponentsMeasures } from '../../../app/store/measures/actions';
import { convertToFilter } from './utils';
import { getFavorites } from '../../../api/favorites';
import { receiveFavorites } from '../../../app/store/favorites/actions';

const PAGE_SIZE = 50;

const METRICS = [
  'alert_status',
  'reliability_rating',
  'security_rating',
  'sqale_rating',
  'duplicated_lines_density',
  'coverage',
  'ncloc',
  'ncloc_language_distribution'
];

const FACETS = [
  'reliability_rating',
  'security_rating',
  'sqale_rating',
  'coverage',
  'duplicated_lines_density',
  'ncloc',
  'alert_status'
];

const onFail = dispatch => error => {
  parseError(error).then(message => dispatch(addGlobalErrorMessage(message)));
  dispatch(updateState({ loading: false }));
};

const onReceiveMeasures = dispatch => response => {
  const byComponentKey = groupBy(response.measures, 'component');

  const toStore = {};

  Object.keys(byComponentKey).forEach(componentKey => {
    const measures = {};
    byComponentKey[componentKey].forEach(measure => {
      measures[measure.metric] = measure.value;
    });
    toStore[componentKey] = measures;
  });

  dispatch(receiveComponentsMeasures(toStore));
};

const fetchProjectMeasures = projects => dispatch => {
  if (!projects.length) {
    return Promise.resolve();
  }

  const projectKeys = projects.map(project => project.key);
  return getMeasuresForProjects(projectKeys, METRICS).then(onReceiveMeasures(dispatch), onFail(dispatch));
};

const onReceiveProjects = dispatch => response => {
  dispatch(receiveComponents(response.components));
  dispatch(receiveProjects(response.components, response.facets));
  dispatch(fetchProjectMeasures(response.components)).then(() => {
    dispatch(updateState({ loading: false }));
  });
  dispatch(updateState({
    total: response.paging.total,
    pageIndex: response.paging.pageIndex,
  }));
};

const onReceiveMoreProjects = dispatch => response => {
  dispatch(receiveComponents(response.components));
  dispatch(receiveMoreProjects(response.components));
  dispatch(fetchProjectMeasures(response.components)).then(() => {
    dispatch(updateState({ loading: false }));
  });
  dispatch(updateState({ pageIndex: response.paging.pageIndex }));
};

export const fetchProjects = query => dispatch => {
  dispatch(updateState({ loading: true }));
  const data = { ps: PAGE_SIZE, facets: FACETS.join() };
  const filter = convertToFilter(query);
  if (filter) {
    data.filter = filter;
  }
  return searchProjects(data).then(onReceiveProjects(dispatch), onFail(dispatch));
};

export const fetchMoreProjects = query => (dispatch, getState) => {
  dispatch(updateState({ loading: true }));
  const state = getState();
  const { pageIndex } = getProjectsAppState(state);
  const data = { ps: PAGE_SIZE, p: pageIndex + 1 };
  const filter = convertToFilter(query);
  if (filter) {
    data.filter = filter;
  }
  return searchProjects(data).then(onReceiveMoreProjects(dispatch), onFail(dispatch));
};

export const fetchFavoriteProjects = () => dispatch => {
  dispatch(updateState({ loading: true }));

  return getFavorites().then(favorites => {
    dispatch(receiveFavorites(favorites));

    const projects = favorites.filter(component => component.qualifier === 'TRK');

    dispatch(receiveComponents(projects));
    dispatch(receiveProjects(projects, []));
    dispatch(fetchProjectMeasures(projects)).then(() => {
      dispatch(updateState({ loading: false }));
    });
    dispatch(updateState({
      total: projects.length,
      pageIndex: 1,
    }));
  }, onFail(dispatch));
};
