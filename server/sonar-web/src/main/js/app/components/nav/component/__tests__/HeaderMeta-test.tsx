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
import { mockBranch, mockPullRequest } from '../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockTask, mockTaskWarning } from '../../../../../helpers/mocks/tasks';
import { mockCurrentUser, mockLoggedInUser } from '../../../../../helpers/testMocks';
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import { TaskStatuses } from '../../../../../types/tasks';
import { CurrentUser } from '../../../../../types/users';
import HeaderMeta, { HeaderMetaProps } from '../HeaderMeta';

it('should render correctly for a branch with warnings', async () => {
  const user = userEvent.setup();

  renderHeaderMeta();

  expect(screen.getByText('version_x.0.0.1')).toBeInTheDocument();

  expect(screen.getByText('project_navigation.analysis_status.warnings')).toBeInTheDocument();

  await user.click(screen.getByText('project_navigation.analysis_status.details_link'));

  expect(screen.getByRole('heading', { name: 'warnings' })).toBeInTheDocument();
});

it('should handle a branch with missing version and no warnings', () => {
  renderHeaderMeta({ component: mockComponent({ version: undefined }), warnings: [] });

  expect(screen.queryByText('version_x.0.0.1')).not.toBeInTheDocument();
  expect(screen.queryByText('project_navigation.analysis_status.warnings')).not.toBeInTheDocument();
});

it('should render correctly with a failed analysis', async () => {
  const user = userEvent.setup();

  renderHeaderMeta({
    currentTask: mockTask({
      status: TaskStatuses.Failed,
      errorMessage: 'this is the error message',
    }),
  });

  expect(screen.getByText('project_navigation.analysis_status.failed')).toBeInTheDocument();

  await user.click(screen.getByText('project_navigation.analysis_status.details_link'));

  expect(screen.getByRole('heading', { name: 'error' })).toBeInTheDocument();
});

it('should render correctly for a pull request', () => {
  renderHeaderMeta({
    branchLike: mockPullRequest({
      url: 'https://example.com/pull/1234',
    }),
  });

  expect(screen.queryByText('version_x.0.0.1')).not.toBeInTheDocument();
  expect(screen.getByText('branch_like_navigation.for_merge_into_x_from_y')).toBeInTheDocument();
});

it('should render correctly when the user is not logged in', () => {
  renderHeaderMeta({}, mockCurrentUser({ dismissedNotices: {} }));
  expect(screen.queryByText('homepage.current.is_default')).not.toBeInTheDocument();
  expect(screen.queryByText('homepage.current')).not.toBeInTheDocument();
  expect(screen.queryByText('homepage.check')).not.toBeInTheDocument();
});

function renderHeaderMeta(
  props: Partial<HeaderMetaProps> = {},
  currentUser: CurrentUser = mockLoggedInUser()
) {
  return renderApp(
    '/',
    <HeaderMeta
      branchLike={mockBranch()}
      component={mockComponent({ version: '0.0.1' })}
      onWarningDismiss={jest.fn()}
      warnings={[
        mockTaskWarning({ key: '1', message: 'ERROR_1' }),
        mockTaskWarning({ key: '2', message: 'ERROR_2' }),
      ]}
      {...props}
    />,
    { currentUser }
  );
}
