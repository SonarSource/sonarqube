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
import BranchesServiceMock from '../../../../api/mocks/BranchesServiceMock';
import ComputeEngineServiceMock from '../../../../api/mocks/ComputeEngineServiceMock';
import { useComponent } from '../../../../app/components/componentContext/withComponentContext';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockTask, mockTaskWarning } from '../../../../helpers/mocks/tasks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { TaskStatuses } from '../../../../types/tasks';
import { AnalysisStatus } from '../AnalysisStatus';

const branchesHandler = new BranchesServiceMock();
const handler = new ComputeEngineServiceMock();

jest.mock('../../../../app/components/componentContext/withComponentContext', () => ({
  useComponent: jest.fn(() => ({
    isInProgress: true,
    isPending: false,
    currentTask: mockTask(),
    component: mockComponent(),
  })),
}));

beforeEach(() => {
  branchesHandler.reset();
  handler.reset();
});

it('renders correctly when there is a background task in progress', () => {
  renderAnalysisStatus();
  expect(
    screen.getByText('project_navigation.analysis_status.in_progress', { exact: false }),
  ).toBeInTheDocument();
});

it('renders correctly when there is a background task pending', () => {
  jest.mocked(useComponent).mockReturnValue({
    isInProgress: false,
    isPending: true,
    currentTask: mockTask(),
    onComponentChange: jest.fn(),
    fetchComponent: jest.fn(),
  });
  renderAnalysisStatus();
  expect(
    screen.getByText('project_navigation.analysis_status.pending', { exact: false }),
  ).toBeInTheDocument();
});

it('renders correctly when there is a failing background task', () => {
  jest.mocked(useComponent).mockReturnValue({
    isInProgress: false,
    isPending: false,
    currentTask: mockTask({ status: TaskStatuses.Failed }),
    onComponentChange: jest.fn(),
    fetchComponent: jest.fn(),
  });
  renderAnalysisStatus();
  expect(
    screen.getByText('project_navigation.analysis_status.failed', { exact: false }),
  ).toBeInTheDocument();
});

it('renders correctly when there are analysis warnings', async () => {
  const user = userEvent.setup();
  jest.mocked(useComponent).mockReturnValue({
    isInProgress: false,
    isPending: false,
    currentTask: mockTask(),
    onComponentChange: jest.fn(),
    fetchComponent: jest.fn(),
  });
  handler.setTaskWarnings([mockTaskWarning({ message: 'warning 1' })]);
  renderAnalysisStatus();

  await user.click(await screen.findByText('project_navigation.analysis_status.details_link'));
  expect(screen.getByText('warning 1')).toBeInTheDocument();
  await user.click(screen.getByText('close'));
  expect(screen.queryByText('warning 1')).not.toBeInTheDocument();
});

function renderAnalysisStatus(overrides: Partial<Parameters<typeof AnalysisStatus>[0]> = {}) {
  return renderComponent(
    <AnalysisStatus component={mockComponent()} {...overrides} />,
    '/?id=my-project',
  );
}
