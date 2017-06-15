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
import { groupBy, uniq } from 'lodash';
import { searchProjects, setProjectTags as apiSetProjectTags } from '../../../api/components';
import { addGlobalErrorMessage } from '../../../store/globalMessages/duck';
import { parseError } from '../../code/utils';
import { receiveComponents, receiveProjectTags } from '../../../store/components/actions';
import { receiveProjects, receiveMoreProjects } from './projectsDuck';
import { updateState } from './stateDuck';
import { getProjectsAppState, getComponent } from '../../../store/rootReducer';
import { getMeasuresForProjects } from '../../../api/measures';
import { receiveComponentsMeasures } from '../../../store/measures/actions';
import { convertToQueryData } from './utils';
import { receiveFavorites } from '../../../store/favorites/duck';
import { getOrganizations } from '../../../api/organizations';
import { receiveOrganizations } from '../../../store/organizations/duck';
import { isDiffMetric, getPeriodValue } from '../../../helpers/measures';

const PAGE_SIZE = 50;
const PAGE_SIZE_VISUALIZATIONS = 99;

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

const LEAK_METRICS = [
  'alert_status',
  'new_bugs',
  'new_reliability_rating',
  'new_vulnerabilities',
  'new_security_rating',
  'new_code_smells',
  'new_maintainability_rating',
  'new_coverage',
  'new_duplicated_lines_density',
  'new_lines'
];

const METRICS_BY_VISUALIZATION = {
  risk: ['reliability_rating', 'security_rating', 'coverage', 'ncloc', 'sqale_index'],
  // x, y, size, color
  reliability: ['ncloc', 'reliability_remediation_effort', 'bugs', 'reliability_rating'],
  security: ['ncloc', 'security_remediation_effort', 'vulnerabilities', 'security_rating'],
  maintainability: ['ncloc', 'sqale_index', 'code_smells', 'sqale_rating'],
  coverage: ['complexity', 'coverage', 'uncovered_lines'],
  duplications: ['ncloc', 'duplicated_lines', 'duplicated_blocks']
};

const FACETS = [
  'reliability_rating',
  'security_rating',
  'sqale_rating',
  'coverage',
  'duplicated_lines_density',
  'ncloc',
  'alert_status',
  'languages',
  'tags'
];

const LEAK_FACETS = [
  'new_reliability_rating',
  'new_security_rating',
  'new_maintainability_rating',
  'new_coverage',
  'new_duplicated_lines_density',
  'new_lines',
  'alert_status',
  'languages',
  'tags'
];

const onFail = dispatch => error => {
  parseError(error).then(message => dispatch(addGlobalErrorMessage(message)));
  dispatch(updateState({ loading: false }));
};

const onReceiveMeasures = (dispatch, expectedProjectKeys) => response => {
  const byComponentKey = groupBy(response.measures, 'component');

  const toStore = {};

  // fill store with empty objects for expected projects
  // this is required to not have "null"s for provisioned projects
  expectedProjectKeys.forEach(projectKey => (toStore[projectKey] = {}));

  Object.keys(byComponentKey).forEach(componentKey => {
    const measures = {};
    byComponentKey[componentKey].forEach(measure => {
      measures[measure.metric] = isDiffMetric(measure.metric)
        ? getPeriodValue(measure, 1)
        : measure.value;
    });
    toStore[componentKey] = measures;
  });

  dispatch(receiveComponentsMeasures(toStore));
};

const onReceiveOrganizations = dispatch => response => {
  dispatch(receiveOrganizations(response.organizations));
};

const defineMetrics = query => {
  switch (query.view) {
    case 'visualizations':
      return METRICS_BY_VISUALIZATION[query.visualization || 'risk'];
    case 'leak':
      return LEAK_METRICS;
    default:
      return METRICS;
  }
};

const defineFacets = query => {
  if (query.view === 'leak') {
    return LEAK_FACETS;
  }
  return FACETS;
};

const fetchProjectMeasures = (projects, query) => dispatch => {
  if (!projects.length) {
    return Promise.resolve();
  }

  const projectKeys = projects.map(project => project.key);
  const metrics = defineMetrics(query);
  return getMeasuresForProjects(projectKeys, metrics).then(
    onReceiveMeasures(dispatch, projectKeys),
    onFail(dispatch)
  );
};

const fetchProjectOrganizations = projects => dispatch => {
  if (!projects.length) {
    return Promise.resolve();
  }

  const organizationKeys = uniq(projects.map(project => project.organization));
  return getOrganizations(organizationKeys).then(
    onReceiveOrganizations(dispatch),
    onFail(dispatch)
  );
};

const handleFavorites = (dispatch, projects) => {
  const toAdd = projects.filter(project => project.isFavorite);
  const toRemove = projects.filter(project => project.isFavorite === false);
  if (toAdd.length || toRemove.length) {
    dispatch(receiveFavorites(toAdd, toRemove));
  }
};

const onReceiveProjects = (dispatch, query) => response => {
  dispatch(receiveComponents(response.components));
  dispatch(receiveProjects(response.components, response.facets));
  handleFavorites(dispatch, response.components);
  Promise.all([
    dispatch(fetchProjectMeasures(response.components, query)),
    dispatch(fetchProjectOrganizations(response.components))
  ]).then(() => {
    dispatch(updateState({ loading: false }));
  });
  dispatch(
    updateState({
      total: response.paging.total,
      pageIndex: response.paging.pageIndex
    })
  );
};

const onReceiveMoreProjects = (dispatch, query) => response => {
  dispatch(receiveComponents(response.components));
  dispatch(receiveMoreProjects(response.components));
  handleFavorites(dispatch, response.components);
  Promise.all([
    dispatch(fetchProjectMeasures(response.components, query)),
    dispatch(fetchProjectOrganizations(response.components))
  ]).then(() => {
    dispatch(updateState({ loading: false }));
  });
  dispatch(updateState({ pageIndex: response.paging.pageIndex }));
};

export const fetchProjects = (query, isFavorite, organization) => dispatch => {
  dispatch(updateState({ loading: true }));
  const ps = query.view === 'visualizations' ? PAGE_SIZE_VISUALIZATIONS : PAGE_SIZE;
  const data = convertToQueryData(query, isFavorite, organization, {
    ps,
    facets: defineFacets(query).join(),
    f: 'analysisDate,leakPeriodDate'
  });
  return searchProjects(data).then(onReceiveProjects(dispatch, query), onFail(dispatch));
};

export const fetchMoreProjects = (query, isFavorite, organization) => (dispatch, getState) => {
  dispatch(updateState({ loading: true }));
  const state = getState();
  const { pageIndex } = getProjectsAppState(state);
  const data = convertToQueryData(query, isFavorite, organization, {
    ps: PAGE_SIZE,
    p: pageIndex + 1,
    f: 'analysisDate,leakPeriodDate'
  });
  return searchProjects(data).then(onReceiveMoreProjects(dispatch, query), onFail(dispatch));
};

export const setProjectTags = (project, tags) => (dispatch, getState) => {
  const previousTags = getComponent(getState(), project).tags;
  dispatch(receiveProjectTags(project, tags));
  return apiSetProjectTags({ project, tags: tags.join(',') }).then(null, error => {
    dispatch(receiveProjectTags(project, previousTags));
    onFail(dispatch)(error);
  });
};
