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
import { screen, waitFor } from '@testing-library/react';
import * as React from 'react';
import BranchesServiceMock from '../../../../api/mocks/BranchesServiceMock';
import ComputeEngineServiceMock from '../../../../api/mocks/ComputeEngineServiceMock';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockTask } from '../../../../helpers/mocks/tasks';
import { mockCurrentUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../types/component';
import { TaskStatuses, TaskTypes } from '../../../../types/tasks';
import { App } from '../App';

const handlerBranches = new BranchesServiceMock();
const handlerCe = new ComputeEngineServiceMock();

beforeEach(() => {
  handlerBranches.reset();
  handlerCe.reset();
});

it('should render Empty Overview for Application with no analysis', async () => {
  renderApp({ component: mockComponent({ qualifier: ComponentQualifier.Application }) });

  await appLoaded();

  expect(await screen.findByText('provisioning.no_analysis.application')).toBeInTheDocument();
});

it('should render Empty Overview on main branch with no analysis', async () => {
  renderApp({}, mockCurrentUser());

  await appLoaded();

  expect(
    await screen.findByText('provisioning.no_analysis_on_main_branch.main'),
  ).toBeInTheDocument();
});

it('should render Empty Overview on main branch with multiple branches with bad configuration', async () => {
  renderApp({ branchLikes: [mockBranch(), mockBranch()] });

  await appLoaded();

  expect(
    await screen.findByText(
      'provisioning.no_analysis_on_main_branch.bad_configuration.main.branches.main_branch',
    ),
  ).toBeInTheDocument();
});

it('should not render for portfolios and subportfolios', () => {
  const rtl = renderApp({
    component: mockComponent({ qualifier: ComponentQualifier.Portfolio }),
  });
  expect(rtl.container).toBeEmptyDOMElement();

  rtl.unmount();

  renderApp({
    component: mockComponent({ qualifier: ComponentQualifier.Portfolio }),
  });
  expect(rtl.container).toBeEmptyDOMElement();
});

describe('Permission provisioning', () => {
  beforeEach(() => {
    jest.useFakeTimers({ advanceTimers: true });
  });
  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });
  it('should render warning when permission is sync', async () => {
    handlerCe.addTask(
      mockTask({
        componentKey: 'my-project',
        type: TaskTypes.GithubProjectPermissionsProvisioning,
        status: TaskStatuses.InProgress,
      }),
    );

    renderApp();
    await jest.runOnlyPendingTimersAsync();

    expect(
      await screen.findByText('provisioning.permission_synch_in_progress'),
    ).toBeInTheDocument();

    handlerCe.clearTasks();
    handlerCe.addTask(
      mockTask({
        componentKey: 'my-project',
        type: TaskTypes.GithubProjectPermissionsProvisioning,
        status: TaskStatuses.Success,
      }),
    );

    await jest.runOnlyPendingTimersAsync();

    expect(screen.queryByText('provisioning.permission_synch_in_progress')).not.toBeInTheDocument();
  });
});

const appLoaded = async () => {
  await waitFor(() => {
    expect(screen.getByText('loading')).toBeInTheDocument();
  });

  await waitFor(() => {
    expect(screen.queryByText('loading')).not.toBeInTheDocument();
  });
};

function renderApp(props = {}, userProps = {}) {
  return renderComponent(
    <CurrentUserContextProvider currentUser={mockCurrentUser({ isLoggedIn: true, ...userProps })}>
      <App hasFeature={jest.fn().mockReturnValue(false)} component={mockComponent()} {...props} />
    </CurrentUserContextProvider>,
    '/?id=my-project',
  );
}
