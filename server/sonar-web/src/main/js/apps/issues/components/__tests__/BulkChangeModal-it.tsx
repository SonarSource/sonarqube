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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { bulkChangeIssues } from '../../../../api/issues';
import { SEVERITIES } from '../../../../helpers/constants';
import { mockIssue, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { IssueType } from '../../../../types/issues';
import { Issue } from '../../../../types/types';
import BulkChangeModal, { MAX_PAGE_SIZE } from '../BulkChangeModal';

jest.mock('../../../../api/issues', () => ({
  searchIssueTags: jest.fn().mockResolvedValue(['tag1', 'tag2']),
  bulkChangeIssues: jest.fn().mockResolvedValue({}),
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
  renderBulkChangeModal(issues);

  expect(
    await screen.findByText(`issue_bulk_change.form.title.${MAX_PAGE_SIZE}`)
  ).toBeInTheDocument();
  expect(await screen.findByText('issue_bulk_change.max_issues_reached')).toBeInTheDocument();
});

it.each([
  ['type', 'set_type'],
  ['severity', 'set_severity'],
])('should render select for %s', async (_field, action) => {
  renderBulkChangeModal([mockIssue(false, { actions: [action] })]);

  expect(await screen.findByText('issue.' + action)).toBeInTheDocument();
});

it('should render tags correctly', async () => {
  renderBulkChangeModal([mockIssue(false, { actions: ['set_tags'] })]);

  expect(await screen.findByRole('combobox', { name: 'issue.add_tags' })).toBeInTheDocument();
  expect(await screen.findByRole('combobox', { name: 'issue.remove_tags' })).toBeInTheDocument();
});

it('should render transitions correctly', async () => {
  renderBulkChangeModal([
    mockIssue(false, { actions: ['set_transition'], transitions: ['Transition1'] }),
  ]);

  expect(await screen.findByText('issue.transition')).toBeInTheDocument();
  expect(await screen.findByText('issue.transition.Transition1')).toBeInTheDocument();
});

it('should disable the submit button unless some change is configured', async () => {
  const user = userEvent.setup();
  renderBulkChangeModal([mockIssue(false, { actions: ['set_severity', 'comment'] })]);

  // Apply button should be disabled
  expect(await screen.findByRole('button', { name: 'apply' })).toBeDisabled();

  // Adding a comment should not enable the submit button
  await user.type(screen.getByRole('textbox', { name: /issue.comment.formlink/ }), 'some comment');
  expect(screen.getByRole('button', { name: 'apply' })).toBeDisabled();

  // Select a severity
  await selectEvent.select(screen.getByRole('combobox', { name: 'issue.set_severity' }), [
    `severity.${SEVERITIES[0]}`,
  ]);

  // Apply button should be enabled now
  expect(screen.getByRole('button', { name: 'apply' })).toBeEnabled();
});

it('should properly submit', async () => {
  const onDone = jest.fn();
  const user = userEvent.setup();
  renderBulkChangeModal(
    [
      mockIssue(false, {
        key: 'issue1',
        actions: ['assign', 'set_transition', 'set_tags', 'set_type', 'set_severity', 'comment'],
        transitions: ['Transition1', 'Transition2'],
      }),
      mockIssue(false, {
        key: 'issue2',
        actions: ['assign', 'set_transition', 'set_tags', 'set_type', 'set_severity', 'comment'],
        transitions: ['Transition1', 'Transition2'],
      }),
    ],
    {
      onDone,
      currentUser: mockLoggedInUser({
        login: 'toto',
        name: 'Toto',
      }),
    }
  );

  expect(bulkChangeIssues).toHaveBeenCalledTimes(0);
  expect(onDone).toHaveBeenCalledTimes(0);

  // Assign
  await user.click(await screen.findByRole('combobox', { name: 'issue.assign.formlink' }));
  await user.click(await screen.findByText('Toto'));

  // Transition
  await user.click(await screen.findByText('issue.transition.Transition2'));

  // Add a tag
  await selectEvent.select(screen.getByRole('combobox', { name: 'issue.add_tags' }), [
    'tag1',
    'tag2',
  ]);

  // Select a type
  await selectEvent.select(screen.getByRole('combobox', { name: 'issue.set_type' }), [
    `issue.type.CODE_SMELL`,
  ]);

  // Select a severity
  await selectEvent.select(screen.getByRole('combobox', { name: 'issue.set_severity' }), [
    `severity.${SEVERITIES[0]}`,
  ]);

  // Severity
  await selectEvent.select(screen.getByRole('combobox', { name: 'issue.set_severity' }), [
    `severity.${SEVERITIES[0]}`,
  ]);

  // Comment
  await user.type(screen.getByRole('textbox', { name: /issue.comment.formlink/ }), 'some comment');

  // Send notification
  await user.click(screen.getByRole('checkbox', { name: 'issue.send_notifications' }));

  // Submit
  await user.click(screen.getByRole('button', { name: 'apply' }));

  expect(bulkChangeIssues).toHaveBeenCalledTimes(1);
  expect(onDone).toHaveBeenCalledTimes(1);
  expect(bulkChangeIssues).toHaveBeenCalledWith(['issue1', 'issue2'], {
    assign: 'toto',
    comment: 'some comment',
    set_severity: 'BLOCKER',
    add_tags: 'tag1,tag2',
    do_transition: 'Transition2',
    set_type: IssueType.CodeSmell,
    sendNotifications: true,
  });
});

function renderBulkChangeModal(issues: Issue[], props: Partial<BulkChangeModal['props']> = {}) {
  return renderComponent(
    <BulkChangeModal
      component={undefined}
      currentUser={{ isLoggedIn: true, dismissedNotices: {} }}
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
  );
}
