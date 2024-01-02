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
import { shallow } from 'enzyme';
import * as React from 'react';
import { getProjectAlmBinding, validateProjectAlmBinding } from '../../../api/alm-settings';
import { getBranches, getPullRequests } from '../../../api/branches';
import { getAnalysisStatus, getTasksForComponent } from '../../../api/ce';
import { getComponentData } from '../../../api/components';
import { getComponentNavigation } from '../../../api/navigation';
import { mockProjectAlmBindingConfigurationErrors } from '../../../helpers/mocks/alm-settings';
import { mockBranch, mockMainBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockTask } from '../../../helpers/mocks/tasks';
import { HttpStatus } from '../../../helpers/request';
import { mockLocation, mockRouter } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { AlmKeys } from '../../../types/alm-settings';
import { ComponentQualifier } from '../../../types/component';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';
import { Component } from '../../../types/types';
import handleRequiredAuthorization from '../../utils/handleRequiredAuthorization';
import { ComponentContainer } from '../ComponentContainer';
import PageUnavailableDueToIndexation from '../indexation/PageUnavailableDueToIndexation';

jest.mock('../../../api/branches', () => {
  const { mockMainBranch, mockPullRequest } = jest.requireActual(
    '../../../helpers/mocks/branch-like'
  );
  return {
    getBranches: jest
      .fn()
      .mockResolvedValue([mockMainBranch({ status: { qualityGateStatus: 'OK' } })]),
    getPullRequests: jest
      .fn()
      .mockResolvedValue([
        mockPullRequest({ key: 'pr-89', status: { qualityGateStatus: 'ERROR' } }),
        mockPullRequest({ key: 'pr-90', title: 'PR Feature 2' }),
      ]),
  };
});

jest.mock('../../../api/ce', () => ({
  getAnalysisStatus: jest.fn().mockResolvedValue({ component: { warnings: [] } }),
  getTasksForComponent: jest.fn().mockResolvedValue({ queue: [] }),
}));

jest.mock('../../../api/components', () => ({
  getComponentData: jest.fn().mockResolvedValue({ component: { analysisDate: '2018-07-30' } }),
}));

jest.mock('../../../api/navigation', () => ({
  getComponentNavigation: jest.fn().mockResolvedValue({
    breadcrumbs: [{ key: 'portfolioKey', name: 'portfolio', qualifier: 'VW' }],
    key: 'portfolioKey',
  }),
}));

jest.mock('../../../api/alm-settings', () => ({
  getProjectAlmBinding: jest.fn().mockResolvedValue(undefined),
  validateProjectAlmBinding: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('../../utils/handleRequiredAuthorization', () => ({
  __esModule: true,
  default: jest.fn(),
}));

const Inner = () => <div />;

beforeEach(() => {
  jest.useFakeTimers();
  jest.clearAllMocks();
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

it('changes component', () => {
  const wrapper = shallowRender();
  wrapper.setState({
    branchLikes: [mockMainBranch()],
    component: { qualifier: 'TRK', visibility: 'public' } as Component,
    loading: false,
  });

  wrapper.instance().handleComponentChange({ visibility: 'private' });
  expect(wrapper.state().component).toEqual({ qualifier: 'TRK', visibility: 'private' });
});

it('loads the project binding, if any', async () => {
  const component = mockComponent({
    breadcrumbs: [{ key: 'foo', name: 'foo', qualifier: ComponentQualifier.Project }],
  });
  (getComponentNavigation as jest.Mock).mockResolvedValueOnce({});
  (getComponentData as jest.Mock<any>)
    .mockResolvedValueOnce({ component })
    .mockResolvedValueOnce({ component });
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce(undefined).mockResolvedValueOnce({
    alm: AlmKeys.GitHub,
    key: 'foo',
  });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getProjectAlmBinding).toHaveBeenCalled();
  expect(wrapper.state().projectBinding).toBeUndefined();

  wrapper.setProps({ location: mockLocation({ query: { id: 'bar' } }) });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectBinding).toEqual({ alm: AlmKeys.GitHub, key: 'foo' });
});

it("doesn't load branches portfolio", async () => {
  const wrapper = shallowRender({ location: mockLocation({ query: { id: 'portfolioKey' } }) });
  await waitAndUpdate(wrapper);
  expect(getBranches).not.toHaveBeenCalled();
  expect(getPullRequests).not.toHaveBeenCalled();
  expect(getComponentData).toHaveBeenCalledWith({ component: 'portfolioKey', branch: undefined });
  expect(getComponentNavigation).toHaveBeenCalledWith({
    component: 'portfolioKey',
    branch: undefined,
  });
});

it('updates branches on change', async () => {
  const updateBranchStatus = jest.fn();
  const wrapper = shallowRender({
    hasFeature: () => true,
    location: mockLocation({ query: { id: 'portfolioKey' } }),
    updateBranchStatus,
  });
  wrapper.setState({
    branchLikes: [mockMainBranch()],
    component: mockComponent({
      breadcrumbs: [{ key: 'projectKey', name: 'project', qualifier: 'TRK' }],
    }),
    loading: false,
  });
  wrapper.instance().handleBranchesChange();
  expect(getBranches).toHaveBeenCalledWith('projectKey');
  expect(getPullRequests).toHaveBeenCalledWith('projectKey');
  await waitAndUpdate(wrapper);
  expect(updateBranchStatus).toHaveBeenCalledTimes(2);
});

it('sets main branch when current branch is not found', async () => {
  const router = mockRouter();
  const wrapper = shallowRender({
    hasFeature: () => true,
    location: mockLocation({ query: { id: 'portfolioKey', branch: 'any-branch' } }),
    router,
  });
  await waitAndUpdate(wrapper);

  wrapper.instance().handleBranchesChange();
  await waitAndUpdate(wrapper);

  expect(router.replace).toHaveBeenCalledWith({ query: { id: 'portfolioKey' } });
});

it('fetches status', async () => {
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: {},
  });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getTasksForComponent).toHaveBeenCalledWith('portfolioKey');
});

it('filters correctly the pending tasks for a main branch', () => {
  const wrapper = shallowRender();
  const component = wrapper.instance();
  const mainBranch = mockMainBranch();
  const branch3 = mockBranch({ name: 'branch-3' });
  const branch2 = mockBranch({ name: 'branch-2' });
  const pullRequest = mockPullRequest();

  expect(component.isSameBranch({})).toBe(true);
  expect(component.isSameBranch({}, mainBranch)).toBe(true);
  expect(component.isSameBranch({ branch: mainBranch.name }, mainBranch)).toBe(true);
  expect(component.isSameBranch({}, branch3)).toBe(false);
  expect(component.isSameBranch({ branch: branch3.name }, branch3)).toBe(true);
  expect(component.isSameBranch({ branch: 'feature' }, branch2)).toBe(false);
  expect(component.isSameBranch({ branch: 'branch-6.6' }, branch2)).toBe(false);
  expect(component.isSameBranch({ branch: branch2.name }, branch2)).toBe(true);
  expect(component.isSameBranch({ branch: 'branch-6.7' }, pullRequest)).toBe(false);
  expect(component.isSameBranch({ pullRequest: pullRequest.key }, pullRequest)).toBe(true);

  const currentTask = mockTask({ pullRequest: pullRequest.key, status: TaskStatuses.InProgress });
  const failedTask = { ...currentTask, status: TaskStatuses.Failed };
  const pendingTasks = [currentTask, mockTask({ branch: branch3.name }), mockTask()];
  expect(component.getCurrentTask(currentTask)).toBeUndefined();
  expect(component.getCurrentTask(failedTask, mainBranch)).toBe(failedTask);
  expect(component.getCurrentTask(currentTask, mainBranch)).toBeUndefined();
  expect(component.getCurrentTask(currentTask, pullRequest)).toMatchObject(currentTask);
  expect(component.getPendingTasksForBranchLike(pendingTasks, mainBranch)).toMatchObject([{}]);
  expect(component.getPendingTasksForBranchLike(pendingTasks, pullRequest)).toMatchObject([
    currentTask,
  ]);
});

it('reload component after task progress finished', async () => {
  (getTasksForComponent as jest.Mock<any>)
    .mockResolvedValueOnce({
      queue: [{ id: 'foo', status: TaskStatuses.InProgress, type: TaskTypes.ViewRefresh }],
    })
    .mockResolvedValueOnce({
      queue: [],
    });
  const wrapper = shallowRender();

  // First round, there's something in the queue, and component navigation was
  // not called again (it's called once at mount, hence the 1 times assertion
  // here).
  await waitAndUpdate(wrapper);
  expect(getComponentNavigation).toHaveBeenCalledTimes(1);
  expect(getTasksForComponent).toHaveBeenCalledTimes(1);

  jest.runOnlyPendingTimers();

  // Second round, the queue is now empty, hence we assume the previous task
  // was done. We immediately load the component again.
  expect(getTasksForComponent).toHaveBeenCalledTimes(2);

  // Trigger the update.
  await waitAndUpdate(wrapper);
  // The component was correctly re-loaded.
  expect(getComponentNavigation).toHaveBeenCalledTimes(2);
  // The status API call will be called 1 final time after the component is
  // fully loaded, so the total will be 3.
  expect(getTasksForComponent).toHaveBeenCalledTimes(3);

  // Make sure the timeout was cleared. It should not be called again.
  jest.runAllTimers();
  await waitAndUpdate(wrapper);
  // The number of calls haven't changed.
  expect(getComponentNavigation).toHaveBeenCalledTimes(2);
  expect(getTasksForComponent).toHaveBeenCalledTimes(3);
});

it('reloads component after task progress finished, and moves straight to current', async () => {
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: { key: 'bar' },
  });
  (getTasksForComponent as jest.Mock<any>)
    .mockResolvedValueOnce({ queue: [] })
    .mockResolvedValueOnce({
      queue: [],
      current: { id: 'foo', status: TaskStatuses.Success, type: TaskTypes.AppRefresh },
    });
  const wrapper = shallowRender();

  // First round, nothing in the queue, and component navigation was not called
  // again (it's called once at mount, hence the 1 times assertion here).
  await waitAndUpdate(wrapper);
  expect(getComponentNavigation).toHaveBeenCalledTimes(1);
  expect(getTasksForComponent).toHaveBeenCalledTimes(1);

  jest.runOnlyPendingTimers();

  // Second round, nothing in the queue, BUT a success task is current. This
  // means the queue was processed too quick for us to see, and we didn't see
  // any pending tasks in the queue. So we immediately load the component again.
  expect(getTasksForComponent).toHaveBeenCalledTimes(2);

  // Trigger the update.
  await waitAndUpdate(wrapper);
  // The component was correctly re-loaded.
  expect(getComponentNavigation).toHaveBeenCalledTimes(2);
  // The status API call will be called 1 final time after the component is
  // fully loaded, so the total will be 3.
  expect(getTasksForComponent).toHaveBeenCalledTimes(3);
});

it('only fully loads a non-empty component once', async () => {
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: { key: 'bar', analysisDate: '2019-01-01' },
  });
  (getTasksForComponent as jest.Mock<any>).mockResolvedValueOnce({
    queue: [],
    current: { id: 'foo', status: TaskStatuses.Success, type: TaskTypes.Report },
  });
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  expect(getComponentNavigation).toHaveBeenCalledTimes(1);
  expect(getTasksForComponent).toHaveBeenCalledTimes(1);
});

it('only fully reloads a non-empty component if there was previously some task in progress', async () => {
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: { key: 'bar', analysisDate: '2019-01-01' },
  });
  (getTasksForComponent as jest.Mock<any>)
    .mockResolvedValueOnce({
      queue: [{ id: 'foo', status: TaskStatuses.InProgress, type: TaskTypes.AppRefresh }],
    })
    .mockResolvedValueOnce({
      queue: [],
      current: { id: 'foo', status: TaskStatuses.Success, type: TaskTypes.AppRefresh },
    });
  const wrapper = shallowRender();

  // First round, a pending task in the queue. This should trigger a reload of the
  // status endpoint.
  await waitAndUpdate(wrapper);
  jest.runOnlyPendingTimers();

  // Second round, nothing in the queue, and a success task is current. This
  // implies the current task was updated, and previously we displayed some information
  // about a pending task. This new information must prompt the component to reload
  // all data.
  expect(getTasksForComponent).toHaveBeenCalledTimes(2);

  // Trigger the update.
  await waitAndUpdate(wrapper);
  // The component was correctly re-loaded.
  expect(getComponentNavigation).toHaveBeenCalledTimes(2);
  // The status API call will be called 1 final time after the component is
  // fully loaded, so the total will be 3.
  expect(getTasksForComponent).toHaveBeenCalledTimes(3);
});

it('should show component not found if it does not exist', async () => {
  (getComponentNavigation as jest.Mock).mockRejectedValueOnce(
    new Response(null, { status: HttpStatus.NotFound })
  );
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should redirect if the user has no access', async () => {
  (getComponentNavigation as jest.Mock).mockRejectedValueOnce(
    new Response(null, { status: HttpStatus.Forbidden })
  );
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(handleRequiredAuthorization).toHaveBeenCalled();
});

it('should redirect if the component is a portfolio', async () => {
  const componentKey = 'comp-key';
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: { key: componentKey, breadcrumbs: [{ qualifier: ComponentQualifier.Portfolio }] },
  });

  const replace = jest.fn();

  const wrapper = shallowRender({
    location: mockLocation({ pathname: '/dashboard' }),
    router: mockRouter({ replace }),
  });
  await waitAndUpdate(wrapper);
  expect(replace).toHaveBeenCalledWith({ pathname: '/portfolio', search: `?id=${componentKey}` });
});

it('should display display the unavailable page if the component needs issue sync', async () => {
  (getComponentData as jest.Mock).mockResolvedValueOnce({
    component: { key: 'test', qualifier: ComponentQualifier.Project, needIssueSync: true },
  });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.find(PageUnavailableDueToIndexation).exists()).toBe(true);
});

it('should correctly reload last task warnings if anything got dismissed', async () => {
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: mockComponent({
      breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Project }],
    }),
  });
  (getComponentNavigation as jest.Mock).mockResolvedValueOnce({});

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  (getAnalysisStatus as jest.Mock).mockClear();

  wrapper.instance().handleWarningDismiss();
  expect(getAnalysisStatus).toHaveBeenCalledTimes(1);
});

describe('should correctly validate the project binding depending on the context', () => {
  const COMPONENT = mockComponent({
    breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Project }],
  });
  const PROJECT_BINDING_ERRORS = mockProjectAlmBindingConfigurationErrors();

  it.each([
    ["has an analysis; won't perform any check", { ...COMPONENT, analysisDate: '2020-01' }],
    ['has a project binding; check is OK', COMPONENT, undefined, 1],
    ['has a project binding; check is not OK', COMPONENT, PROJECT_BINDING_ERRORS, 1],
  ])('%s', async (_, component, projectBindingErrors = undefined, n = 0) => {
    (getComponentNavigation as jest.Mock).mockResolvedValueOnce({});
    (getComponentData as jest.Mock<any>).mockResolvedValueOnce({ component });

    if (n > 0) {
      (validateProjectAlmBinding as jest.Mock).mockResolvedValueOnce(projectBindingErrors);
    }

    const wrapper = shallowRender({ hasFeature: () => true });
    await waitAndUpdate(wrapper);
    expect(wrapper.state().projectBindingErrors).toBe(projectBindingErrors);

    expect(validateProjectAlmBinding).toHaveBeenCalledTimes(n);
  });
});

it.each([
  [ComponentQualifier.Application],
  [ComponentQualifier.Portfolio],
  [ComponentQualifier.SubPortfolio],
])(
  'should not care about PR decoration settings for %s',
  async (componentQualifier: ComponentQualifier) => {
    const component = mockComponent({
      breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: componentQualifier }],
    });
    (getComponentNavigation as jest.Mock).mockResolvedValueOnce({});
    (getComponentData as jest.Mock<any>).mockResolvedValueOnce({ component });

    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);

    expect(getProjectAlmBinding).not.toHaveBeenCalled();
    expect(validateProjectAlmBinding).not.toHaveBeenCalled();
  }
);

function shallowRender(props: Partial<ComponentContainer['props']> = {}) {
  return shallow<ComponentContainer>(
    <ComponentContainer
      hasFeature={jest.fn().mockReturnValue(false)}
      location={mockLocation({ query: { id: 'foo' } })}
      updateBranchStatus={jest.fn()}
      router={mockRouter()}
      {...props}
    >
      <Inner />
    </ComponentContainer>
  );
}
