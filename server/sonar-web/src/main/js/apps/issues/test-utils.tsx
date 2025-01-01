/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { Outlet, Route } from 'react-router-dom';
import { byPlaceholderText, byRole, byTestId, byText } from '~sonar-aligned/helpers/testSelector';
import BranchesServiceMock from '../../api/mocks/BranchesServiceMock';
import ComponentsServiceMock from '../../api/mocks/ComponentsServiceMock';
import CveServiceMock from '../../api/mocks/CveServiceMock';
import FixSuggestionsServiceMock from '../../api/mocks/FixSuggestionsServiceMock';
import IssuesServiceMock from '../../api/mocks/IssuesServiceMock';
import { ModeServiceMock } from '../../api/mocks/ModeServiceMock';
import SourcesServiceMock from '../../api/mocks/SourcesServiceMock';
import UsersServiceMock from '../../api/mocks/UsersServiceMock';
import { mockComponent } from '../../helpers/mocks/component';
import { mockCurrentUser } from '../../helpers/testMocks';
import { renderApp, renderAppWithComponentContext } from '../../helpers/testReactTestingUtils';
import {
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../types/clean-code-taxonomy';
import { Feature } from '../../types/features';
import { IssueSeverity } from '../../types/issues';
import { Component } from '../../types/types';
import { NoticeType } from '../../types/users';
import IssuesApp from './components/IssuesApp';
import { projectIssuesRoutes } from './routes';

export const usersHandler = new UsersServiceMock();
export const issuesHandler = new IssuesServiceMock(usersHandler);
export const cveHandler = new CveServiceMock();
export const componentsHandler = new ComponentsServiceMock();
export const sourcesHandler = new SourcesServiceMock();
export const branchHandler = new BranchesServiceMock();
export const fixIssueHandler = new FixSuggestionsServiceMock();
export const modeHandler = new ModeServiceMock();

export const ui = {
  loading: byText('issues.loading_issues'),
  fixGenerated: byText('issues.code_fix.fix_is_being_generated'),
  noFixAvailable: byText('issues.code_fix.something_went_wrong'),
  suggestedExplanation: byText(fixIssueHandler.fixSuggestion.explanation),
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

  fixedIssuesHeading: byRole('heading', { level: 2, name: 'issues.fixed_issues' }),

  issueItem1: byRole('region', { name: 'Issue with no location message' }),
  issueItem2: byRole('region', { name: 'FlowIssue' }),
  issueItem3: byRole('region', { name: 'Issue on file' }),
  issueItem4: byRole('region', { name: 'Fix this' }),
  issueItem5: byRole('region', { name: 'Fix that' }),
  issueItem6: byRole('region', { name: 'Second issue' }),
  issueItem7: byRole('region', { name: 'Issue with tags' }),
  issueItem8: byRole('region', { name: 'Issue on page 2' }),
  issueItem9: byRole('region', { name: 'Issue inside folderA' }),
  issueItem10: byRole('region', { name: 'Issue with prioritized rule' }),
  projectIssueItem6: byRole('button', { name: 'Second issue' }),

  conciseIssueTotal: byTestId('page-counter-total'),
  conciseIssueItem2: byTestId('issues-nav-bar').byRole('button', { name: 'Fix that' }),
  conciseIssueItem4: byTestId('issues-nav-bar').byRole('button', { name: 'Issue with tags' }),

  assigneeFacet: byRole('button', { name: 'issues.facet.assignees' }),
  authorFacet: byRole('button', { name: 'issues.facet.authors' }),
  codeVariantsFacet: byRole('button', { name: 'issues.facet.codeVariants' }),
  creationDateFacet: byRole('button', { name: 'issues.facet.createdAt' }),
  languageFacet: byRole('button', { name: 'issues.facet.languages' }),
  projectFacet: byRole('button', { name: 'issues.facet.projects' }),
  resolutionFacet: byRole('button', { name: 'issues.facet.resolutions' }),
  ruleFacet: byRole('button', { name: 'issues.facet.rules' }),
  scopeFacet: byRole('button', { name: 'issues.facet.scopes' }),
  issueStatusFacet: byRole('button', { name: 'issues.facet.issueStatuses' }),
  tagFacet: byRole('button', { name: 'issues.facet.tags' }),
  typeFacet: byRole('button', { name: 'issues.facet.types' }),
  getFixSuggestion: byRole('button', { name: 'issues.code_fix.get_fix_suggestion' }),
  getAFixSuggestion: byRole('button', { name: 'issues.code_fix.get_a_fix_suggestion' }),

  seeFixSuggestion: byRole('button', { name: 'issues.code_fix.see_fix_suggestion' }),
  cleanCodeAttributeCategoryFacet: byRole('button', {
    name: 'issues.facet.cleanCodeAttributeCategories',
  }),
  softwareQualityFacet: byRole('button', {
    name: 'issues.facet.impactSoftwareQualities',
  }),
  severityFacet: byRole('button', { name: 'coding_rules.facet.impactSeverities' }),
  standardSeverityFacet: byRole('button', { name: 'issues.facet.severities' }),
  prioritizedRuleFacet: byRole('button', { name: 'issues.facet.prioritized_rule.category' }),

  clearCodeCategoryFacet: byTestId('clear-issues.facet.cleanCodeAttributeCategories'),
  clearSoftwareQualityFacet: byTestId('clear-issues.facet.impactSoftwareQualities'),
  clearAssigneeFacet: byTestId('clear-issues.facet.assignees'),
  clearAuthorFacet: byTestId('clear-issues.facet.authors'),
  clearCodeVariantsFacet: byTestId('clear-issues.facet.codeVariants'),
  clearCreationDateFacet: byTestId('clear-issues.facet.createdAt'),
  clearIssueTypeFacet: byTestId('clear-issues.facet.types'),
  clearProjectFacet: byTestId('clear-issues.facet.projects'),
  clearResolutionFacet: byTestId('clear-issues.facet.resolutions'),
  clearRuleFacet: byTestId('clear-issues.facet.rules'),
  clearScopeFacet: byTestId('clear-issues.facet.scopes'),
  clearSeverityFacet: byTestId('clear-coding_rules.facet.impactSeverities'),
  clearStandardSeverityFacet: byTestId('clear-issues.facet.severities'),
  clearIssueStatusFacet: byTestId('clear-issues.facet.issueStatuses'),
  clearTagFacet: byTestId('clear-issues.facet.tags'),
  clearPrioritizedRuleFacet: byTestId('clear-issues.facet.prioritized_rule.category'),

  responsibleCategoryFilter: byRole('checkbox', {
    name: `issue.clean_code_attribute_category.${CleanCodeAttributeCategory.Responsible}`,
  }),
  consistentCategoryFilter: byRole('checkbox', {
    name: `issue.clean_code_attribute_category.${CleanCodeAttributeCategory.Consistent}`,
  }),
  softwareQualityMaintainabilityFilter: byRole('checkbox', {
    name: `software_quality.${SoftwareQuality.Maintainability}`,
  }),
  codeSmellIssueTypeFilter: byRole('checkbox', { name: 'issue.type.CODE_SMELL' }),
  confirmedStatusFilter: byRole('checkbox', { name: 'issue.issue_status.CONFIRMED' }),
  fixedResolutionFilter: byRole('checkbox', { name: 'issue.resolution.FIXED' }),
  mainScopeFilter: byRole('checkbox', { name: 'issue.scope.MAIN' }),
  mediumSeverityFilter: byRole('checkbox', {
    name: `severity_impact.${SoftwareImpactSeverity.Medium}`,
  }),
  majorSeverityFilter: byRole('checkbox', {
    name: `severity.${IssueSeverity.Major}`,
  }),
  openStatusFilter: byRole('checkbox', { name: 'issue.issue_status.OPEN' }),
  vulnerabilityIssueTypeFilter: byRole('checkbox', { name: 'issue.type.VULNERABILITY' }),
  prioritizedRuleFilter: byRole('checkbox', { name: 'issues.facet.prioritized_rule' }),

  cveTable: byRole('table', { name: 'rule.cve_details' }),

  bulkChangeComment: byRole('textbox', { name: /issue_bulk_change.resolution_comment/ }),

  clearAllFilters: byRole('button', { name: 'clear_all_filters' }),

  dateInputMonthSelect: byTestId('month-select'),
  dateInputYearSelect: byTestId('year-select'),

  authorFacetSearch: byPlaceholderText('search.search_for_authors'),
  inNewCodeFilter: byRole('checkbox', { name: 'issues.new_code' }),
  languageFacetList: byRole('group', { name: 'issues.facet.languages' }),
  ruleFacetList: byRole('group', { name: 'issues.facet.rules' }),
  ruleFacetSearch: byPlaceholderText('search.search_for_rules'),
  tagFacetSearch: byPlaceholderText('search.search_for_tags'),

  issueCodeFixTab: byRole('tab', { name: 'coding_rules.description_section.title.code_fix' }),
  issueCodeTab: byRole('tab', { name: 'issue.tabs.code' }),
  issueActivityTab: byRole('tab', { name: 'coding_rules.description_section.title.activity' }),
  issueActivityAddComment: byRole('button', {
    name: `issue.activity.add_comment`,
  }),
  issueAcitivityEditComment: byRole('button', { name: 'issue.comment.edit' }),
  issueActivityDeleteComment: byRole('button', { name: 'issue.comment.delete' }),

  guidePopup: byRole('alertdialog'),
};

export async function waitOnDataLoaded() {
  await waitFor(() => expect(ui.loading.query()).not.toBeInTheDocument());
}

export function renderIssueApp(
  currentUser = mockCurrentUser({
    dismissedNotices: {
      [NoticeType.ISSUE_GUIDE]: true,
      [NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE]: true,
    },
  }),
  featureList: Feature[] = [],
  navigateTo?: string,
) {
  return renderApp('issues', <IssuesApp />, {
    currentUser,
    featureList,
    navigateTo,
  });
}

export function renderProjectIssuesApp(
  navigateTo?: string,
  overrides?: Partial<Component>,
  currentUser = mockCurrentUser({
    dismissedNotices: {
      [NoticeType.ISSUE_GUIDE]: true,
      [NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE]: true,
    },
  }),
  featureList = [Feature.BranchSupport],
) {
  return renderAppWithComponentContext(
    'project/issues',
    () => (
      <Route
        element={
          <div data-guiding-id="issue-5">
            <Outlet />
          </div>
        }
      >
        {projectIssuesRoutes()}
      </Route>
    ),
    { navigateTo, currentUser, featureList },
    { component: mockComponent(overrides) },
  );
}
