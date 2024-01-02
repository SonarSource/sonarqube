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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { bulkChangeIssues } from '../../../../api/issues';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import { mockIssue, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentPropsType } from '../../../../helpers/testUtils';
import { IssueTransition } from '../../../../types/issues';
import { Issue } from '../../../../types/types';
import { CurrentUser } from '../../../../types/users';
import BulkChangeModal, { MAX_PAGE_SIZE } from '../BulkChangeModal';

jest.mock('../../../../api/issues', () => ({
  bulkChangeIssues: jest.fn().mockResolvedValue({}),
  searchIssueTags: jest.fn().mockResolvedValue(['tag1', 'tag2']),
}));

afterEach(() => {
  jest.clearAllMocks();
});

it('should display error message when no issues available', async () => {
  renderBulkChangeModal([]);

  expect(await screen.findByText('issue_bulk_change.no_match')).toBeInTheDocument();
});

it('should display warning when too many issues are passed', async () => {
  const issues: Issue[] = [];

  for (let i = MAX_PAGE_SIZE + 1; i > 0; i--) {
    issues.push(mockIssue());
  }

  renderBulkChangeModal(issues, { needIssueSync: true });

  expect(
    await screen.findByText(`issue_bulk_change.form.title.${MAX_PAGE_SIZE}`),
  ).toBeInTheDocument();

  expect(await screen.findByText('issue_bulk_change.max_issues_reached')).toBeInTheDocument();
});

it('should render tags correctly', async () => {
  renderBulkChangeModal([mockIssue(false, { actions: ['set_tags'] })]);

  expect(await screen.findByRole('combobox', { name: 'issue.add_tags' })).toBeInTheDocument();
  expect(await screen.findByRole('combobox', { name: 'issue.remove_tags' })).toBeInTheDocument();
});

it('should render transitions correctly', async () => {
  renderBulkChangeModal([
    mockIssue(false, { actions: ['set_transition'], transitions: [IssueTransition.FalsePositive] }),
  ]);

  expect(await screen.findByText('issue.change_status')).toBeInTheDocument();
  expect(await screen.findByText('issue.transition.falsepositive')).toBeInTheDocument();
});

it('should only render the comment field when necessary', async () => {
  const user = userEvent.setup();
  renderBulkChangeModal([
    mockIssue(false, {
      actions: ['set_transition', 'comment'],
      key: 'issue1',
      transitions: [IssueTransition.Reopen],
    }),
    mockIssue(false, {
      actions: ['set_transition', 'comment'],
      key: 'issue2',
      transitions: [IssueTransition.Accept],
    }),
  ]);

  // Open should not trigger comment
  await user.click(await screen.findByText('issue.transition.reopen'));
  expect(
    screen.queryByRole('textbox', { name: /issue_bulk_change.resolution_comment/ }),
  ).not.toBeInTheDocument();

  // Accept should trigger comment
  await user.click(await screen.findByText('issue.transition.accept'));
  expect(
    await screen.findByRole('textbox', { name: /issue_bulk_change.resolution_comment/ }),
  ).toBeInTheDocument();
});

it('should disable the submit button unless some change is configured', async () => {
  const user = userEvent.setup();
  renderBulkChangeModal([mockIssue(false, { actions: ['set_tags', 'comment'] })]);

  // Apply button should be disabled
  expect(await screen.findByRole('button', { name: 'apply' })).toBeDisabled();

  // Add a tag
  await user.click(screen.getByRole('combobox', { name: 'issue.add_tags' }));
  await user.click(screen.getByText('tag1'));
  await user.click(screen.getByText('tag2'));

  // Apply button should be enabled now
  expect(screen.getByRole('button', { name: 'apply' })).toBeEnabled();
});

it('should properly submit', async () => {
  const onDone = jest.fn();
  const user = userEvent.setup();

  renderBulkChangeModal(
    [
      mockIssue(false, {
        actions: ['assign', 'set_transition', 'set_tags', 'set_type', 'set_severity', 'comment'],
        key: 'issue1',
        transitions: [IssueTransition.Accept, IssueTransition.FalsePositive],
      }),
      mockIssue(false, {
        actions: ['assign', 'set_transition', 'set_tags', 'set_type', 'set_severity', 'comment'],
        key: 'issue2',
        transitions: [IssueTransition.Accept, IssueTransition.FalsePositive],
      }),
    ],
    {
      onDone,
    },
    mockLoggedInUser({
      login: 'toto',
      name: 'Toto',
    }),
  );

  expect(bulkChangeIssues).toHaveBeenCalledTimes(0);
  expect(onDone).toHaveBeenCalledTimes(0);

  // Assign
  await user.click(
    await screen.findByRole('combobox', { name: 'issue_bulk_change.assignee.change' }),
  );

  await user.click(await screen.findByText('Toto'));

  // Transition
  await user.click(await screen.findByText('issue.transition.accept'));

  // Add a tag
  await user.click(screen.getByRole('combobox', { name: 'issue.add_tags' }));
  await user.click(screen.getByText('tag1'));
  await user.click(screen.getByText('tag2'));

  // Comment
  await user.type(
    screen.getByRole('textbox', { name: /issue_bulk_change.resolution_comment/ }),
    'some comment',
  );

  // Send notification
  await user.click(screen.getByRole('checkbox', { name: 'issue.send_notifications' }));

  // Submit
  await user.click(screen.getByRole('button', { name: 'apply' }));

  expect(bulkChangeIssues).toHaveBeenCalledTimes(1);
  expect(onDone).toHaveBeenCalledTimes(1);

  expect(bulkChangeIssues).toHaveBeenCalledWith(['issue1', 'issue2'], {
    add_tags: 'tag1,tag2',
    assign: 'toto',
    comment: 'some comment',
    do_transition: 'accept',
    sendNotifications: true,
  });
});

function renderBulkChangeModal(
  issues: Issue[],
  props: Partial<ComponentPropsType<typeof BulkChangeModal>> = {},
  currentUser: CurrentUser = mockLoggedInUser(),
) {
  return renderComponent(
    <CurrentUserContextProvider currentUser={currentUser}>
      <BulkChangeModal
        fetchIssues={() =>
          Promise.resolve({
            issues,
            paging: {
              pageIndex: issues.length,
              pageSize: issues.length,
              total: issues.length,
            },
          })
        }
        onClose={() => {}}
        onDone={() => {}}
        {...props}
      />
    </CurrentUserContextProvider>,
    '',
  );
}
