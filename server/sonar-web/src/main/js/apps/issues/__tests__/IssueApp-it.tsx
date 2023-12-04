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
import { TabKeys } from '../../../components/rules/RuleTabViewer';
import { byRole } from '../../../helpers/testSelector';
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

describe('issue app', () => {
  it('should always be able to render the open issue', async () => {
    renderProjectIssuesApp('project/issues?issueStatuses=CONFIRMED&open=issue2&id=myproject&why=1');

    expect(await ui.conciseIssueTotal.find()).toHaveTextContent('3');
    expect(ui.conciseIssueItem4.get()).toBeInTheDocument();
    expect(ui.conciseIssueItem2.get()).toBeInTheDocument();
  });

  it('should navigate to Why is this an issue tab', async () => {
    renderProjectIssuesApp('project/issues?issues=issue2&open=issue2&id=myproject&why=1');

    expect(
      await screen.findByRole('tab', {
        name: `coding_rules.description_section.title.root_cause`,
      }),
    ).toHaveAttribute('aria-current', 'true');
  });

  it('should interact with flows and locations', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp('project/issues?id=myproject');

    await user.click(await ui.issueItemAction2.find());

    expect(await screen.findByLabelText('list_of_issues')).toBeInTheDocument();

    const dataFlowButton = await screen.findByRole('button', {
      name: 'issue.flow.x_steps.2 Backtracking 1',
    });
    const exectionFlowButton = screen.getByRole('button', {
      name: 'issue.flow.x_steps.3 issue.full_execution_flow',
    });

    let dataLocation1Button = screen.getByLabelText('Data location 1');
    let dataLocation2Button = screen.getByLabelText('Data location 2');

    expect(dataFlowButton).toBeInTheDocument();
    expect(dataLocation1Button).toBeInTheDocument();
    expect(dataLocation2Button).toBeInTheDocument();

    await user.click(dataFlowButton);
    // Colapsing flow
    expect(dataLocation1Button).not.toBeInTheDocument();
    expect(dataLocation2Button).not.toBeInTheDocument();

    await user.click(exectionFlowButton);
    expect(screen.getByLabelText('Execution location 1')).toBeInTheDocument();
    expect(screen.getByLabelText('Execution location 2')).toBeInTheDocument();
    expect(screen.getByLabelText('Execution location 3')).toBeInTheDocument();

    // Keyboard interaction
    await user.click(dataFlowButton);
    dataLocation1Button = screen.getByLabelText('Data location 1');
    dataLocation2Button = screen.getByLabelText('Data location 2');

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
    expect(screen.getByLabelText('Execution location 1')).toHaveAttribute('aria-current', 'true');
    await user.keyboard('{Alt>}{ArrowLeft}{/Alt}');
    expect(screen.getByLabelText('Data location 1')).toHaveAttribute('aria-current', 'true');
  });

  it('should show education principles', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp('project/issues?issues=issue2&open=issue2&id=myproject');
    await user.click(
      await screen.findByRole('tab', { name: `coding_rules.description_section.title.more_info` }),
    );
    expect(screen.getByRole('heading', { name: 'Defense-In-Depth', level: 3 })).toBeInTheDocument();
  });

  it('should be able to change the issue status', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    // Get a specific issue list item
    const listItem = within(await screen.findByLabelText('Fix that'));

    expect(listItem.getByText('issue.issue_status.OPEN')).toBeInTheDocument();

    await user.click(listItem.getByText('issue.issue_status.OPEN'));

    expect(listItem.getByText('issue.transition.accept')).toBeInTheDocument();
    expect(listItem.getByText('issue.transition.confirm')).toBeInTheDocument();

    await user.click(listItem.getByText('issue.transition.confirm'));

    expect(listItem.getByRole('textbox')).toBeInTheDocument();

    await user.type(listItem.getByRole('textbox'), 'test');
    await user.click(listItem.getByText('resolve'));

    expect(
      listItem.getByLabelText(
        'issue.transition.status_x_click_to_change.issue.issue_status.CONFIRMED',
      ),
    ).toBeInTheDocument();

    // Change status again
    await user.click(listItem.getByText('issue.issue_status.CONFIRMED'));
    await user.click(listItem.getByText('issue.transition.accept'));
    await user.click(listItem.getByText('resolve'));

    expect(
      listItem.getByLabelText(
        'issue.transition.status_x_click_to_change.issue.issue_status.ACCEPTED',
      ),
    ).toBeInTheDocument();

    expect(
      listItem.queryByRole('button', { name: 'issue.comment.submit' }),
    ).not.toBeInTheDocument();
  });

  it('should be able to assign issue to a different user', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    // Get a specific issue list item
    const listItem = within(await screen.findByLabelText('Fix that'));
    // Assign issue to a different user
    await user.click(listItem.getByLabelText('issue.assign.unassigned_click_to_assign'));
    await user.click(screen.getByLabelText('search.search_for_users'));
    await user.keyboard('luke');

    expect(screen.getByText('Skywalker')).toBeInTheDocument();

    await user.click(screen.getByText('Skywalker'));
    await listItem.findByLabelText('issue.assign.assigned_to_x_click_to_change.luke');

    expect(
      listItem.getByLabelText('issue.assign.assigned_to_x_click_to_change.luke'),
    ).toBeInTheDocument();
  });

  it('should be able to change tags on a issue', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    // Get a specific issue list item
    const listItem = within(await screen.findByLabelText('Fix that'));

    // Change tags
    expect(listItem.getByText('issue.no_tag')).toBeInTheDocument();

    await user.click(listItem.getByText('issue.no_tag'));

    expect(listItem.getByLabelText('search.search_for_tags')).toBeInTheDocument();
    expect(listItem.getByText('android')).toBeInTheDocument();
    expect(listItem.getByText('accessibility')).toBeInTheDocument();

    await user.click(listItem.getByText('accessibility'));
    await user.click(listItem.getByText('android'));

    await user.keyboard('{Escape}');
    await expect(
      byRole('button', { name: 'accessibility android +' }).byText('accessibility').get(),
    ).toHaveATooltipWithContent('accessibility, android');

    await user.click(listItem.getByRole('button', { name: 'accessibility android +' }));

    // Unselect
    await user.click(screen.getByLabelText('accessibility'));

    await user.keyboard('{Escape}');
    await expect(
      byRole('button', { name: 'android +' }).byText('android').get(),
    ).toHaveATooltipWithContent('android');

    await user.click(listItem.getByRole('button', { name: 'android +' }));

    await user.click(screen.getByLabelText('search.search_for_tags'));
    await user.keyboard('addNewTag');

    expect(screen.getByLabelText('issue.create_tag: addnewtag')).toBeInTheDocument();
  });

  it('should not allow performing actions when user does not have permission', async () => {
    const user = userEvent.setup();
    renderIssueApp();

    await user.click(await ui.issueItem4.find());

    expect(
      screen.queryByRole('button', {
        name: `issue.assign.unassigned_click_to_assign`,
      }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', {
        name: `issue.type.type_x_click_to_change.issue.type.CODE_SMELL`,
      }),
    ).not.toBeInTheDocument();

    expect(
      screen.queryByRole('button', {
        name: `issue.transition.status_x_click_to_change.issue.status.OPEN`,
      }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', {
        name: `issue.severity.severity_x_click_to_change.severity.MAJOR`,
      }),
    ).not.toBeInTheDocument();
  });

  it('should open the actions popup using keyboard shortcut', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    // Select an issue with an advanced rule
    await user.click(await ui.issueItemAction5.find());

    // Open status popup on key press 'f'
    await user.keyboard('f');

    expect(screen.getByText('issue.transition.confirm')).toBeInTheDocument();
    expect(screen.getByText('issue.transition.resolve')).toBeInTheDocument();

    // Open comment popup on key press 'c'
    await user.keyboard('c');

    expect(screen.getByText('issue.comment.formlink')).toBeInTheDocument();
    await user.keyboard('{Escape}');

    // Open tags popup on key press 't'
    await user.keyboard('t');

    expect(screen.getByRole('searchbox', { name: 'search.search_for_tags' })).toBeInTheDocument();
    expect(screen.getByText('android')).toBeInTheDocument();
    expect(screen.getByText('accessibility')).toBeInTheDocument();

    // Close tags popup
    await user.click(screen.getByText('issue.no_tag'));

    // Open assign popup on key press 'a'
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

    await user.click(await ui.issueItemAction4.find());

    expect(screen.getByRole('link', { name: 'location 1' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'location 2' })).toBeInTheDocument();

    // Select the "why is this an issue" tab
    await user.click(
      screen.getByRole('tab', { name: 'coding_rules.description_section.title.root_cause' }),
    );

    expect(
      screen.queryByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
      }),
    ).toHaveAttribute('aria-current', 'false');

    await user.click(screen.getByRole('link', { name: 'location 1' }));

    expect(
      screen.queryByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
      }),
    ).toHaveAttribute('aria-current', 'true');

    // Select the same selected hotspot location should also navigate back to code page
    await user.click(
      screen.getByRole('tab', { name: 'coding_rules.description_section.title.root_cause' }),
    );

    expect(
      screen.queryByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
      }),
    ).toHaveAttribute('aria-current', 'false');

    await user.click(screen.getByRole('link', { name: 'location 1' }));

    expect(
      screen.queryByRole('tab', {
        name: `issue.tabs.${TabKeys.Code}`,
      }),
    ).toHaveAttribute('aria-current', 'true');
  });

  it('should show sonarlint badge if applicable', async () => {
    const user = userEvent.setup();
    issuesHandler.setIsAdmin(true);
    renderIssueApp();

    // Select an issue with quick fix available
    await user.click(await ui.issueItemAction7.find());

    await expect(screen.getByText('issue.quick_fix')).toHaveATooltipWithContent(
      'issue.quick_fix_available_with_sonarlint',
    );
  });
});
