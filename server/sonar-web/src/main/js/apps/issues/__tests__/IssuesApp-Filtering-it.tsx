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

import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { renderOwaspTop102021Category } from '../../../helpers/security-standard';
import { mockLoggedInUser, mockRawIssue } from '../../../helpers/testMocks';
import { NoticeType } from '../../../types/users';
import IssuesList from '../components/IssuesList';
import {
  branchHandler,
  componentsHandler,
  issuesHandler,
  renderIssueApp,
  renderProjectIssuesApp,
  ui,
  waitOnDataLoaded,
} from '../test-utils';

jest.mock('../components/IssuesList', () => {
  const fakeIssueList = (props: IssuesList['props']) => {
    return (
      <>
        {props.issues.map((i) => (
          <section key={i.key} aria-label={i.message}>
            {i.message}
          </section>
        ))}
      </>
    );
  };
  return {
    __esModule: true,
    default: fakeIssueList,
  };
});

beforeEach(() => {
  issuesHandler.reset();
  componentsHandler.reset();
  branchHandler.reset();
  window.scrollTo = jest.fn();
  window.HTMLElement.prototype.scrollTo = jest.fn();
});

describe('issues app filtering', () => {
  it('should combine sidebar filters properly', async () => {
    jest.useFakeTimers();
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderIssueApp();
    await waitOnDataLoaded();

    // Select CC responsible category (should make the first issue disappear)
    await user.click(ui.responsibleCategoryFilter.get());
    expect(ui.issueItem1.query()).not.toBeInTheDocument();

    // Select responsible + Maintainability quality
    await user.click(ui.softwareQualityMaintainabilityFilter.get());
    expect(ui.issueItem5.query()).not.toBeInTheDocument();

    // Select MEDIUM severity
    await user.click(ui.severityFacet.get());
    await user.click(ui.mediumSeverityFilter.get());
    expect(ui.issueItem8.query()).not.toBeInTheDocument();

    // Expand scope and set code smells + major severity + main scope
    await user.click(ui.scopeFacet.get());
    await user.click(ui.mainScopeFilter.get());
    expect(ui.issueItem4.query()).not.toBeInTheDocument();

    // Check that filters were applied as expected
    expect(ui.issueItem6.get()).toBeInTheDocument();

    // Status
    await user.click(ui.issueStatusFacet.get());
    await user.click(ui.openStatusFilter.get());
    expect(ui.issueItem6.query()).not.toBeInTheDocument(); // Issue 6 should vanish

    // Ctrl+click on confirmed status
    await user.keyboard('{Control>}');
    await user.click(ui.confirmedStatusFilter.get());
    await user.keyboard('{/Control}');
    expect(ui.issueItem6.get()).toBeInTheDocument(); // Issue 6 should come back

    // Rule
    await user.click(ui.ruleFacet.get());
    await user.click(screen.getByRole('checkbox', { name: 'other' }));

    // Name should apply to the rule
    expect(screen.getByRole('checkbox', { name: '(HTML) Advanced rule' })).toBeInTheDocument();

    // Tag
    await user.click(ui.tagFacet.get());
    await user.type(ui.tagFacetSearch.get(), 'unu');
    await user.click(screen.getByRole('checkbox', { name: 'unused' }));

    // Project
    await user.click(ui.projectFacet.get());
    await user.click(screen.getByRole('checkbox', { name: 'org.project2' }));

    // Assignee
    await user.click(ui.assigneeFacet.get());
    await user.click(screen.getByRole('checkbox', { name: 'email2@sonarsource.com' }));
    await user.click(screen.getByRole('checkbox', { name: 'email1@sonarsource.com' })); // Change assignee

    // Author
    await user.click(ui.authorFacet.get());
    await user.type(ui.authorFacetSearch.get(), 'email');
    await user.click(screen.getByRole('checkbox', { name: 'email4@sonarsource.com' }));
    await user.click(screen.getByRole('checkbox', { name: 'email3@sonarsource.com' })); // Change author

    // Deprecated type
    await user.click(ui.typeFacet.get());
    await user.click(ui.codeSmellIssueTypeFilter.get());

    expect(ui.issueItem1.query()).not.toBeInTheDocument();
    expect(ui.issueItem2.query()).not.toBeInTheDocument();
    expect(ui.issueItem3.query()).not.toBeInTheDocument();
    expect(ui.issueItem4.query()).not.toBeInTheDocument();
    expect(ui.issueItem5.query()).not.toBeInTheDocument();
    expect(ui.issueItem6.query()).not.toBeInTheDocument();
    expect(ui.issueItem7.get()).toBeInTheDocument();

    // Clear filters one by one
    await user.click(ui.clearCodeCategoryFacet.get());
    await user.click(ui.clearSoftwareQualityFacet.get());
    await user.click(ui.clearIssueTypeFacet.get());
    await user.click(ui.clearSeverityFacet.get());
    await user.click(ui.clearScopeFacet.get());
    await user.click(ui.clearRuleFacet.get());
    await user.click(ui.clearTagFacet.get());
    await user.click(ui.clearProjectFacet.get());
    await user.click(ui.clearAssigneeFacet.get());
    await user.click(ui.clearAuthorFacet.get());
    expect(ui.issueItem1.get()).toBeInTheDocument();
    expect(ui.issueItem2.get()).toBeInTheDocument();
    expect(ui.issueItem3.get()).toBeInTheDocument();
    expect(ui.issueItem4.get()).toBeInTheDocument();
    expect(ui.issueItem5.get()).toBeInTheDocument();
    expect(ui.issueItem6.get()).toBeInTheDocument();
    expect(ui.issueItem7.get()).toBeInTheDocument();
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('should properly filter by code variants', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp();
    await waitOnDataLoaded();

    await user.click(ui.codeVariantsFacet.get());
    await user.click(screen.getByRole('checkbox', { name: /variant 1/ }));

    expect(ui.issueItem1.query()).not.toBeInTheDocument();
    expect(ui.issueItem7.get()).toBeInTheDocument();

    // Clear filter
    await user.click(ui.clearCodeVariantsFacet.get());
    expect(ui.issueItem1.get()).toBeInTheDocument();
  });

  it('should properly hide the code variants filter if no issue has any code variants', async () => {
    issuesHandler.setIssueList([
      {
        issue: mockRawIssue(),
        snippets: {},
      },
    ]);
    renderProjectIssuesApp();
    await waitOnDataLoaded();

    expect(ui.codeVariantsFacet.query()).not.toBeInTheDocument();
  });

  it('should allow to set creation date', async () => {
    const user = userEvent.setup();
    const currentUser = mockLoggedInUser({ dismissedNotices: { [NoticeType.ISSUE_GUIDE]: true } });
    issuesHandler.setCurrentUser(currentUser);

    renderIssueApp(currentUser);

    await waitOnDataLoaded();

    // Select a specific date range such that only one issue matches
    await user.click(ui.creationDateFacet.get());
    await user.click(screen.getByPlaceholderText('start_date'));

    const monthSelector = within(ui.dateInputMonthSelect.get()).getByRole('combobox');

    await user.click(monthSelector);

    await user.click(within(ui.dateInputMonthSelect.get()).getByText('Jan'));

    const yearSelector = within(ui.dateInputYearSelect.get()).getByRole('combobox');

    await user.click(yearSelector);

    await user.click(within(ui.dateInputYearSelect.get()).getAllByText('2023')[-1]);

    await user.click(screen.getByText('1', { selector: 'button' }));
    await user.click(screen.getByText('10'));

    expect(ui.issueItem1.get()).toBeInTheDocument();
    expect(ui.issueItem2.query()).not.toBeInTheDocument();
    expect(ui.issueItem3.query()).not.toBeInTheDocument();
    expect(ui.issueItem4.query()).not.toBeInTheDocument();
    expect(ui.issueItem5.query()).not.toBeInTheDocument();
    expect(ui.issueItem6.query()).not.toBeInTheDocument();
    expect(ui.issueItem7.query()).not.toBeInTheDocument();
  });

  it('should allow to only show my issues', async () => {
    const user = userEvent.setup();
    const currentUser = mockLoggedInUser({ dismissedNotices: { [NoticeType.ISSUE_GUIDE]: true } });
    issuesHandler.setCurrentUser(currentUser);
    renderIssueApp(currentUser);
    await waitOnDataLoaded();

    // By default, it should show all issues
    expect(ui.issueItem2.get()).toBeInTheDocument();
    expect(ui.issueItem3.get()).toBeInTheDocument();

    // Only show my issues
    await user.click(screen.getByRole('radio', { name: 'issues.my_issues' }));
    expect(ui.issueItem2.query()).not.toBeInTheDocument();
    expect(ui.issueItem3.get()).toBeInTheDocument();

    // Show all issues again
    await user.click(screen.getByRole('radio', { name: 'all' }));
    expect(ui.issueItem2.get()).toBeInTheDocument();
    expect(ui.issueItem3.get()).toBeInTheDocument();
  });

  it('should search for rules with proper types', async () => {
    const user = userEvent.setup();

    renderIssueApp();

    await user.click(await ui.ruleFacet.find());

    await user.type(ui.ruleFacetSearch.get(), 'rule');

    expect(within(ui.ruleFacetList.get()).getAllByRole('checkbox')).toHaveLength(2);

    expect(
      within(ui.ruleFacetList.get()).getByRole('checkbox', {
        name: /Advanced rule/,
      }),
    ).toBeInTheDocument();

    expect(
      within(ui.ruleFacetList.get()).getByRole('checkbox', {
        name: /Simple rule/,
      }),
    ).toBeInTheDocument();
  });

  it('should update collapsed facets with filter change', async () => {
    const user = userEvent.setup();

    renderIssueApp();

    await user.click(await ui.languageFacet.find());
    expect(await ui.languageFacetList.find()).toBeInTheDocument();
    expect(
      within(ui.languageFacetList.get()).getByRole('checkbox', { name: 'java' }),
    ).toHaveTextContent('java25short_number_suffix.k');
    expect(
      within(ui.languageFacetList.get()).getByRole('checkbox', { name: 'ts' }),
    ).toHaveTextContent('ts3.4short_number_suffix.k');

    await user.click(ui.languageFacet.get());
    expect(ui.languageFacetList.query()).not.toBeInTheDocument();

    await user.click(ui.responsibleCategoryFilter.get());
    await user.click(ui.languageFacet.get());
    expect(await ui.languageFacetList.find()).toBeInTheDocument();
    expect(
      within(ui.languageFacetList.get()).getByRole('checkbox', { name: 'java' }),
    ).toHaveTextContent('java111');
    expect(
      within(ui.languageFacetList.get()).getByRole('checkbox', { name: 'ts' }),
    ).toHaveTextContent('ts674');
  });

  it('should show the new code issues only', async () => {
    const user = userEvent.setup();

    renderProjectIssuesApp('project/issues?id=myproject');

    expect(await ui.issueItems.findAll()).toHaveLength(7);
    await user.click(await ui.inNewCodeFilter.find());
    expect(await ui.issueItems.findAll()).toHaveLength(6);
  });

  it('should support OWASP Top 10 version 2021', async () => {
    const user = userEvent.setup();
    renderIssueApp();
    await user.click(screen.getByRole('button', { name: 'issues.facet.standards' }));
    const owaspTop102021 = screen.getByRole('button', { name: 'issues.facet.owaspTop10_2021' });
    expect(owaspTop102021).toBeInTheDocument();

    await user.click(owaspTop102021);
    await Promise.all(
      issuesHandler.owasp2021FacetList().values.map(async ({ val }) => {
        const standard = await issuesHandler.getStandards();
        /* eslint-disable-next-line testing-library/render-result-naming-convention */
        const linkName = renderOwaspTop102021Category(standard, val);
        expect(screen.getByRole('checkbox', { name: linkName })).toBeInTheDocument();
      }),
    );
  });
});

describe('issues app when reindexing', () => {
  it('should display only some facets while reindexing is in progress', () => {
    issuesHandler.setIsAdmin(true);
    renderProjectIssuesApp(undefined, { needIssueSync: true });

    // Enabled facets
    expect(ui.inNewCodeFilter.get()).toBeInTheDocument();
    expect(ui.typeFacet.get()).toBeInTheDocument();

    // Disabled facets
    expect(ui.cleanCodeAttributeCategoryFacet.query()).not.toBeInTheDocument();
    expect(ui.softwareQualityFacet.query()).not.toBeInTheDocument();
    expect(ui.assigneeFacet.query()).not.toBeInTheDocument();
    expect(ui.authorFacet.query()).not.toBeInTheDocument();
    expect(ui.codeVariantsFacet.query()).not.toBeInTheDocument();
    expect(ui.creationDateFacet.query()).not.toBeInTheDocument();
    expect(ui.languageFacet.query()).not.toBeInTheDocument();
    expect(ui.projectFacet.query()).not.toBeInTheDocument();
    expect(ui.resolutionFacet.query()).not.toBeInTheDocument();
    expect(ui.ruleFacet.query()).not.toBeInTheDocument();
    expect(ui.scopeFacet.query()).not.toBeInTheDocument();
    expect(ui.issueStatusFacet.query()).not.toBeInTheDocument();
    expect(ui.tagFacet.query()).not.toBeInTheDocument();

    // Indexation message
    expect(screen.getByText(/indexation\.filters_unavailable/)).toBeInTheDocument();
  });
});
