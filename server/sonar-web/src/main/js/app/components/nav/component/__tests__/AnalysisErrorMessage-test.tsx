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
import * as React from 'react';
import BranchesServiceMock from '../../../../../api/mocks/BranchesServiceMock';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockTask } from '../../../../../helpers/mocks/tasks';
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../../types/features';
import { AnalysisErrorMessage } from '../AnalysisErrorMessage';

const handler = new BranchesServiceMock();

beforeEach(() => {
  handler.reset();
});

it('should work when error is on a different branch', () => {
  renderAnalysisErrorMessage({
    currentTask: mockTask({ branch: 'branch-1.2' }),
  });

  expect(screen.getByText(/component_navigation.status.failed_branch_X/)).toBeInTheDocument();
  expect(screen.getByText(/branch-1\.2/)).toBeInTheDocument();
});

it('should work for errors on Pull Requests', async () => {
  renderAnalysisErrorMessage(
    {
      currentTask: mockTask({ pullRequest: '01', pullRequestTitle: 'Fix stuff' }),
    },
    undefined,
    'pullRequest=01&id=my-project',
  );

  expect(await screen.findByText(/component_navigation.status.failed_X/)).toBeInTheDocument();
  expect(screen.getByText(/01 - Fix stuff/)).toBeInTheDocument();
});

it('should provide a link to admins', () => {
  renderAnalysisErrorMessage({
    component: mockComponent({ configuration: { showBackgroundTasks: true } }),
  });

  expect(screen.getByText(/component_navigation.status.failed_X.admin.link/)).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'background_tasks.page' })).toBeInTheDocument();
});

it('should explain to admins how to get the staktrace', () => {
  renderAnalysisErrorMessage(
    {
      component: mockComponent({ configuration: { showBackgroundTasks: true } }),
    },
    'project/background_tasks',
  );

  expect(screen.getByText(/component_navigation.status.failed_X.admin.help/)).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: 'background_tasks.page' })).not.toBeInTheDocument();
});

function renderAnalysisErrorMessage(
  overrides: Partial<Parameters<typeof AnalysisErrorMessage>[0]> = {},
  location = '/',
  params?: string,
) {
  return renderApp(
    location,
    <AnalysisErrorMessage
      component={mockComponent()}
      currentTask={mockTask()}
      onLeave={jest.fn()}
      {...overrides}
    />,
    { navigateTo: params ? `/?${params}` : undefined, featureList: [Feature.BranchSupport] },
  );
}
