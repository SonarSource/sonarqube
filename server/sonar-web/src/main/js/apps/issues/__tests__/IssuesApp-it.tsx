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
import React from 'react';
import { TabKeys } from '../../../components/rules/RuleTabViewer';
import { mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
import { byRole } from '../../../helpers/testSelector';
import { ComponentQualifier } from '../../../types/component';
import { IssueType } from '../../../types/issues';
import { NoticeType } from '../../../types/users';
import {
  branchHandler,
  componentsHandler,
  issuesHandler,
  renderIssueApp,
  renderProjectIssuesApp,
  ui,
} from '../test-utils';

jest.mock('../sidebar/Sidebar', () => {
  const fakeSidebar = () => {
    return <div data-guiding-id="issue-5" />;
  };
  return {
    __esModule: true,
    default: fakeSidebar,
    Sidebar: fakeSidebar,
  };
});

jest.mock('../../../components/common/ScreenPositionHelper', () => ({
  __esModule: true,
  default: class ScreenPositionHelper extends React.Component<{
    children: (args: { top: number }) => React.ReactNode;
  }> {
    render() {
      // eslint-disable-next-line testing-library/no-node-access
      return this.props.children({ top: 10 });
    }
  },
}));

beforeEach(() => {
  issuesHandler.reset();
  componentsHandler.reset();
  branchHandler.reset();
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
  });

  describe('navigation', () => {
    it('should handle keyboard navigation in list and open / close issues', async () => {
      const user = userEvent.setup();
      renderIssueApp();

      // Navigate to 2nd issue
      await act(async () => {
        await user.keyboard('{ArrowDown}');
      });

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

      await act(async () => {
        await user.click(screen.getByRole('button', { name: 'show_more' }));
      });

      expect(ui.issueItems.getAll()).toHaveLength(10);
      expect(ui.issueItem8.get()).toBeInTheDocument();
    });

    // Improve this to include all the bulk change fonctionality
    it('should be able to bulk change', async () => {
      const user = userEvent.setup();
      const currentUser = mockLoggedInUser({
        dismissedNotices: { [NoticeType.ISSUE_GUIDE]: true },
      });
      issuesHandler.setIsAdmin(true);
      issuesHandler.setCurrentUser(currentUser);
      renderIssueApp(currentUser);

      // Check that the bulk button has correct behavior
      expect(screen.getByRole('button', { name: 'bulk_change' })).toBeDisabled();

      await act(async () => {
        await user.click(screen.getByRole('checkbox', { name: 'issues.select_all_issues' }));
      });

      expect(
        screen.getByRole('button', { name: 'issues.bulk_change_X_issues.10' })
      ).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: 'issues.bulk_change_X_issues.10' }));
      await user.click(screen.getByRole('button', { name: 'cancel' }));
      expect(screen.getByRole('button', { name: 'issues.bulk_change_X_issues.10' })).toHaveFocus();
      await user.click(screen.getByRole('checkbox', { name: 'issues.select_all_issues' }));

      // Check that we bulk change the selected issue
      const issueBoxFixThat = within(screen.getByRole('region', { name: 'Fix that' }));

      await user.click(
        screen.getByRole('checkbox', { name: 'issues.action_select.label.Fix that' })
      );
      await user.click(screen.getByRole('button', { name: 'issues.bulk_change_X_issues.1' }));

      await user.click(screen.getByRole('textbox', { name: /issue.comment.formlink/ }));
      await user.keyboard('New Comment');
      expect(screen.getByRole('button', { name: 'apply' })).toBeDisabled();

      await user.click(screen.getByRole('radio', { name: 'issue.transition.falsepositive' }));
      await user.click(screen.getByRole('button', { name: 'apply' }));

      expect(
        issueBoxFixThat.queryByLabelText(
          'issue.transition.status_x_click_to_change.issue.status.falsepositive'
        )
      ).not.toBeInTheDocument();
    });
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

    await user.keyboard('{Escape}');
    await expect(
      byRole('button', { name: 'accessibility android +' }).byText('accessibility').get()
    ).toHaveATooltipWithContent('accessibility, android');

    await act(async () => {
      await user.click(listItem.getByRole('button', { name: 'accessibility android +' }));
    });

    // Unselect
    await act(async () => {
      await user.click(screen.getByRole('checkbox', { name: 'accessibility' }));
    });

    await user.keyboard('{Escape}');
    await expect(
      byRole('button', { name: 'android +' }).byText('android').get()
    ).toHaveATooltipWithContent('android');

    await act(async () => {
      await user.click(listItem.getByRole('button', { name: 'android +' }));
    });

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

    await act(async () => {
      await user.click(await ui.issueItem4.find());
    });

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

      // Open status popup on key press 'f'
      await user.keyboard('f');
    });
    expect(screen.getByText('issue.transition.confirm')).toBeInTheDocument();
    expect(screen.getByText('issue.transition.resolve')).toBeInTheDocument();

    // Open comment popup on key press 'c'
    await act(async () => {
      await user.keyboard('c');
    });
    expect(screen.getByText('issue.comment.formlink')).toBeInTheDocument();
    await act(async () => {
      await user.keyboard('{Escape}');
    });

    // Open tags popup on key press 't'
    await act(async () => {
      await user.keyboard('t');
    });
    expect(screen.getByRole('searchbox', { name: 'search.search_for_tags' })).toBeInTheDocument();
    expect(screen.getByText('android')).toBeInTheDocument();
    expect(screen.getByText('accessibility')).toBeInTheDocument();

    // Close tags popup
    await act(async () => {
      await user.click(screen.getByText('issue.no_tag'));

      // Open assign popup on key press 'a'
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
    await act(async () => {
      await user.click(await ui.issueItem5.find());
    });

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
  });
});

describe('redirects', () => {
  it('should work for hotspots', () => {
    renderProjectIssuesApp(`project/issues?types=${IssueType.SecurityHotspot}`);

    expect(screen.getByText('/security_hotspots?assignedToMe=false')).toBeInTheDocument();
  });
});

describe('activity', () => {
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

describe('guide', () => {
  it('should display guide', async () => {
    const user = userEvent.setup();
    renderIssueApp(mockCurrentUser({ isLoggedIn: true }));

    expect(await ui.guidePopup.find()).toBeInTheDocument();

    expect(await ui.guidePopup.find()).toBeInTheDocument();
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.1.title');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.1.content');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.1.5');

    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.2.title');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.2.content');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.2.5');

    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.3.title');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.3.content');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.3.5');

    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.4.title');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.4.content');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.4.5');

    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.5.title');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.5.content');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.5.5');

    expect(ui.guidePopup.byRole('button', { name: 'Next' }).query()).not.toBeInTheDocument();

    await user.click(ui.guidePopup.byRole('button', { name: 'close' }).get());

    expect(ui.guidePopup.query()).not.toBeInTheDocument();
  });

  it('should not show guide for those who dismissed it', async () => {
    renderIssueApp(
      mockCurrentUser({ isLoggedIn: true, dismissedNotices: { [NoticeType.ISSUE_GUIDE]: true } })
    );

    expect((await ui.issueItems.findAll()).length).toBeGreaterThan(0);
    expect(ui.guidePopup.query()).not.toBeInTheDocument();
  });

  it('should skip guide', async () => {
    const user = userEvent.setup();
    renderIssueApp(mockCurrentUser({ isLoggedIn: true }));

    expect(await ui.guidePopup.find()).toBeInTheDocument();
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_list.1.title');
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.1.5');

    await user.click(ui.guidePopup.byRole('button', { name: 'skip' }).get());

    expect(ui.guidePopup.query()).not.toBeInTheDocument();
  });

  it('should not show guide if issues need sync', async () => {
    renderProjectIssuesApp(
      undefined,
      { needIssueSync: true },
      mockCurrentUser({ isLoggedIn: true })
    );

    expect((await ui.issueItems.findAll()).length).toBeGreaterThan(0);
    expect(ui.guidePopup.query()).not.toBeInTheDocument();
  });

  it('should not show guide if user is not logged in', async () => {
    renderIssueApp(mockCurrentUser({ isLoggedIn: false }));

    expect((await ui.issueItems.findAll()).length).toBeGreaterThan(0);
    expect(ui.guidePopup.query()).not.toBeInTheDocument();
  });

  it('should not show guide if there are no issues', () => {
    issuesHandler.setIssueList([]);
    renderIssueApp(mockCurrentUser({ isLoggedIn: true }));

    expect(ui.loading.query()).not.toBeInTheDocument();
    expect(ui.guidePopup.query()).not.toBeInTheDocument();
  });

  it('should show guide on issue page', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp(
      'project/issues?issues=issue11&open=issue11&id=myproject',
      undefined,
      mockCurrentUser({ isLoggedIn: true })
    );

    expect(await ui.guidePopup.find()).toBeInTheDocument();
    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.1.5');

    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.2.5');

    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.3.5');

    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.4.5');

    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

    expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.5.5');

    expect(ui.guidePopup.byRole('button', { name: 'Next' }).query()).not.toBeInTheDocument();

    await user.click(ui.guidePopup.byRole('button', { name: 'close' }).get());

    expect(ui.guidePopup.query()).not.toBeInTheDocument();
  });
});
