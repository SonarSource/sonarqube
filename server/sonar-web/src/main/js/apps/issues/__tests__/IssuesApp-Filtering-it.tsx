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

import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderOwaspTop102021Category } from '../../../helpers/security-standard';
import { mockLoggedInUser, mockRawIssue } from '../../../helpers/testMocks';
import { Feature } from '../../../types/features';
import { SettingsKey } from '../../../types/settings';
import { NoticeType } from '../../../types/users';
import IssuesList from '../components/IssuesList';
import {
  branchHandler,
  componentsHandler,
  issuesHandler,
  renderIssueApp,
  renderProjectIssuesApp,
  settingsHandler,
  ui,
  usersHandler,
  waitOnDataLoaded,
} from '../test-utils';

jest.mock('../components/IssuesList', () => {
  const fakeIssueList = (props: IssuesList['props']) => {
    return (
      <>
        {props.issues.map((i) => (
          <section key={i.key} aria-label={i.message} data-moar={`scope: ${i.scope}`}>
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
  usersHandler.reset();
  settingsHandler.reset();
  window.scrollTo = jest.fn();
  window.HTMLElement.prototype.scrollTo = jest.fn();
});

describe('issues app filtering', () => {
  it('should combine sidebar filters properly', async () => {
    issuesHandler.setPageSize(50);
    const user = userEvent.setup();
    renderIssueApp(undefined, [Feature.PrioritizedRules]);
    await waitOnDataLoaded();

    // Select CC responsible category (should make the first issue disappear)
    await user.click(await ui.responsibleCategoryFilter.find());
    await waitFor(() => {
      expect(ui.issueItems.getAll()).toHaveLength(10);
    });
    expect(ui.issueItem1.query()).not.toBeInTheDocument();

    // Select responsible + Maintainability quality
    await user.click(ui.softwareQualityMaintainabilityFilter.get());
    await waitFor(() => {
      expect(ui.issueItems.getAll()).toHaveLength(9);
    });
    expect(ui.issueItem5.query()).not.toBeInTheDocument();

    // Select MEDIUM severity
    await user.click(ui.mediumSeverityFilter.get());
    await waitFor(() => {
      expect(ui.issueItems.getAll()).toHaveLength(8);
    });
    expect(ui.issueItem8.query()).not.toBeInTheDocument();

    // Expand scope and set code smells + major severity + main scope
    await user.click(ui.scopeFacet.get());
    await waitFor(() => expect(ui.mainScopeFilter.get()).toBeEnabled());
    await user.click(ui.mainScopeFilter.get());

    await waitFor(() => {
      expect(ui.issueItems.getAll()).toHaveLength(6);
    });

    expect(ui.issueItem4.query()).not.toBeInTheDocument();

    // Check that filters were applied as expected
    expect(ui.issueItem6.get()).toBeInTheDocument();

    // Status
    await user.click(ui.issueStatusFacet.get());
    await waitFor(() => expect(ui.openStatusFilter.get()).toBeEnabled());
    await user.click(ui.openStatusFilter.get());
    await waitFor(() => {
      expect(ui.issueItems.getAll()).toHaveLength(3);
    });
    expect(ui.issueItem6.query()).not.toBeInTheDocument(); // Issue 6 should vanish

    // Ctrl+click on confirmed status
    await user.keyboard('{Control>}');
    await user.click(ui.confirmedStatusFilter.get());
    await user.keyboard('{/Control}');
    expect(await ui.issueItem6.find()).toBeInTheDocument(); // Issue 6 should come back

    // Rule
    await user.click(ui.ruleFacet.get());
    await user.click(await screen.findByRole('checkbox', { name: 'other' }));

    // Name should apply to the rule
    expect(screen.getByRole('checkbox', { name: '(HTML) Advanced rule' })).toBeInTheDocument();

    // Tag
    await user.click(ui.tagFacet.get());
    await user.type(ui.tagFacetSearch.get(), 'unu');
    await user.click(screen.getByRole('checkbox', { name: 'unused' }));

    // Project
    await user.click(ui.projectFacet.get());
    expect(
      await screen.findByRole('checkbox', { name: 'org.sonarsource.javascript:javascript' }),
    ).toHaveTextContent('SonarJS');
    await user.click(
      screen.getByRole('checkbox', { name: 'org.sonarsource.javascript:javascript' }),
    );

    // Assignee
    await user.click(ui.assigneeFacet.get());
    await user.click(await screen.findByRole('checkbox', { name: 'email2@sonarsource.com' }));
    await user.click(screen.getByRole('checkbox', { name: 'email1@sonarsource.com' })); // Change assignee

    // Author
    await user.click(ui.authorFacet.get());
    await user.type(ui.authorFacetSearch.get(), 'email');
    await user.click(await screen.findByRole('checkbox', { name: 'email4@sonarsource.com' }));
    await user.click(screen.getByRole('checkbox', { name: 'email3@sonarsource.com' })); // Change author

    // No filters from standard mode
    expect(ui.typeFacet.query()).not.toBeInTheDocument();
    expect(ui.standardSeverityFacet.query()).not.toBeInTheDocument();

    // Prioritized Rule
    expect(await ui.issueItem7.find()).toBeInTheDocument();
    await user.click(ui.prioritizedRuleFacet.get());
    await waitFor(() => expect(ui.prioritizedRuleFilter.get()).toBeEnabled());
    await user.click(ui.prioritizedRuleFilter.get());

    await waitFor(() => {
      expect(ui.issueItems.getAll()).toHaveLength(1);
    });

    expect(ui.issueItem1.query()).not.toBeInTheDocument();
    expect(ui.issueItem2.query()).not.toBeInTheDocument();
    expect(ui.issueItem3.query()).not.toBeInTheDocument();
    expect(ui.issueItem4.query()).not.toBeInTheDocument();
    expect(ui.issueItem5.query()).not.toBeInTheDocument();
    expect(ui.issueItem6.query()).not.toBeInTheDocument();
    expect(ui.issueItem7.query()).not.toBeInTheDocument();
    expect(ui.issueItem10.get()).toBeInTheDocument();

    // Clear filters one by one
    await user.click(ui.clearCodeCategoryFacet.get());
    await user.click(ui.clearSoftwareQualityFacet.get());
    await user.click(ui.clearSeverityFacet.get());
    await user.click(ui.clearScopeFacet.get());
    await user.click(ui.clearRuleFacet.get());
    await user.click(ui.clearTagFacet.get());
    await user.click(ui.clearProjectFacet.get());
    await user.click(ui.clearAssigneeFacet.get());
    await user.click(ui.clearAuthorFacet.get());
    await user.click(ui.clearPrioritizedRuleFacet.get());
    expect(await ui.issueItem1.find()).toBeInTheDocument();
    expect(ui.issueItem2.get()).toBeInTheDocument();
    expect(ui.issueItem3.get()).toBeInTheDocument();
    expect(ui.issueItem4.get()).toBeInTheDocument();
    expect(ui.issueItem5.get()).toBeInTheDocument();
    expect(ui.issueItem6.get()).toBeInTheDocument();
    expect(ui.issueItem7.get()).toBeInTheDocument();
    expect(ui.issueItem10.get()).toBeInTheDocument();
  });

  it('should combine sidebar filters properly in standard mode', async () => {
    issuesHandler.setPageSize(50);
    settingsHandler.set(SettingsKey.MQRMode, 'false');
    const user = userEvent.setup();
    renderIssueApp(undefined, [Feature.PrioritizedRules]);
    await waitOnDataLoaded();

    // No MQR filters
    expect(ui.cleanCodeAttributeCategoryFacet.query()).not.toBeInTheDocument();
    expect(ui.softwareQualityFacet.query()).not.toBeInTheDocument();
    expect(ui.severityFacet.query()).not.toBeInTheDocument();

    // Select Type + severity
    await user.click(ui.codeSmellIssueTypeFilter.get());
    await user.click(ui.majorSeverityFilter.get());

    expect(ui.issueItem1.query()).not.toBeInTheDocument();
    expect(ui.issueItem2.query()).not.toBeInTheDocument();
    expect(await ui.issueItem3.find()).toBeInTheDocument();
    expect(ui.issueItem4.query()).not.toBeInTheDocument();
    expect(ui.issueItem5.get()).toBeInTheDocument();
    expect(ui.issueItem6.get()).toBeInTheDocument();
    expect(ui.issueItem7.get()).toBeInTheDocument();
    expect(ui.issueItem10.get()).toBeInTheDocument();

    // Clear filters one by one
    await user.click(ui.clearIssueTypeFacet.get());
    await user.click(ui.clearStandardSeverityFacet.get());
    expect(await ui.issueItem1.find()).toBeInTheDocument();
    expect(ui.issueItem2.get()).toBeInTheDocument();
    expect(ui.issueItem3.get()).toBeInTheDocument();
    expect(ui.issueItem4.get()).toBeInTheDocument();
    expect(ui.issueItem5.get()).toBeInTheDocument();
    expect(ui.issueItem6.get()).toBeInTheDocument();
    expect(ui.issueItem7.get()).toBeInTheDocument();
    expect(ui.issueItem10.get()).toBeInTheDocument();
  });

  it('should properly filter by code variants', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp();
    await waitOnDataLoaded();

    await user.click(await ui.codeVariantsFacet.find());
    await user.click(screen.getByRole('checkbox', { name: /variant 1/ }));

    expect(ui.issueItem1.query()).not.toBeInTheDocument();
    expect(await ui.issueItem7.find()).toBeInTheDocument();

    // Clear filter
    await user.click(ui.clearCodeVariantsFacet.get());
    expect(await ui.issueItem1.find()).toBeInTheDocument();
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
    usersHandler.setCurrentUser(currentUser);

    renderIssueApp(currentUser);

    await waitOnDataLoaded();

    // Select a specific date range such that only one issue matches
    await user.click(ui.creationDateFacet.get());
    await user.click(screen.getByPlaceholderText('start_date'));

    const monthSelector = within(ui.dateInputMonthSelect.get()).getByRole('combobox');

    await user.click(monthSelector);

    await user.click(within(ui.dateInputMonthSelect.get()).getAllByText('Jan').slice(-1)[0]);

    const yearSelector = within(ui.dateInputYearSelect.get()).getByRole('combobox');

    await user.click(yearSelector);

    await user.click(within(ui.dateInputYearSelect.get()).getAllByText('2023').slice(-1)[0]);

    await user.click(screen.getByText('1', { selector: 'button' }));
    await user.click(screen.getByText('10'));

    expect(await ui.issueItem1.find()).toBeInTheDocument();
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
    usersHandler.setCurrentUser(currentUser);
    renderIssueApp(currentUser);
    await waitOnDataLoaded();

    // By default, it should show all issues
    expect(await ui.issueItem2.find()).toBeInTheDocument();
    expect(ui.issueItem3.get()).toBeInTheDocument();

    // Only show my issues
    await user.click(screen.getByRole('radio', { name: 'issues.my_issues' }));
    expect(ui.issueItem2.query()).not.toBeInTheDocument();
    expect(ui.issueItem3.get()).toBeInTheDocument();

    // Show all issues again
    await user.click(screen.getByRole('radio', { name: 'all' }));
    expect(await ui.issueItem2.find()).toBeInTheDocument();
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
    issuesHandler.setPageSize(50);

    renderProjectIssuesApp('project/issues?id=myproject');

    expect(await ui.issueItems.findAll()).toHaveLength(11);
    await user.click(await ui.inNewCodeFilter.find());
    expect(await ui.issueItems.findAll()).toHaveLength(7);
  });

  it('should support OWASP Top 10 version 2021', async () => {
    const user = userEvent.setup();
    renderIssueApp();
    await waitOnDataLoaded();
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

  it('should close all filters if there is a filter from other mode', async () => {
    let component = renderIssueApp();
    await waitOnDataLoaded();
    expect(screen.getAllByRole('button', { expanded: true })).toHaveLength(3);

    component.unmount();

    component = renderIssueApp(undefined, undefined, 'issues?types=CODE_SMELL');
    await waitOnDataLoaded();
    expect(screen.queryByRole('button', { expanded: true })).not.toBeInTheDocument();

    component.unmount();

    settingsHandler.set(SettingsKey.MQRMode, 'false');

    renderIssueApp(undefined, undefined, 'issues?impactSeverities=BLOCKER');
    await waitOnDataLoaded();
    expect(screen.queryByRole('button', { expanded: true })).not.toBeInTheDocument();
  });
});

describe('issues app when reindexing', () => {
  it('should display only some facets while reindexing is in progress', async () => {
    issuesHandler.setIsAdmin(true);
    settingsHandler.set(SettingsKey.MQRMode, 'false');
    renderProjectIssuesApp(undefined, { needIssueSync: true });

    // Enabled facets
    expect(await ui.typeFacet.find()).toBeInTheDocument();
    expect(ui.inNewCodeFilter.get()).toBeInTheDocument();

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

    // Indexing message
    expect(screen.getByText(/indexation\.filters_unavailable/)).toBeInTheDocument();
  });
});
