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
import React from 'react';
import { mockRestUser } from '../../../helpers/testMocks';
import {
  branchHandler,
  componentsHandler,
  issuesHandler,
  renderIssueApp,
  ui,
  usersHandler,
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
  usersHandler.reset();
  usersHandler.users = [
    mockRestUser({
      login: 'bob.marley',
      email: 'bob.marley@test.com',
      name: 'Bob Marley',
    }),
  ];
  window.scrollTo = jest.fn();
  window.HTMLElement.prototype.scrollTo = jest.fn();
});

it('should be able to add or update comment', async () => {
  const user = userEvent.setup();
  issuesHandler.setIsAdmin(true);
  renderIssueApp();
  await user.click(await ui.issueItemAction5.find());

  expect(ui.issueActivityTab.get()).toBeInTheDocument();

  await user.click(ui.issueActivityTab.get());

  // Add comment to the issue
  await user.click(ui.issueActivityAddComment.get());
  await user.click(screen.getByRole('textbox'));
  await user.keyboard('activity comment');
  await user.click(screen.getByText('hotspots.comment.submit'));

  expect(screen.getByText('activity comment')).toBeInTheDocument();

  // Cancel editing the comment
  await user.click(ui.issueAcitivityEditComment.get());
  await user.click(screen.getByRole('textbox'));
  await user.keyboard(' new');
  await user.click(screen.getByRole('button', { name: 'cancel' }));

  expect(screen.queryByText('activity comment new')).not.toBeInTheDocument();

  // Edit the comment
  await user.click(ui.issueAcitivityEditComment.get());
  await user.click(screen.getByRole('textbox'));
  await user.keyboard(' new');
  await user.click(screen.getByText('hotspots.comment.submit'));

  expect(screen.getByText('activity comment new')).toBeInTheDocument();

  // Delete the comment
  await user.click(ui.issueActivityDeleteComment.get());
  await user.click(screen.getByRole('button', { name: 'delete' })); // Confirm button

  expect(screen.queryByText('activity comment new')).not.toBeInTheDocument();
});

it('should be able to show changelog', async () => {
  const user = userEvent.setup();
  issuesHandler.setIsAdmin(true);
  renderIssueApp();

  await user.click(await ui.issueItemAction5.find());

  await user.click(ui.issueActivityTab.get());

  expect(screen.getByText(/issue.activity.review_history.created/)).toHaveTextContent('Bob Marley');
  expect(
    screen.getByText(
      'issue.changelog.changed_to.issue.changelog.field.assign.darth.vader (issue.changelog.was.luke.skywalker)',
    ),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'issue.changelog.changed_to.issue.changelog.field.status.REOPENED (issue.changelog.was.CONFIRMED)',
    ),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'issue.changelog.changed_to.issue.changelog.field.issueStatus.ACCEPTED (issue.changelog.was.OPEN)',
    ),
  ).toBeInTheDocument();
  expect(
    screen.queryByText(
      'issue.changelog.changed_to.issue.changelog.field.status.RESOLVED (issue.changelog.was.REOPENED)',
    ),
  ).not.toBeInTheDocument();
});

it('should show author email if there is no user with that email', async () => {
  const user = userEvent.setup();
  issuesHandler.setIsAdmin(true);
  renderIssueApp();

  await user.click(await ui.issueItemAction6.find());

  await user.click(ui.issueActivityTab.get());

  expect(screen.getByText(/issue.activity.review_history.created/)).toHaveTextContent(
    'unknownemail@test.com',
  );
});
