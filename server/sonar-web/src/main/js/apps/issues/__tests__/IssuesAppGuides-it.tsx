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
import { waitForElementToBeRemoved } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { mockCurrentUser } from '../../../helpers/testMocks';
import { IssueTransition } from '../../../types/issues';
import { NoticeType } from '../../../types/users';
import {
  branchHandler,
  componentsHandler,
  issuesHandler,
  renderIssueApp,
  renderProjectIssuesApp,
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

describe('issue guides', () => {
  beforeEach(() => {
    issuesHandler.reset();
    componentsHandler.reset();
    branchHandler.reset();
    usersHandler.reset();
    window.scrollTo = jest.fn();
    window.HTMLElement.prototype.scrollTo = jest.fn();
  });

  describe('Issue Guide', () => {
    it('should display guide', async () => {
      const user = userEvent.setup();
      renderIssueApp(
        mockCurrentUser({
          isLoggedIn: true,
          dismissedNotices: { [NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE]: true },
        }),
      );

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
        mockCurrentUser({
          isLoggedIn: true,
          dismissedNotices: {
            [NoticeType.ISSUE_GUIDE]: true,
            [NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE]: true,
          },
        }),
      );

      expect((await ui.issueItems.findAll()).length).toBeGreaterThan(0);
      expect(ui.guidePopup.query()).not.toBeInTheDocument();
    });

    it('should skip guide', async () => {
      const user = userEvent.setup();
      renderIssueApp(
        mockCurrentUser({
          isLoggedIn: true,
          dismissedNotices: { [NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE]: true },
        }),
      );

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
        mockCurrentUser({
          isLoggedIn: true,
          dismissedNotices: { [NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE]: true },
        }),
      );

      expect((await ui.issueItems.findAll()).length).toBeGreaterThan(0);
      expect(ui.guidePopup.query()).not.toBeInTheDocument();
    });

    it('should not show guide if user is not logged in', async () => {
      renderIssueApp(mockCurrentUser({ isLoggedIn: false }));

      expect((await ui.issueItems.findAll()).length).toBeGreaterThan(0);
      expect(ui.guidePopup.query()).not.toBeInTheDocument();
    });

    it('should not show guide if there are no issues', async () => {
      issuesHandler.setIssueList([]);
      renderIssueApp(mockCurrentUser({ isLoggedIn: true }));

      await waitForElementToBeRemoved(ui.loading.query());
      expect(ui.guidePopup.query()).not.toBeInTheDocument();
    });

    it('should show guide on issue page and save its step on opened issue', async () => {
      const user = userEvent.setup();
      renderProjectIssuesApp('project/issues', undefined, mockCurrentUser({ isLoggedIn: true }));

      expect(await ui.guidePopup.find()).toBeInTheDocument();
      expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.1.5');

      await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

      expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.2.5');

      await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

      expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.3.5');

      await user.click(ui.issueItemAction1.get());

      expect(await ui.guidePopup.find()).toHaveTextContent('guiding.step_x_of_y.3.5');

      await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

      expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.4.5');

      await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());

      expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.5.5');

      expect(ui.guidePopup.byRole('button', { name: 'Next' }).query()).not.toBeInTheDocument();

      await user.click(ui.guidePopup.byRole('button', { name: 'close' }).get());

      expect(ui.guidePopup.query()).not.toBeInTheDocument();
    });
  });

  describe('Issue new status and transition guide', () => {
    it('should save transition guide step', async () => {
      const user = userEvent.setup();
      issuesHandler.list[0].issue.transitions = [
        IssueTransition.Accept,
        IssueTransition.Confirm,
        IssueTransition.Resolve,
        IssueTransition.FalsePositive,
        IssueTransition.WontFix,
      ];
      renderProjectIssuesApp(
        'project/issues',
        undefined,
        mockCurrentUser({ isLoggedIn: true, dismissedNotices: { [NoticeType.ISSUE_GUIDE]: true } }),
      );

      expect(await ui.guidePopup.find()).toBeInTheDocument();
      await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());
      expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.2.3');

      await user.click(ui.issueItemAction1.get());

      expect(await ui.guidePopup.find()).toHaveTextContent('guiding.step_x_of_y.2.3');

      await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());
      await user.click(ui.guidePopup.byRole('button', { name: 'close' }).get());

      expect(ui.guidePopup.query()).not.toBeInTheDocument();
    });
  });
});
