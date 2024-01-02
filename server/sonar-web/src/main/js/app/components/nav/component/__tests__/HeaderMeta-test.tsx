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
import { getAnalysisStatus } from '../../../../../api/ce';
import BranchesServiceMock from '../../../../../api/mocks/BranchesServiceMock';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockTask } from '../../../../../helpers/mocks/tasks';
import { mockCurrentUser, mockLoggedInUser } from '../../../../../helpers/testMocks';
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../../types/features';
import { TaskStatuses } from '../../../../../types/tasks';
import { CurrentUser } from '../../../../../types/users';
import HeaderMeta, { HeaderMetaProps } from '../HeaderMeta';

jest.mock('../../../../../api/ce');

const handler = new BranchesServiceMock();

beforeEach(() => handler.reset());

it('should render correctly for a branch with warnings', async () => {
  const user = userEvent.setup();
  jest.mocked(getAnalysisStatus).mockResolvedValue({
    component: {
      warnings: [{ dismissable: false, key: 'key', message: 'bar' }],
      key: 'compkey',
      name: 'me',
    },
  });
  renderHeaderMeta({}, undefined, 'branch=normal-branch&id=my-project');

  expect(await screen.findByText('version_x.0.0.1')).toBeInTheDocument();

  expect(
    await screen.findByText('project_navigation.analysis_status.warnings'),
  ).toBeInTheDocument();

  await user.click(screen.getByText('project_navigation.analysis_status.details_link'));

  expect(screen.getByRole('heading', { name: 'warnings' })).toBeInTheDocument();
});

it('should handle a branch with missing version and no warnings', () => {
  jest.mocked(getAnalysisStatus).mockResolvedValue({
    component: {
      warnings: [],
      key: 'compkey',
      name: 'me',
    },
  });
  renderHeaderMeta({ component: mockComponent({ version: undefined }) });

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

  expect(await screen.findByText('project_navigation.analysis_status.failed')).toBeInTheDocument();

  await user.click(screen.getByText('project_navigation.analysis_status.details_link'));

  expect(screen.getByRole('heading', { name: 'error' })).toBeInTheDocument();
});

it('should render correctly for a pull request', async () => {
  renderHeaderMeta({}, undefined, 'pullRequest=01&id=my-project');

  expect(
    await screen.findByText('branch_like_navigation.for_merge_into_x_from_y'),
  ).toBeInTheDocument();
  expect(screen.queryByText('version_x.0.0.1')).not.toBeInTheDocument();
});

it('should render correctly when the user is not logged in', () => {
  renderHeaderMeta({}, mockCurrentUser({ dismissedNotices: {} }));
  expect(screen.queryByText('homepage.current.is_default')).not.toBeInTheDocument();
  expect(screen.queryByText('homepage.current')).not.toBeInTheDocument();
  expect(screen.queryByText('homepage.check')).not.toBeInTheDocument();
});

function renderHeaderMeta(
  props: Partial<HeaderMetaProps> = {},
  currentUser: CurrentUser = mockLoggedInUser(),
  params?: string,
) {
  return renderApp('/', <HeaderMeta component={mockComponent({ version: '0.0.1' })} {...props} />, {
    currentUser,
    navigateTo: params ? `/?id=my-project&${params}` : '/?id=my-project',
    featureList: [Feature.BranchSupport],
  });
}
