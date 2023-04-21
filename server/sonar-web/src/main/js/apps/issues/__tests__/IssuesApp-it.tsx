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
import { mockLoggedInUser } from '../../../helpers/testMocks';
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
      await user.click(await screen.findByRole('region', { name: 'Fix that' }));
      expect(screen.getByRole('tab', { name: 'issue.tabs.code' })).toBeInTheDocument();

      // Are rule headers present?
      expect(screen.getByRole('heading', { level: 1, name: 'Fix that' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'advancedRuleId' })).toBeInTheDocument();

      // Select the "why is this an issue" tab and check its content
      await user.click(
        screen.getByRole('tab', { name: `coding_rules.description_section.title.root_cause` })
      );
      expect(screen.getByRole('heading', { name: 'Because' })).toBeInTheDocument();

      // Select the "how to fix it" tab
      await user.click(
        screen.getByRole('tab', { name: `coding_rules.description_section.title.how_to_fix` })
      );

      // Is the context selector present with the expected values and default selection?
      expect(screen.getByRole('button', { name: 'Context 2' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Context 3' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Spring' })).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'coding_rules.description_context.other' })
      ).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Spring' })).toHaveClass('selected');

      // Select context 2 and check tab content
      await user.click(screen.getByRole('button', { name: 'Context 2' }));
      expect(screen.getByText('Context 2 content')).toBeInTheDocument();

      // Select the "other" context and check tab content
      await user.click(
        screen.getByRole('button', { name: 'coding_rules.description_context.other' })
      );
      expect(screen.getByText('coding_rules.context.others.title')).toBeInTheDocument();
      expect(screen.getByText('coding_rules.context.others.description.first')).toBeInTheDocument();
      expect(
        screen.getByText('coding_rules.context.others.description.second')
      ).toBeInTheDocument();

      // Select the main info tab and check its content
      await user.click(
        screen.getByRole('tab', { name: `coding_rules.description_section.title.more_info` })
      );
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
      await user.click(
        screen.getByRole('tab', { name: `coding_rules.description_section.title.root_cause` })
      );
      expect(screen.getByRole('heading', { name: 'Default' })).toBeInTheDocument();

      // Select the previous issue (with a simple rule) through keyboard shortcut
      await act(async () => {
        await user.keyboard('{ArrowUp}');
      });

      // Are rule headers present?
      expect(screen.getByRole('heading', { level: 1, name: 'Issue on file' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'simpleRuleId' })).toBeInTheDocument();

      // The "Where is the issue" tab should be selected by default. Check its content
      expect(screen.getByRole('region', { name: 'Issue on file' })).toBeInTheDocument();
      expect(
        screen.getByRole('row', {
          name: '2 * SonarQube',
        })
      ).toBeInTheDocument();
    });

    it('should be able to navigate to other issue located in the same file', async () => {
      const user = userEvent.setup();
      renderIssueApp();

      await user.click(await ui.issueItem5.find());
      expect(ui.issueItem6.get()).toBeInTheDocument();

      await user.click(ui.issueItem6.get());
      expect(screen.getByRole('heading', { level: 1, name: 'Second issue' })).toBeInTheDocument();
    });

    it('should be able to show more issues', async () => {
      const user = userEvent.setup();
      renderIssueApp();

      expect(await ui.issueItems.findAll()).toHaveLength(7);
      expect(ui.issueItem8.query()).not.toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: 'show_more' }));
      expect(ui.issueItems.getAll()).toHaveLength(8);
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
        screen.getByRole('button', { name: 'issues.bulk_change_X_issues.8' })
      ).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: 'issues.bulk_change_X_issues.8' }));
      await user.click(screen.getByRole('button', { name: 'cancel' }));
      expect(screen.getByRole('button', { name: 'issues.bulk_change_X_issues.8' })).toHaveFocus();
      await user.click(screen.getByRole('checkbox', { name: 'issues.select_all_issues' }));

      // Check that we bulk change the selected issue
      const issueBoxFixThat = within(screen.getByRole('region', { name: 'Fix that' }));

      expect(
        issueBoxFixThat.getByRole('button', {
          name: 'issue.type.type_x_click_to_change.issue.type.CODE_SMELL',
        })
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
        issueBoxFixThat.getByRole('button', {
          name: 'issue.type.type_x_click_to_change.issue.type.BUG',
        })
      ).toBeInTheDocument();
    });
  });
  describe('filtering', () => {
    it('should handle filtering from a specific issue properly', async () => {
      const user = userEvent.setup();
      renderIssueApp();
      await waitOnDataLoaded();

      // Ensure issue type filter is unchecked
      expect(ui.codeSmellIssueTypeFilter.get()).not.toBeChecked();
      expect(ui.vulnerabilityIssueTypeFilter.get()).not.toBeChecked();
      expect(ui.issueItem1.get()).toBeInTheDocument();
      expect(ui.issueItem2.get()).toBeInTheDocument();

      // Open filter similar issue dropdown for issue 2 (Code smell)
      await user.click(
        await within(ui.issueItem2.get()).findByRole('button', {
          name: 'issue.filter_similar_issues',
        })
      );
      await user.click(
        await within(ui.issueItem2.get()).findByRole('button', {
          name: 'issue.type.CODE_SMELL',
        })
      );

      expect(ui.codeSmellIssueTypeFilter.get()).toBeChecked();
      expect(ui.vulnerabilityIssueTypeFilter.get()).not.toBeChecked();
      expect(ui.issueItem1.query()).not.toBeInTheDocument();
      expect(ui.issueItem2.get()).toBeInTheDocument();
      expect(
        screen.queryByRole('button', { name: 'issues.facet.owaspTop10_2021' })
      ).not.toBeInTheDocument();

      // Clear filters
      await user.click(ui.clearAllFilters.get());

      // Open filter similar issue dropdown for issue 3 (Vulnerability)
      await user.click(
        await within(await ui.issueItem1.find()).findByRole('button', {
          name: 'issue.filter_similar_issues',
        })
      );
      await user.click(
        await within(await ui.issueItem1.find()).findByRole('button', {
          name: 'issue.type.VULNERABILITY',
        })
      );

      expect(ui.codeSmellIssueTypeFilter.get()).not.toBeChecked();
      expect(ui.vulnerabilityIssueTypeFilter.get()).toBeChecked();
      expect(ui.issueItem1.get()).toBeInTheDocument();
      expect(ui.issueItem2.query()).not.toBeInTheDocument();
      // Standards should now be expanded and Owasp should be visible
      expect(screen.getByRole('button', { name: 'issues.facet.owaspTop10_2021' })).toBeVisible();
    });

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
      expect(screen.getByRole('checkbox', { name: '(HTML) Advanced rule' })).toBeInTheDocument(); // Name should apply to the rule

      // Tag
      await user.click(ui.tagFacet.get());
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

    it('should allow to set creation date', async () => {
      const user = userEvent.setup();
      const currentUser = mockLoggedInUser();
      issuesHandler.setCurrentUser(currentUser);
      renderIssueApp(currentUser);
      await waitOnDataLoaded();

      // Select a specific date range such that only one issue matches
      await user.click(ui.creationDateFacet.get());
      await user.click(screen.getByPlaceholderText('start_date'));
      await user.selectOptions(ui.dateInputMonthSelect.get(), 'January');
      await user.selectOptions(ui.dateInputYearSelect.get(), '2023');
      await user.click(screen.getByText('1'));
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
      await user.click(screen.getByRole('button', { name: 'issues.my_issues' }));
      expect(ui.issueItem2.query()).not.toBeInTheDocument();
      expect(ui.issueItem3.get()).toBeInTheDocument();

      // Show all issues again
      await user.click(screen.getByRole('button', { name: 'all' }));
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
});

describe('issues item', () => {
  it('should navigate to Why is this an issue tab', async () => {
    renderProjectIssuesApp('project/issues?issues=issue2&open=issue2&id=myproject&why=1');

    expect(
      await screen.findByRole('tab', {
        name: `coding_rules.description_section.title.root_cause`,
        selected: true,
      })
    ).toBeInTheDocument();
  });

  it('should show secondary location even when no message is present', async () => {
    renderProjectIssuesApp('project/issues?issues=issue101&open=issue101&id=myproject');

    expect(await screen.findByRole('button', { name: '1 issue.location_x.1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '2 issue.location_x.2' })).toBeInTheDocument();
  });

  it('should interact with flows and locations', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp('project/issues?issues=issue11&open=issue11&id=myproject');
    const dataFlowButton = await screen.findByRole('button', { name: 'Backtracking 1' });
    const exectionFlowButton = screen.getByRole('button', { name: 'issue.execution_flow' });

    let dataLocation1Button = screen.getByRole('button', { name: '1 Data location 1' });
    let dataLocation2Button = screen.getByRole('button', { name: '2 Data location 2' });

    expect(dataFlowButton).toBeInTheDocument();
    expect(dataLocation1Button).toBeInTheDocument();
    expect(dataLocation2Button).toBeInTheDocument();

    await user.click(dataFlowButton);
    // Colapsing flow
    expect(dataLocation1Button).not.toBeInTheDocument();
    expect(dataLocation2Button).not.toBeInTheDocument();

    await user.click(exectionFlowButton);
    expect(screen.getByRole('button', { name: '1 Execution location 1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '2 Execution location 2' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '3 Execution location 3' })).toBeInTheDocument();

    // Keyboard interaction
    await user.click(dataFlowButton);
    dataLocation1Button = screen.getByRole('button', { name: '1 Data location 1' });
    dataLocation2Button = screen.getByRole('button', { name: '2 Data location 2' });

    // Location navigation
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');
    expect(dataLocation1Button).toHaveClass('selected');
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');
    expect(dataLocation1Button).not.toHaveClass('selected');
    expect(dataLocation2Button).toHaveClass('selected');
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');
    expect(dataLocation1Button).not.toHaveClass('selected');
    expect(dataLocation2Button).not.toHaveClass('selected');
    await user.keyboard('{Alt>}{ArrowUp}{/Alt}');
    expect(dataLocation1Button).not.toHaveClass('selected');
    expect(dataLocation2Button).toHaveClass('selected');

    // Flow navigation
    await user.keyboard('{Alt>}{ArrowRight}{/Alt}');
    expect(screen.getByRole('button', { name: '1 Execution location 1' })).toHaveClass('selected');
    await user.keyboard('{Alt>}{ArrowLeft}{/Alt}');
    expect(screen.getByRole('button', { name: '1 Data location 1' })).toHaveClass('selected');
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
    await user.click(
      listItem.getByRole('button', {
        name: `issue.type.type_x_click_to_change.issue.type.CODE_SMELL`,
      })
    );
    expect(listItem.getByText('issue.type.BUG')).toBeInTheDocument();
    expect(listItem.getByText('issue.type.VULNERABILITY')).toBeInTheDocument();

    await user.click(listItem.getByText('issue.type.VULNERABILITY'));
    expect(
      listItem.getByRole('button', {
        name: `issue.type.type_x_click_to_change.issue.type.VULNERABILITY`,
      })
    ).toBeInTheDocument();

    // Change issue severity
    expect(listItem.getByText('severity.MAJOR')).toBeInTheDocument();

    await user.click(
      listItem.getByRole('button', {
        name: `issue.severity.severity_x_click_to_change.severity.MAJOR`,
      })
    );
    expect(listItem.getByText('severity.MINOR')).toBeInTheDocument();
    expect(listItem.getByText('severity.INFO')).toBeInTheDocument();
    await user.click(listItem.getByText('severity.MINOR'));
    expect(
      listItem.getByRole('button', {
        name: `issue.severity.severity_x_click_to_change.severity.MINOR`,
      })
    ).toBeInTheDocument();

    // Change issue status
    expect(listItem.getByText('issue.status.OPEN')).toBeInTheDocument();

    await user.click(listItem.getByText('issue.status.OPEN'));
    expect(listItem.getByText('issue.transition.confirm')).toBeInTheDocument();
    expect(listItem.getByText('issue.transition.resolve')).toBeInTheDocument();

    await user.click(listItem.getByText('issue.transition.confirm'));
    expect(
      listItem.getByRole('button', {
        name: `issue.transition.status_x_click_to_change.issue.status.CONFIRMED`,
      })
    ).toBeInTheDocument();

    // As won't fix
    await user.click(listItem.getByText('issue.status.CONFIRMED'));
    await user.click(listItem.getByText('issue.transition.wontfix'));
    // Comment should open and close
    expect(listItem.getByRole('button', { name: 'issue.comment.formlink' })).toBeInTheDocument();
    await user.keyboard('test');
    await user.click(listItem.getByRole('button', { name: 'issue.comment.formlink' }));
    expect(
      listItem.queryByRole('button', { name: 'issue.comment.submit' })
    ).not.toBeInTheDocument();

    // Assign issue to a different user
    await user.click(
      listItem.getByRole('button', {
        name: `issue.assign.unassigned_click_to_assign`,
      })
    );
    await user.click(listItem.getByRole('searchbox', { name: 'search.search_for_users' }));
    await user.keyboard('luke');
    expect(listItem.getByText('Skywalker')).toBeInTheDocument();
    await user.keyboard('{ArrowUp}{enter}');
    expect(
      listItem.getByRole('button', {
        name: 'issue.assign.assigned_to_x_click_to_change.luke',
      })
    ).toBeInTheDocument();

    // Add comment to the issue
    await user.click(
      listItem.getByRole('button', {
        name: `issue.comment.add_comment`,
      })
    );
    await user.keyboard('comment');
    await user.click(listItem.getByRole('button', { name: 'issue.comment.formlink' }));
    expect(listItem.getByText('comment')).toBeInTheDocument();

    // Cancel editing the comment
    await user.click(listItem.getByRole('button', { name: 'issue.comment.edit' }));
    await user.keyboard('New ');
    await user.click(listItem.getByRole('button', { name: 'issue.comment.edit.cancel' }));
    expect(listItem.queryByText('New comment')).not.toBeInTheDocument();

    // Edit the comment
    await user.click(listItem.getByRole('button', { name: 'issue.comment.edit' }));
    await user.keyboard('New ');
    await user.click(listItem.getByText('save'));
    expect(listItem.getByText('New comment')).toBeInTheDocument();

    // Delete the comment
    await user.click(listItem.getByRole('button', { name: 'issue.comment.delete' }));
    await user.click(listItem.getByRole('button', { name: 'delete' })); // Confirm button
    expect(listItem.queryByText('New comment')).not.toBeInTheDocument();

    // Add comment using keyboard
    await user.click(
      listItem.getByRole('button', {
        name: `issue.comment.add_comment`,
      })
    );
    await user.keyboard('comment');
    await user.keyboard('{Control>}{enter}{/Control}');
    expect(listItem.getByText('comment')).toBeInTheDocument();

    // Edit the comment using keyboard
    await user.click(listItem.getByRole('button', { name: 'issue.comment.edit' }));
    await user.keyboard('New ');
    await user.keyboard('{Control>}{enter}{/Control}');
    expect(listItem.getByText('New comment')).toBeInTheDocument();
    await user.keyboard('{Escape}');

    // Change tags
    expect(listItem.getByText('issue.no_tag')).toBeInTheDocument();
    await user.click(listItem.getByText('issue.no_tag'));
    expect(listItem.getByRole('searchbox', { name: 'search.search_for_tags' })).toBeInTheDocument();
    expect(listItem.getByText('android')).toBeInTheDocument();
    expect(listItem.getByText('accessibility')).toBeInTheDocument();

    await user.click(listItem.getByText('accessibility'));
    await user.click(listItem.getByText('android'));
    expect(listItem.getByTitle('accessibility, android')).toBeInTheDocument();

    // Unselect
    await user.click(screen.getByText('accessibility'));
    expect(screen.getByTitle('android')).toBeInTheDocument();

    await user.click(screen.getByRole('searchbox', { name: 'search.search_for_tags' }));
    await user.keyboard('addNewTag');
    expect(
      screen.getByRole('checkbox', { name: 'create_new_element: addnewtag' })
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

    await user.click(
      screen.getByRole('button', {
        name: `issue.comment.add_comment`,
      })
    );
    expect(screen.queryByRole('button', { name: 'issue.comment.submit' })).not.toBeInTheDocument();
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
    await user.click(await ui.issueItem5.find());

    // open severity popup on key press 'i'
    await user.keyboard('i');
    expect(screen.getByRole('button', { name: 'severity.MINOR' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'severity.INFO' })).toBeInTheDocument();

    // open status popup on key press 'f'
    await user.keyboard('f');
    expect(screen.getByText('issue.transition.confirm')).toBeInTheDocument();
    expect(screen.getByText('issue.transition.resolve')).toBeInTheDocument();

    // open comment popup on key press 'c'
    await user.keyboard('c');
    expect(screen.getByText('issue.comment.formlink')).toBeInTheDocument();
    await user.keyboard('{Escape}');

    // open tags popup on key press 't'
    await user.keyboard('t');
    expect(screen.getByRole('searchbox', { name: 'search.search_for_tags' })).toBeInTheDocument();
    expect(screen.getByText('android')).toBeInTheDocument();
    expect(screen.getByText('accessibility')).toBeInTheDocument();
    // closing tags popup
    await user.click(screen.getByText('issue.no_tag'));

    // open assign popup on key press 'a'
    await user.keyboard('a');
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

    await user.click(await ui.issueItem4.find());
    expect(screen.getByRole('button', { name: '1 location 1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '2 location 2' })).toBeInTheDocument();

    // Select the "why is this an issue" tab
    await user.click(
      screen.getByRole('tab', { name: 'coding_rules.description_section.title.root_cause' })
    );
    expect(
      screen.queryByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
        selected: true,
      })
    ).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '1 location 1' }));
    expect(
      screen.getByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
        selected: true,
      })
    ).toBeInTheDocument();

    // Select the same selected hotspot location should also navigate back to code page
    await user.click(
      screen.getByRole('tab', { name: 'coding_rules.description_section.title.root_cause' })
    );
    expect(
      screen.queryByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
        selected: true,
      })
    ).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '1 location 1' }));
    expect(
      screen.getByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
        selected: true,
      })
    ).toBeInTheDocument();
  });

  it('should show issue tags if applicable', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    // Select an issue with an advanced rule
    await user.click(await ui.issueItem7.find());

    expect(
      screen.getByRole('heading', {
        name: 'Issue with tags issue.quick_fix_available_with_sonarlint_no_link issue.resolution.badge.DEPRECATED',
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
