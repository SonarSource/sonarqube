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
import BranchesServiceMock from '../../api/mocks/BranchesServiceMock';
import ComponentsServiceMock from '../../api/mocks/ComponentsServiceMock';
import IssuesServiceMock from '../../api/mocks/IssuesServiceMock';
import { mockComponent } from '../../helpers/mocks/component';
import { mockCurrentUser } from '../../helpers/testMocks';
import { renderApp, renderAppWithComponentContext } from '../../helpers/testReactTestingUtils';
import { byLabelText, byPlaceholderText, byRole, byTestId } from '../../helpers/testSelector';
import { Component } from '../../types/types';
import { CurrentUser } from '../../types/users';
import IssuesApp from './components/IssuesApp';
import { projectIssuesRoutes } from './routes';

export const issuesHandler = new IssuesServiceMock();
export const componentsHandler = new ComponentsServiceMock();
export const branchHandler = new BranchesServiceMock();

export const ui = {
  loading: byLabelText('loading'),
  issuePageHeadering: byRole('heading', { level: 1, name: 'issues.page' }),
  issueItemAction1: byRole('link', { name: 'Issue with no location message' }),
  issueItemAction2: byRole('link', { name: 'FlowIssue' }),
  issueItemAction3: byRole('link', { name: 'Issue on file' }),
  issueItemAction4: byRole('link', { name: 'Fix this' }),
  issueItemAction5: byRole('link', { name: 'Fix that' }),
  issueItemAction6: byRole('link', { name: 'Second issue' }),
  issueItemAction7: byRole('link', { name: 'Issue with tags' }),
  issueItemAction8: byRole('link', { name: 'Issue on page 2' }),

  issueItems: byRole('region'),

  issueItem1: byRole('region', { name: 'Issue with no location message' }),
  issueItem2: byRole('region', { name: 'FlowIssue' }),
  issueItem3: byRole('region', { name: 'Issue on file' }),
  issueItem4: byRole('region', { name: 'Fix this' }),
  issueItem5: byRole('region', { name: 'Fix that' }),
  issueItem6: byRole('region', { name: 'Second issue' }),
  issueItem7: byRole('region', { name: 'Issue with tags' }),
  issueItem8: byRole('region', { name: 'Issue on page 2' }),
  projectIssueItem6: byRole('button', { name: 'Second issue', exact: false }),

  assigneeFacet: byRole('button', { name: 'issues.facet.assignees' }),
  authorFacet: byRole('button', { name: 'issues.facet.authors' }),
  codeVariantsFacet: byRole('button', { name: 'issues.facet.codeVariants' }),
  creationDateFacet: byRole('button', { name: 'issues.facet.createdAt' }),
  languageFacet: byRole('button', { name: 'issues.facet.languages' }),
  projectFacet: byRole('button', { name: 'issues.facet.projects' }),
  resolutionFacet: byRole('button', { name: 'issues.facet.resolutions' }),
  ruleFacet: byRole('button', { name: 'issues.facet.rules' }),
  scopeFacet: byRole('button', { name: 'issues.facet.scopes' }),
  statusFacet: byRole('button', { name: 'issues.facet.statuses' }),
  tagFacet: byRole('button', { name: 'issues.facet.tags' }),
  typeFacet: byRole('button', { name: 'issues.facet.types' }),

  clearAssigneeFacet: byTestId('clear-issues.facet.assignees'),
  clearAuthorFacet: byTestId('clear-issues.facet.authors'),
  clearCodeVariantsFacet: byTestId('clear-issues.facet.codeVariants'),
  clearCreationDateFacet: byTestId('clear-issues.facet.createdAt'),
  clearIssueTypeFacet: byTestId('clear-issues.facet.types'),
  clearProjectFacet: byTestId('clear-issues.facet.projects'),
  clearResolutionFacet: byTestId('clear-issues.facet.resolutions'),
  clearRuleFacet: byTestId('clear-issues.facet.rules'),
  clearScopeFacet: byTestId('clear-issues.facet.scopes'),
  clearSeverityFacet: byTestId('clear-issues.facet.severities'),
  clearStatusFacet: byTestId('clear-issues.facet.statuses'),
  clearTagFacet: byTestId('clear-issues.facet.tags'),

  codeSmellIssueTypeFilter: byRole('checkbox', { name: 'issue.type.CODE_SMELL' }),
  confirmedStatusFilter: byRole('checkbox', { name: 'issue.status.CONFIRMED' }),
  fixedResolutionFilter: byRole('checkbox', { name: 'issue.resolution.FIXED' }),
  mainScopeFilter: byRole('checkbox', { name: 'issue.scope.MAIN' }),
  majorSeverityFilter: byRole('checkbox', { name: 'severity.MAJOR' }),
  openStatusFilter: byRole('checkbox', { name: 'issue.status.OPEN' }),
  vulnerabilityIssueTypeFilter: byRole('checkbox', { name: 'issue.type.VULNERABILITY' }),

  clearAllFilters: byRole('button', { name: 'clear_all_filters' }),

  dateInputMonthSelect: byTestId('month-select'),
  dateInputYearSelect: byTestId('year-select'),

  authorFacetSearch: byPlaceholderText('search.search_for_authors'),
  inNewCodeFilter: byRole('checkbox', { name: 'issues.new_code' }),
  languageFacetList: byRole('list', { name: 'issues.facet.languages' }),
  ruleFacetList: byRole('list', { name: 'issues.facet.rules' }),
  ruleFacetSearch: byPlaceholderText('search.search_for_rules'),
  tagFacetSearch: byPlaceholderText('search.search_for_tags'),
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
