/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { waitFor } from '@testing-library/react';
import React from 'react';
import { byLabelText, byRole } from 'testing-library-selector';
import ComponentsServiceMock from '../../api/mocks/ComponentsServiceMock';
import IssuesServiceMock from '../../api/mocks/IssuesServiceMock';
import { mockComponent } from '../../helpers/mocks/component';
import { mockCurrentUser } from '../../helpers/testMocks';
import { renderApp, renderAppWithComponentContext } from '../../helpers/testReactTestingUtils';
import { Component } from '../../types/types';
import { CurrentUser } from '../../types/users';
import IssuesApp from './components/IssuesApp';
import { projectIssuesRoutes } from './routes';

jest.mock('../../api/issues');
jest.mock('../../api/rules');
jest.mock('../../api/components');
jest.mock('../../api/users');

export const issuesHandler = new IssuesServiceMock();
export const componentsHandler = new ComponentsServiceMock();

export const ui = {
  loading: byLabelText('loading'),
  issueItems: byRole('region'),

  issueItem1: byRole('region', { name: 'Issue with no location message' }),
  issueItem2: byRole('region', { name: 'FlowIssue' }),
  issueItem3: byRole('region', { name: 'Issue on file' }),
  issueItem4: byRole('region', { name: 'Fix this' }),
  issueItem5: byRole('region', { name: 'Fix that' }),
  issueItem6: byRole('region', { name: 'Second issue' }),
  issueItem7: byRole('region', { name: 'Issue with tags' }),
  issueItem8: byRole('region', { name: 'Issue on page 2' }),

  clearIssueTypeFacet: byRole('button', { name: 'clear_x_filter.issues.facet.types' }),
  codeSmellIssueTypeFilter: byRole('checkbox', { name: 'issue.type.CODE_SMELL' }),
  vulnerabilityIssueTypeFilter: byRole('checkbox', { name: 'issue.type.VULNERABILITY' }),
  clearSeverityFacet: byRole('button', { name: 'clear_x_filter.issues.facet.severities' }),
  majorSeverityFilter: byRole('checkbox', { name: 'severity.MAJOR' }),
  scopeFacet: byRole('button', { name: 'issues.facet.scopes' }),
  clearScopeFacet: byRole('button', { name: 'clear_x_filter.issues.facet.scopes' }),
  mainScopeFilter: byRole('checkbox', { name: 'issue.scope.MAIN' }),
  resolutionFacet: byRole('button', { name: 'issues.facet.resolutions' }),
  clearResolutionFacet: byRole('button', { name: 'clear_x_filter.issues.facet.resolutions' }),
  fixedResolutionFilter: byRole('checkbox', { name: 'issue.resolution.FIXED' }),
  statusFacet: byRole('button', { name: 'issues.facet.statuses' }),
  creationDateFacet: byRole('button', { name: 'issues.facet.createdAt' }),
  clearCreationDateFacet: byRole('button', { name: 'clear_x_filter.issues.facet.createdAt' }),
  clearStatusFacet: byRole('button', { name: 'clear_x_filter.issues.facet.statuses' }),
  openStatusFilter: byRole('checkbox', { name: 'issue.status.OPEN' }),
  confirmedStatusFilter: byRole('checkbox', { name: 'issue.status.CONFIRMED' }),
  languageFacet: byRole('button', { name: 'issues.facet.languages' }),
  ruleFacet: byRole('button', { name: 'issues.facet.rules' }),
  clearRuleFacet: byRole('button', { name: 'clear_x_filter.issues.facet.rules' }),
  tagFacet: byRole('button', { name: 'issues.facet.tags' }),
  clearTagFacet: byRole('button', { name: 'clear_x_filter.issues.facet.tags' }),
  projectFacet: byRole('button', { name: 'issues.facet.projects' }),
  clearProjectFacet: byRole('button', { name: 'clear_x_filter.issues.facet.projects' }),
  assigneeFacet: byRole('button', { name: 'issues.facet.assignees' }),
  clearAssigneeFacet: byRole('button', { name: 'clear_x_filter.issues.facet.assignees' }),
  authorFacet: byRole('button', { name: 'issues.facet.authors' }),
  clearAuthorFacet: byRole('button', { name: 'clear_x_filter.issues.facet.authors' }),

  dateInputMonthSelect: byRole('combobox', { name: 'Month:' }),
  dateInputYearSelect: byRole('combobox', { name: 'Year:' }),

  clearAllFilters: byRole('button', { name: 'clear_all_filters' }),

  ruleFacetList: byRole('list', { name: 'rules' }),
  languageFacetList: byRole('list', { name: 'languages' }),
  ruleFacetSearch: byRole('searchbox', { name: 'search.search_for_rules' }),
};

export async function waitOnDataLoaded() {
  await waitFor(() => {
    expect(ui.loading.query()).not.toBeInTheDocument();
  });
}

export function renderIssueApp(currentUser?: CurrentUser) {
  renderApp('project/issues', <IssuesApp />, { currentUser: mockCurrentUser(currentUser) });
}

export function renderProjectIssuesApp(navigateTo?: string, overrides?: Partial<Component>) {
  renderAppWithComponentContext(
    'project/issues',
    projectIssuesRoutes,
    { navigateTo },
    { component: mockComponent(overrides) }
  );
}
