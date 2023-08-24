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

import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { branchHandler, componentsHandler, issuesHandler, renderIssueApp } from '../test-utils';

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
