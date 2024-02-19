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
import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import IssuesServiceMock from '../../../../api/mocks/IssuesServiceMock';
import UsersServiceMock from '../../../../api/mocks/UsersServiceMock';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import IssueTransitionComponent from '../../../../components/issue/components/IssueTransition';
import { mockCurrentUser, mockIssue } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { IssueTransition } from '../../../../types/issues';
import { Issue } from '../../../../types/types';
import { NoticeType } from '../../../../types/users';
import { ui } from '../../test-utils';
import IssueNewStatusAndTransitionGuide from '../IssueNewStatusAndTransitionGuide';

const usersHandler = new UsersServiceMock();
const issuesHandler = new IssuesServiceMock(usersHandler);

beforeEach(() => {
  usersHandler.reset();
  issuesHandler.reset();
});

it('should display status guide', async () => {
  const user = userEvent.setup();
  renderIssueNewStatusGuide();

  expect(await ui.guidePopup.find()).toBeInTheDocument();
  expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_accept.1.title');

  await act(async () => {
    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());
  });

  expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_accept.2.title');

  await act(async () => {
    await user.click(ui.guidePopup.byRole('button', { name: 'go_back' }).get());
  });
  expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_accept.1.title');

  await act(async () => {
    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());
  });
  await act(async () => {
    await user.click(ui.guidePopup.byRole('button', { name: 'next' }).get());
  });
  expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_accept.3.title');
  expect(ui.guidePopup.byRole('button', { name: 'Next' }).query()).not.toBeInTheDocument();

  await act(async () => {
    await user.click(ui.guidePopup.byRole('button', { name: 'close' }).get());
  });

  expect(ui.guidePopup.query()).not.toBeInTheDocument();
});

it('should not show guide for those who dismissed it', () => {
  renderIssueNewStatusGuide(
    mockCurrentUser({
      isLoggedIn: true,
      dismissedNotices: {
        [NoticeType.ISSUE_GUIDE]: true,
        [NoticeType.ISSUE_NEW_STATUS_AND_TRANSITION_GUIDE]: true,
      },
    }),
  );

  expect(ui.guidePopup.query()).not.toBeInTheDocument();
});

it('should skip guide', async () => {
  const user = userEvent.setup();
  renderIssueNewStatusGuide();

  expect(await ui.guidePopup.find()).toBeInTheDocument();
  expect(ui.guidePopup.get()).toHaveTextContent('guiding.issue_accept.1.title');

  await user.click(ui.guidePopup.byRole('button', { name: 'skip' }).get());

  expect(ui.guidePopup.query()).not.toBeInTheDocument();
});

it('should not show guide if user is not logged in', () => {
  renderIssueNewStatusGuide(mockCurrentUser({ isLoggedIn: false }));

  expect(ui.guidePopup.query()).not.toBeInTheDocument();
});

it('should not show guide if there are no issues', () => {
  renderIssueNewStatusGuide(mockCurrentUser({ isLoggedIn: true }), []);

  expect(ui.guidePopup.query()).not.toBeInTheDocument();
});

it('should not show guide if CCT guide is shown', () => {
  renderIssueNewStatusGuide(
    mockCurrentUser({ isLoggedIn: true, dismissedNotices: { [NoticeType.ISSUE_GUIDE]: false } }),
    [],
  );

  expect(ui.guidePopup.query()).not.toBeInTheDocument();
});

function IssueNewStatusGuide({ issues }: { issues: Issue[] }) {
  const [open, setOpen] = React.useState(false);
  const issue = mockIssue(false, {
    transitions: [
      IssueTransition.Accept,
      IssueTransition.Confirm,
      IssueTransition.Resolve,
      IssueTransition.FalsePositive,
      IssueTransition.WontFix,
    ],
  });

  return (
    <div data-guiding-id={`issue-transition-${issue.key}`}>
      <div data-guiding-id="issue-accept-transition">/</div>
      <IssueTransitionComponent
        isOpen={open}
        togglePopup={() => setOpen(!open)}
        issue={issue}
        onChange={jest.fn()}
      />
      <IssueNewStatusAndTransitionGuide
        togglePopup={(_, __, show) => setOpen(Boolean(show))}
        run
        issues={issues}
      />
    </div>
  );
}

function renderIssueNewStatusGuide(
  currentUser = mockCurrentUser({
    isLoggedIn: true,
    dismissedNotices: { [NoticeType.ISSUE_GUIDE]: true },
  }),
  issues = [
    mockIssue(false, {
      transitions: [
        IssueTransition.Accept,
        IssueTransition.Confirm,
        IssueTransition.Resolve,
        IssueTransition.FalsePositive,
        IssueTransition.WontFix,
      ],
    }),
  ],
) {
  return renderComponent(
    <CurrentUserContextProvider currentUser={currentUser}>
      <IssueNewStatusGuide issues={issues} />
    </CurrentUserContextProvider>,
  );
}
