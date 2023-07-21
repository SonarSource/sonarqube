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

import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import selectEvent from 'react-select-event';
import { TabKeys } from '../../../components/rules/RuleTabViewer';
import { renderOwaspTop102021Category } from '../../../helpers/security-standard';
import { mockLoggedInUser, mockRawIssue } from '../../../helpers/testMocks';
import { ComponentQualifier } from '../../../types/component';
import { IssueType } from '../../../types/issues';
import {
  componentsHandler,
  issuesHandler,
  renderIssueApp,
  renderProjectIssuesApp,
  ui,
  waitOnDataLoaded,
} from '../test-utils';

beforeEach(() => {
  issuesHandler.reset();
  componentsHandler.reset();
  window.scrollTo = jest.fn();
  window.HTMLElement.prototype.scrollTo = jest.fn();
});

describe('issues app', () => {
  describe('rendering', () => {
    it('should show warning when not all issues are accessible', async () => {
      const user = userEvent.setup();
      renderProjectIssuesApp('project/issues?id=myproject', {
        canBrowseAllChildProjects: false,
        qualifier: ComponentQualifier.Portfolio,
      });
      expect(screen.getByText('issues.not_all_issue_show')).toBeInTheDocument();

      await act(async () => {
        await user.keyboard('{ArrowRight}');
      });

      expect(screen.getByText('issues.not_all_issue_show')).toBeInTheDocument();
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
        })
      );
    });
  });

  describe('navigation', () => {
    it('should handle keyboard navigation in list and open / close issues', async () => {
      const user = userEvent.setup();
      renderIssueApp();

      // Navigate to 2nd issue
      await user.keyboard('{ArrowDown}');

      // Select it
      await act(async () => {
        await user.keyboard('{ArrowRight}');
      });
      expect(
        screen.getByRole('heading', { name: issuesHandler.list[1].issue.message })
      ).toBeInTheDocument();

      // Go back
      await act(async () => {
        await user.keyboard('{ArrowLeft}');
      });
      expect(
        screen.queryByRole('heading', { name: issuesHandler.list[1].issue.message })
      ).not.toBeInTheDocument();

      // Navigate to 1st issue and select it
      await user.keyboard('{ArrowUp}');
      await user.keyboard('{ArrowUp}');
      await act(async () => {
        await user.keyboard('{ArrowRight}');
      });
      expect(
        screen.getByRole('heading', { name: issuesHandler.list[0].issue.message })
      ).toBeInTheDocument();
    });

    it('should open issue and navigate', async () => {
      const user = userEvent.setup();
      renderIssueApp();

      // Select an issue with an advanced rule
      await act(async () => {
        await user.click(await screen.findByRole('link', { name: 'Fix that' }));
      });
      expect(screen.getByRole('tab', { name: 'issue.tabs.code' })).toBeInTheDocument();

      // Are rule headers present?
      expect(screen.getByRole('heading', { level: 1, name: 'Fix that' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'advancedRuleId' })).toBeInTheDocument();

      // Select the "why is this an issue" tab and check its content
      await act(async () => {
        await user.click(
          screen.getByRole('tab', { name: `coding_rules.description_section.title.root_cause` })
        );
      });
      expect(screen.getByRole('heading', { name: 'Because' })).toBeInTheDocument();

      // Select the "how to fix it" tab
      await act(async () => {
        await user.click(
          screen.getByRole('tab', { name: `coding_rules.description_section.title.how_to_fix` })
        );
      });

      // Is the context selector present with the expected values and default selection?
      expect(screen.getByRole('radio', { name: 'Context 2' })).toBeInTheDocument();
      expect(screen.getByRole('radio', { name: 'Context 3' })).toBeInTheDocument();
      expect(screen.getByRole('radio', { name: 'Spring' })).toBeInTheDocument();
      expect(
        screen.getByRole('radio', { name: 'coding_rules.description_context.other' })
      ).toBeInTheDocument();
      expect(screen.getByRole('radio', { name: 'Spring', current: true })).toBeInTheDocument();

      // Select context 2 and check tab content
      await act(async () => {
        await user.click(screen.getByRole('radio', { name: 'Context 2' }));
      });
      expect(screen.getByText('Context 2 content')).toBeInTheDocument();

      // Select the "other" context and check tab content
      await act(async () => {
        await user.click(
          screen.getByRole('radio', { name: 'coding_rules.description_context.other' })
        );
      });
      expect(screen.getByText('coding_rules.context.others.title')).toBeInTheDocument();
      expect(screen.getByText('coding_rules.context.others.description.first')).toBeInTheDocument();
      expect(
        screen.getByText('coding_rules.context.others.description.second')
      ).toBeInTheDocument();

      // Select the main info tab and check its content
      await act(async () => {
        await user.click(
          screen.getByRole('tab', { name: `coding_rules.description_section.title.more_info` })
        );
      });
      expect(screen.getByRole('heading', { name: 'Link' })).toBeInTheDocument();

      // Check for extended description (eslint FP)
      // eslint-disable-next-line jest-dom/prefer-in-document
      expect(screen.getAllByText('Extended Description')).toHaveLength(1);

      // Select the previous issue (with a simple rule) through keyboard shortcut
      await act(async () => {
        await user.keyboard('{ArrowUp}');
      });

      // Are rule headers present?
      expect(screen.getByRole('heading', { level: 1, name: 'Fix this' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'simpleRuleId' })).toBeInTheDocument();

      // Select the "why is this an issue tab" and check its content
      await act(async () => {
        await user.click(
          screen.getByRole('tab', { name: `coding_rules.description_section.title.root_cause` })
        );
      });
      expect(screen.getByRole('heading', { name: 'Default' })).toBeInTheDocument();

      // Select the previous issue (with a simple rule) through keyboard shortcut
      await act(async () => {
        await user.keyboard('{ArrowUp}');
      });

      // Are rule headers present?
      expect(screen.getByRole('heading', { level: 1, name: 'Issue on file' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'simpleRuleId' })).toBeInTheDocument();

      // The "Where is the issue" tab should be selected by default. Check its content
      expect(screen.getAllByRole('button', { name: 'Issue on file', exact: false })).toHaveLength(
        2
      ); // there will be 2 buttons one in concise issue and other in code viewer
      expect(
        screen.getByRole('row', {
          name: '2 * SonarQube',
        })
      ).toBeInTheDocument();
    });

    it('should be able to navigate to other issue located in the same file', async () => {
      const user = userEvent.setup();
      renderIssueApp();

      await act(async () => {
        await user.click(await ui.issueItemAction5.find());
      });
      expect(ui.projectIssueItem6.getAll()).toHaveLength(2); // there will be 2 buttons one in concise issue and other in code viewer

      await act(async () => {
        await user.click(ui.projectIssueItem6.getAll()[1]);
      });
      expect(screen.getByRole('heading', { level: 1, name: 'Second issue' })).toBeInTheDocument();
    });

    it('should be able to show more issues', async () => {
      const user = userEvent.setup();
      renderIssueApp();

      expect(await ui.issueItems.findAll()).toHaveLength(7);
      expect(ui.issueItem8.query()).not.toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: 'show_more' }));
      expect(ui.issueItems.getAll()).toHaveLength(10);
      expect(ui.issueItem8.get()).toBeInTheDocument();
    });

    // Improve this to include all the bulk change fonctionality
    it('should be able to bulk change', async () => {
      const user = userEvent.setup();
      const currentUser = mockLoggedInUser();
      issuesHandler.setIsAdmin(true);
      issuesHandler.setCurrentUser(currentUser);
      renderIssueApp(currentUser);

      // Check that the bulk button has correct behavior
      expect(screen.getByRole('button', { name: 'bulk_change' })).toBeDisabled();
      await user.click(screen.getByRole('checkbox', { name: 'issues.select_all_issues' }));
      expect(
        screen.getByRole('button', { name: 'issues.bulk_change_X_issues.10' })
      ).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: 'issues.bulk_change_X_issues.10' }));
      await user.click(screen.getByRole('button', { name: 'cancel' }));
      expect(screen.getByRole('button', { name: 'issues.bulk_change_X_issues.10' })).toHaveFocus();
      await user.click(screen.getByRole('checkbox', { name: 'issues.select_all_issues' }));

      // Check that we bulk change the selected issue
      const issueBoxFixThat = within(screen.getByRole('region', { name: 'Fix that' }));

      expect(
        issueBoxFixThat.getByLabelText('issue.type.type_x_click_to_change.issue.type.CODE_SMELL')
      ).toBeInTheDocument();

      await user.click(
        screen.getByRole('checkbox', { name: 'issues.action_select.label.Fix that' })
      );
      await user.click(screen.getByRole('button', { name: 'issues.bulk_change_X_issues.1' }));

      await user.click(screen.getByRole('textbox', { name: /issue.comment.formlink/ }));
      await user.keyboard('New Comment');
      expect(screen.getByRole('button', { name: 'apply' })).toBeDisabled();

      await selectEvent.select(screen.getByRole('combobox', { name: 'issue.set_type' }), [
        'issue.type.BUG',
      ]);
      await user.click(screen.getByRole('button', { name: 'apply' }));

      expect(
        issueBoxFixThat.getByLabelText('issue.type.type_x_click_to_change.issue.type.BUG')
      ).toBeInTheDocument();
    });
  });

  describe('filtering', () => {
    it('should combine sidebar filters properly', async () => {
      const user = userEvent.setup();
      renderIssueApp();
      await waitOnDataLoaded();

      // Select only code smells (should make the first issue disappear)
      await user.click(ui.codeSmellIssueTypeFilter.get());

      // Select code smells + major severity
      await user.click(ui.majorSeverityFilter.get());

      // Expand scope and set code smells + major severity + main scope
      await user.click(ui.scopeFacet.get());
      await user.click(ui.mainScopeFilter.get());

      // Resolution
      await user.click(ui.resolutionFacet.get());
      await user.click(ui.fixedResolutionFilter.get());

      // Stop to check that filters were applied as expected
      expect(ui.issueItem1.query()).not.toBeInTheDocument();
      expect(ui.issueItem2.query()).not.toBeInTheDocument();
      expect(ui.issueItem3.query()).not.toBeInTheDocument();
      expect(ui.issueItem4.query()).not.toBeInTheDocument();
      expect(ui.issueItem5.query()).not.toBeInTheDocument();
      expect(ui.issueItem6.get()).toBeInTheDocument();
      expect(ui.issueItem7.query()).not.toBeInTheDocument();

      // Status
      await user.click(ui.statusFacet.get());

      await user.click(ui.openStatusFilter.get());
      expect(ui.issueItem6.query()).not.toBeInTheDocument(); // Issue 6 should vanish

      // Ctrl+click on confirmed status
      await user.keyboard('{Control>}');
      await user.click(ui.confirmedStatusFilter.get());
      await user.keyboard('{/Control}');
      expect(ui.issueItem6.get()).toBeInTheDocument(); // Issue 6 should come back

      // Clear resolution filter
      await user.click(ui.clearResolutionFacet.get());

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
      expect(ui.issueItem1.query()).not.toBeInTheDocument();
      expect(ui.issueItem2.query()).not.toBeInTheDocument();
      expect(ui.issueItem3.query()).not.toBeInTheDocument();
      expect(ui.issueItem4.query()).not.toBeInTheDocument();
      expect(ui.issueItem5.query()).not.toBeInTheDocument();
      expect(ui.issueItem6.query()).not.toBeInTheDocument();
      expect(ui.issueItem7.get()).toBeInTheDocument();

      // Clear filters one by one
      await user.click(ui.clearIssueTypeFacet.get());
      await user.click(ui.clearSeverityFacet.get());
      await user.click(ui.clearScopeFacet.get());
      await user.click(ui.clearStatusFacet.get());
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
      const currentUser = mockLoggedInUser();
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
      const currentUser = mockLoggedInUser();
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
        })
      ).toBeInTheDocument();

      expect(
        within(ui.ruleFacetList.get()).getByRole('checkbox', {
          name: /Simple rule/,
        })
      ).toBeInTheDocument();

      await user.click(ui.vulnerabilityIssueTypeFilter.get());
      // after changing the issue type filter, search field is reset, so we type again
      await user.type(ui.ruleFacetSearch.get(), 'rule');

      expect(
        within(ui.ruleFacetList.get()).getByRole('checkbox', {
          name: /Advanced rule/,
        })
      ).toBeInTheDocument();
      expect(
        within(ui.ruleFacetList.get()).queryByRole('checkbox', {
          name: /Simple rule/,
        })
      ).not.toBeInTheDocument();
    });

    it('should update collapsed facets with filter change', async () => {
      const user = userEvent.setup();

      renderIssueApp();

      await user.click(await ui.languageFacet.find());
      expect(await ui.languageFacetList.find()).toBeInTheDocument();
      expect(
        within(ui.languageFacetList.get()).getByRole('checkbox', { name: 'java' })
      ).toHaveTextContent('java25short_number_suffix.k');
      expect(
        within(ui.languageFacetList.get()).getByRole('checkbox', { name: 'ts' })
      ).toHaveTextContent('ts3.2short_number_suffix.k');

      await user.click(ui.languageFacet.get());
      expect(ui.languageFacetList.query()).not.toBeInTheDocument();
      await user.click(ui.vulnerabilityIssueTypeFilter.get());
      await user.click(ui.languageFacet.get());
      expect(await ui.languageFacetList.find()).toBeInTheDocument();
      expect(
        within(ui.languageFacetList.get()).getByRole('checkbox', { name: 'java' })
      ).toHaveTextContent('java111');
      expect(
        within(ui.languageFacetList.get()).getByRole('checkbox', { name: 'ts' })
      ).toHaveTextContent('ts674');
    });
  });

  it('should show the new code issues only', async () => {
    const user = userEvent.setup();

    renderProjectIssuesApp('project/issues?id=myproject');

    expect(await ui.issueItems.findAll()).toHaveLength(7);
    await user.click(await ui.inNewCodeFilter.find());
    expect(await ui.issueItems.findAll()).toHaveLength(6);
  });
});

describe('issues item', () => {
  it('should navigate to Why is this an issue tab', async () => {
    renderProjectIssuesApp('project/issues?issues=issue2&open=issue2&id=myproject&why=1');

    expect(
      await screen.findByRole('tab', {
        name: `coding_rules.description_section.title.root_cause`,
      })
    ).toHaveAttribute('aria-current', 'true');
  });

  it('should interact with flows and locations', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp('project/issues?issues=issue11&open=issue11&id=myproject');

    expect(await screen.findByLabelText('list_of_issues')).toBeInTheDocument();

    const dataFlowButton = await screen.findByRole('button', {
      name: 'issue.flow.x_steps.2 Backtracking 1',
    });
    const exectionFlowButton = screen.getByRole('button', {
      name: 'issue.flow.x_steps.3 issue.full_execution_flow',
    });

    let dataLocation1Button = screen.getByRole('link', { name: '1 Data location 1' });
    let dataLocation2Button = screen.getByRole('link', { name: '2 Data location 2' });

    expect(dataFlowButton).toBeInTheDocument();
    expect(dataLocation1Button).toBeInTheDocument();
    expect(dataLocation2Button).toBeInTheDocument();

    await user.click(dataFlowButton);
    // Colapsing flow
    expect(dataLocation1Button).not.toBeInTheDocument();
    expect(dataLocation2Button).not.toBeInTheDocument();

    await user.click(exectionFlowButton);
    expect(screen.getByRole('link', { name: '1 Execution location 1' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '2 Execution location 2' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '3 Execution location 3' })).toBeInTheDocument();

    // Keyboard interaction
    await user.click(dataFlowButton);
    dataLocation1Button = screen.getByRole('link', { name: '1 Data location 1' });
    dataLocation2Button = screen.getByRole('link', { name: '2 Data location 2' });

    // Location navigation
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');

    expect(dataLocation1Button).toHaveAttribute('aria-current', 'true');
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');
    expect(dataLocation1Button).toHaveAttribute('aria-current', 'false');
    expect(dataLocation2Button).toHaveAttribute('aria-current', 'true');
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');
    expect(dataLocation1Button).toHaveAttribute('aria-current', 'false');
    expect(dataLocation2Button).toHaveAttribute('aria-current', 'false');
    await user.keyboard('{Alt>}{ArrowUp}{/Alt}');
    expect(dataLocation1Button).toHaveAttribute('aria-current', 'false');

    expect(dataLocation2Button).toHaveAttribute('aria-current', 'true');

    // Flow navigation
    await user.keyboard('{Alt>}{ArrowRight}{/Alt}');
    expect(screen.getByRole('link', { name: '1 Execution location 1' })).toHaveAttribute(
      'aria-current',
      'true'
    );
    await user.keyboard('{Alt>}{ArrowLeft}{/Alt}');
    expect(screen.getByRole('link', { name: '1 Data location 1' })).toHaveAttribute(
      'aria-current',
      'true'
    );
  });

  it('should show education principles', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp('project/issues?issues=issue2&open=issue2&id=myproject');
    await user.click(
      await screen.findByRole('tab', { name: `coding_rules.description_section.title.more_info` })
    );
    expect(screen.getByRole('heading', { name: 'Defense-In-Depth', level: 3 })).toBeInTheDocument();
  });

  it('should be able to perform action on issues', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    // Get a specific issue list item
    const listItem = within(await screen.findByRole('region', { name: 'Fix that' }));

    // Change issue type
    await act(async () => {
      await user.click(
        listItem.getByLabelText('issue.type.type_x_click_to_change.issue.type.CODE_SMELL')
      );
    });
    expect(listItem.getByText('issue.type.BUG')).toBeInTheDocument();
    expect(listItem.getByText('issue.type.VULNERABILITY')).toBeInTheDocument();

    await act(async () => {
      await user.click(listItem.getByText('issue.type.VULNERABILITY'));
    });
    expect(
      listItem.getByLabelText('issue.type.type_x_click_to_change.issue.type.VULNERABILITY')
    ).toBeInTheDocument();

    // Change issue severity
    expect(listItem.getByText('severity.MAJOR')).toBeInTheDocument();

    await act(async () => {
      await user.click(
        listItem.getByLabelText('issue.severity.severity_x_click_to_change.severity.MAJOR')
      );
    });
    expect(listItem.getByText('severity.MINOR')).toBeInTheDocument();
    expect(listItem.getByText('severity.INFO')).toBeInTheDocument();
    await act(async () => {
      await user.click(listItem.getByText('severity.MINOR'));
    });
    expect(
      listItem.getByLabelText('issue.severity.severity_x_click_to_change.severity.MINOR')
    ).toBeInTheDocument();

    // Change issue status
    expect(listItem.getByText('issue.status.OPEN')).toBeInTheDocument();

    await act(async () => {
      await user.click(listItem.getByText('issue.status.OPEN'));
    });
    expect(listItem.getByText('issue.transition.confirm')).toBeInTheDocument();
    expect(listItem.getByText('issue.transition.resolve')).toBeInTheDocument();

    await act(async () => {
      await user.click(listItem.getByText('issue.transition.confirm'));
    });
    expect(
      listItem.getByLabelText('issue.transition.status_x_click_to_change.issue.status.CONFIRMED')
    ).toBeInTheDocument();

    // As won't fix
    await act(async () => {
      await user.click(listItem.getByText('issue.status.CONFIRMED'));
      await user.click(listItem.getByText('issue.transition.wontfix'));
    });
    // Comment should open and close
    expect(listItem.getByRole('button', { name: 'issue.comment.formlink' })).toBeInTheDocument();
    await act(async () => {
      await user.keyboard('test');
      await user.click(listItem.getByRole('button', { name: 'issue.comment.formlink' }));
    });
    expect(
      listItem.queryByRole('button', { name: 'issue.comment.submit' })
    ).not.toBeInTheDocument();

    // Assign issue to a different user

    await act(async () => {
      await user.click(
        listItem.getByRole('combobox', { name: 'issue.assign.unassigned_click_to_assign' })
      );
      await user.click(screen.getByLabelText('search.search_for_users'));
      await user.keyboard('luke');
    });
    expect(screen.getByText('Skywalker')).toBeInTheDocument();

    await act(async () => {
      await user.click(screen.getByText('Skywalker'));
    });
    await listItem.findByRole('combobox', {
      name: 'issue.assign.assigned_to_x_click_to_change.luke',
    });
    expect(
      listItem.getByRole('combobox', {
        name: 'issue.assign.assigned_to_x_click_to_change.luke',
      })
    ).toBeInTheDocument();

    // Change tags
    expect(listItem.getByText('issue.no_tag')).toBeInTheDocument();
    await act(async () => {
      await user.click(listItem.getByText('issue.no_tag'));
    });
    expect(listItem.getByRole('searchbox', { name: 'search.search_for_tags' })).toBeInTheDocument();
    expect(listItem.getByText('android')).toBeInTheDocument();
    expect(listItem.getByText('accessibility')).toBeInTheDocument();

    await act(async () => {
      await user.click(listItem.getByText('accessibility'));
      await user.click(listItem.getByText('android'));
    });
    expect(listItem.getByTitle('accessibility, android')).toBeInTheDocument();

    // Unselect
    await act(async () => {
      await user.click(screen.getByRole('checkbox', { name: 'accessibility' }));
    });
    expect(listItem.getByTitle('android')).toBeInTheDocument();

    await act(async () => {
      await user.click(screen.getByRole('searchbox', { name: 'search.search_for_tags' }));
      await user.keyboard('addNewTag');
    });
    expect(
      screen.getByRole('checkbox', { name: 'issue.create_tag: addnewtag' })
    ).toBeInTheDocument();
  });

  it('should not allow performing actions when user does not have permission', async () => {
    const user = userEvent.setup();
    renderIssueApp();

    await user.click(await ui.issueItem4.find());

    expect(
      screen.queryByRole('button', {
        name: `issue.assign.unassigned_click_to_assign`,
      })
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', {
        name: `issue.type.type_x_click_to_change.issue.type.CODE_SMELL`,
      })
    ).not.toBeInTheDocument();

    expect(
      screen.queryByRole('button', {
        name: `issue.transition.status_x_click_to_change.issue.status.OPEN`,
      })
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', {
        name: `issue.severity.severity_x_click_to_change.severity.MAJOR`,
      })
    ).not.toBeInTheDocument();
  });

  it('should open the actions popup using keyboard shortcut', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    // Select an issue with an advanced rule
    await act(async () => {
      await user.click(await ui.issueItemAction5.find());

      // open severity popup on key press 'i'

      await user.keyboard('i');
    });
    expect(screen.getByText('severity.MINOR')).toBeInTheDocument();
    expect(screen.getByText('severity.INFO')).toBeInTheDocument();

    // open status popup on key press 'f'
    await act(async () => {
      await user.keyboard('f');
    });
    expect(screen.getByText('issue.transition.confirm')).toBeInTheDocument();
    expect(screen.getByText('issue.transition.resolve')).toBeInTheDocument();

    // open comment popup on key press 'c'
    await act(async () => {
      await user.keyboard('c');
    });
    expect(screen.getByText('issue.comment.formlink')).toBeInTheDocument();
    await act(async () => {
      await user.keyboard('{Escape}');
    });

    // open tags popup on key press 't'

    await act(async () => {
      await user.keyboard('t');
    });
    expect(screen.getByRole('searchbox', { name: 'search.search_for_tags' })).toBeInTheDocument();
    expect(screen.getByText('android')).toBeInTheDocument();
    expect(screen.getByText('accessibility')).toBeInTheDocument();
    // closing tags popup
    await act(async () => {
      await user.click(screen.getByText('issue.no_tag'));

      // open assign popup on key press 'a'
      await user.keyboard('a');
    });
    expect(screen.getByRole('searchbox', { name: 'search.search_for_tags' })).toBeInTheDocument();
  });

  it('should not open the actions popup using keyboard shortcut when keyboard shortcut flag is disabled', async () => {
    localStorage.setItem('sonarqube.preferences.keyboard_shortcuts_enabled', 'false');
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    // Select an issue with an advanced rule
    await user.click(await ui.issueItem5.find());

    // open status popup on key press 'f'
    await user.keyboard('f');
    expect(screen.queryByText('issue.transition.confirm')).not.toBeInTheDocument();
    expect(screen.queryByText('issue.transition.resolve')).not.toBeInTheDocument();

    // open comment popup on key press 'c'
    await user.keyboard('c');
    expect(screen.queryByText('issue.comment.submit')).not.toBeInTheDocument();
    localStorage.setItem('sonarqube.preferences.keyboard_shortcuts_enabled', 'true');
  });

  it('should show code tabs when any secondary location is selected', async () => {
    const user = userEvent.setup();
    renderIssueApp();

    await act(async () => {
      await user.click(await ui.issueItemAction4.find());
    });
    expect(screen.getByRole('link', { name: 'location 1' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'location 2' })).toBeInTheDocument();

    // Select the "why is this an issue" tab
    await act(async () => {
      await user.click(
        screen.getByRole('tab', { name: 'coding_rules.description_section.title.root_cause' })
      );
    });
    expect(
      screen.queryByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
      })
    ).toHaveAttribute('aria-current', 'false');

    await act(async () => {
      await user.click(screen.getByRole('link', { name: 'location 1' }));
    });
    expect(
      screen.queryByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
      })
    ).toHaveAttribute('aria-current', 'true');

    // Select the same selected hotspot location should also navigate back to code page
    await act(async () => {
      await user.click(
        screen.getByRole('tab', { name: 'coding_rules.description_section.title.root_cause' })
      );
    });
    expect(
      screen.queryByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
      })
    ).toHaveAttribute('aria-current', 'false');

    await act(async () => {
      await user.click(screen.getByRole('link', { name: 'location 1' }));
    });
    expect(
      screen.queryByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
      })
    ).toHaveAttribute('aria-current', 'true');
  });

  it('should show issue tags if applicable', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    // Select an issue with an advanced rule
    await act(async () => {
      await user.click(await ui.issueItemAction7.find());
    });

    await expect(
      screen.getByText('issue.quick_fix_available_with_sonarlint_no_link')
    ).toHaveATooltipWithContent('issue.quick_fix_available_with_sonarlint');

    expect(
      screen.getByRole('status', {
        name: 'issue.resolution.badge.DEPRECATED',
      })
    ).toBeInTheDocument();
  });
});

describe('redirects', () => {
  it('should work for hotspots', () => {
    renderProjectIssuesApp(`project/issues?types=${IssueType.SecurityHotspot}`);

    expect(screen.getByText('/security_hotspots?assignedToMe=false')).toBeInTheDocument();
  });

  it('should filter out hotspots', () => {
    renderProjectIssuesApp(
      `project/issues?types=${IssueType.SecurityHotspot},${IssueType.CodeSmell}`
    );

    expect(
      screen.getByRole('checkbox', { name: `issue.type.${IssueType.CodeSmell}` })
    ).toBeInTheDocument();
  });
});

describe('Activity', () => {
  it('should be able to add or update comment', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();
    await act(async () => {
      await user.click(await screen.findByRole('link', { name: 'Fix that' }));
    });

    expect(
      screen.getByRole('tab', { name: `coding_rules.description_section.title.activity` })
    ).toBeInTheDocument();

    await act(async () => {
      await user.click(
        screen.getByRole('tab', { name: `coding_rules.description_section.title.activity` })
      );
    });

    // Add comment to the issue
    await act(async () => {
      await user.click(
        screen.getByRole('button', {
          name: `issue.activity.add_comment`,
        })
      );
      await user.click(screen.getByRole('textbox'));
      await user.keyboard('activity comment');
      await user.click(screen.getByText('hotspots.comment.submit'));
    });
    expect(screen.getByText('activity comment')).toBeInTheDocument();

    // Cancel editing the comment
    await act(async () => {
      await user.click(screen.getByRole('button', { name: 'issue.comment.edit' }));
      await user.click(screen.getByRole('textbox'));
      await user.keyboard(' new');
      await user.click(screen.getByRole('button', { name: 'cancel' }));
    });
    expect(screen.queryByText('activity comment new')).not.toBeInTheDocument();

    // Edit the comment
    await act(async () => {
      await user.click(screen.getByRole('button', { name: 'issue.comment.edit' }));
      await user.click(screen.getByRole('textbox'));
      await user.keyboard(' new');
      await user.click(screen.getByText('hotspots.comment.submit'));
    });
    expect(screen.getByText('activity comment new')).toBeInTheDocument();

    // Delete the comment
    await act(async () => {
      await user.click(screen.getByRole('button', { name: 'issue.comment.delete' }));
      await user.click(screen.getByRole('button', { name: 'delete' })); // Confirm button
    });
    expect(screen.queryByText('activity comment new')).not.toBeInTheDocument();
  });

  it('should be able to show changelog', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    await act(async () => {
      await user.click(await screen.findByRole('link', { name: 'Fix that' }));

      await user.click(
        screen.getByRole('tab', { name: `coding_rules.description_section.title.activity` })
      );
    });

    expect(screen.getByText('issue.activity.review_history.created')).toBeInTheDocument();
    expect(
      screen.getByText(
        'issue.changelog.changed_to.issue.changelog.field.assign.darth.vader (issue.changelog.was.luke.skywalker)'
      )
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'issue.changelog.changed_to.issue.changelog.field.status.REOPENED (issue.changelog.was.CONFIRMED)'
      )
    ).toBeInTheDocument();
  });
});

describe('issues app when reindexing', () => {
  it('should display only some facets while reindexing is in progress', async () => {
    issuesHandler.setIsAdmin(true);
    renderProjectIssuesApp(undefined, { needIssueSync: true });

    // Enabled facets
    expect(ui.inNewCodeFilter.get()).toBeInTheDocument();
    expect(ui.typeFacet.get()).toBeInTheDocument();

    // Disabled facets
    expect(await ui.assigneeFacet.query()).not.toBeInTheDocument();
    expect(await ui.authorFacet.query()).not.toBeInTheDocument();
    expect(await ui.codeVariantsFacet.query()).not.toBeInTheDocument();
    expect(await ui.creationDateFacet.query()).not.toBeInTheDocument();
    expect(await ui.languageFacet.query()).not.toBeInTheDocument();
    expect(await ui.projectFacet.query()).not.toBeInTheDocument();
    expect(await ui.resolutionFacet.query()).not.toBeInTheDocument();
    expect(await ui.ruleFacet.query()).not.toBeInTheDocument();
    expect(await ui.scopeFacet.query()).not.toBeInTheDocument();
    expect(await ui.statusFacet.query()).not.toBeInTheDocument();
    expect(await ui.tagFacet.query()).not.toBeInTheDocument();

    // Indexation message
    expect(screen.getByText(/indexation\.filters_unavailable/)).toBeInTheDocument();
  });
});
