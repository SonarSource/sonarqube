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

import { shallow } from 'enzyme';
import * as React from 'react';
import { getProjectAlmBinding, validateProjectAlmBinding } from '../../../api/alm-settings';
import { getBranches, getPullRequests } from '../../../api/branches';
import { getTasksForComponent } from '../../../api/ce';
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
import { ComponentQualifier, Visibility } from '../../../types/component';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';
import { Component } from '../../../types/types';
import handleRequiredAuthorization from '../../utils/handleRequiredAuthorization';
import { ComponentContainer } from '../ComponentContainer';

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

// eslint-disable-next-line react/function-component-definition
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
    component: {
      qualifier: ComponentQualifier.Project,
      visibility: Visibility.Public,
    } as Component,
    loading: false,
  });

  wrapper.instance().handleComponentChange({ visibility: Visibility.Private });

  expect(wrapper.state().component).toEqual({
    qualifier: ComponentQualifier.Project,
    visibility: Visibility.Private,
  });
});

it('loads the project binding, if any', async () => {
  const component = mockComponent({
    breadcrumbs: [{ key: 'foo', name: 'foo', qualifier: ComponentQualifier.Project }],
  });

  jest
    .mocked(getComponentNavigation)
    .mockResolvedValueOnce({} as unknown as Awaited<ReturnType<typeof getComponentNavigation>>);

  jest
    .mocked(getComponentData)
    .mockResolvedValueOnce({ component } as unknown as Awaited<ReturnType<typeof getComponentData>>)
    .mockResolvedValueOnce({ component } as unknown as Awaited<
      ReturnType<typeof getComponentData>
    >);

  jest
    .mocked(getProjectAlmBinding)
    .mockResolvedValueOnce(undefined as unknown as Awaited<ReturnType<typeof getProjectAlmBinding>>)
    .mockResolvedValueOnce({
      alm: AlmKeys.GitHub,
      key: 'foo',
    } as unknown as Awaited<ReturnType<typeof getProjectAlmBinding>>);

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

it('fetches status', async () => {
  jest.mocked(getComponentData).mockResolvedValueOnce({
    component: {},
  } as unknown as Awaited<ReturnType<typeof getComponentData>>);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getTasksForComponent).toHaveBeenCalledWith('portfolioKey');
});

it('filters correctly the pending tasks for a main branch', () => {
  const wrapper = shallowRender();
  const component = wrapper.instance();
  const mainBranch = mockMainBranch();
  const branch3 = mockBranch({ name: 'branch-3' });
  const pullRequest = mockPullRequest();

  expect(component.isSameBranch({})).toBe(true);
  wrapper.setProps({ location: mockLocation({ query: { branch: mainBranch.name } }) });
  expect(component.isSameBranch({ branch: mainBranch.name })).toBe(true);
  expect(component.isSameBranch({})).toBe(false);
  wrapper.setProps({ location: mockLocation({ query: { branch: branch3.name } }) });
  expect(component.isSameBranch({ branch: branch3.name })).toBe(true);
  wrapper.setProps({ location: mockLocation({ query: { pullRequest: pullRequest.key } }) });
  expect(component.isSameBranch({ pullRequest: pullRequest.key })).toBe(true);

  const currentTask = mockTask({ pullRequest: pullRequest.key, status: TaskStatuses.InProgress });
  const failedTask = { ...currentTask, status: TaskStatuses.Failed };
  const pendingTasks = [currentTask, mockTask({ branch: branch3.name }), mockTask()];
  expect(component.getCurrentTask(failedTask)).toBe(failedTask);
  wrapper.setProps({ location: mockLocation({ query: {} }) });
  expect(component.getCurrentTask(currentTask)).toBeUndefined();
  wrapper.setProps({ location: mockLocation({ query: { pullRequest: pullRequest.key } }) });
  expect(component.getCurrentTask(currentTask)).toMatchObject(currentTask);

  expect(component.getPendingTasksForBranchLike(pendingTasks)).toMatchObject([currentTask]);
  wrapper.setProps({ location: mockLocation({ query: {} }) });
  expect(component.getPendingTasksForBranchLike(pendingTasks)).toMatchObject([{}]);
});

it('reload component after task progress finished', async () => {
  jest
    .mocked(getTasksForComponent)
    .mockResolvedValueOnce({
      queue: [{ id: 'foo', status: TaskStatuses.InProgress, type: TaskTypes.ViewRefresh }],
    } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>)
    .mockResolvedValueOnce({
      queue: [],
    } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

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
  jest.mocked(getComponentData).mockResolvedValueOnce({
    component: { key: 'bar' },
  } as unknown as Awaited<ReturnType<typeof getComponentData>>);

  jest
    .mocked(getTasksForComponent)
    .mockResolvedValueOnce({ queue: [] } as unknown as Awaited<
      ReturnType<typeof getTasksForComponent>
    >)
    .mockResolvedValueOnce({
      queue: [],
      current: { id: 'foo', status: TaskStatuses.Success, type: TaskTypes.AppRefresh },
    } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

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
  jest.mocked(getComponentData).mockResolvedValueOnce({
    component: { key: 'bar', analysisDate: '2019-01-01' },
  } as unknown as Awaited<ReturnType<typeof getComponentData>>);

  jest.mocked(getTasksForComponent).mockResolvedValueOnce({
    queue: [],
    current: { id: 'foo', status: TaskStatuses.Success, type: TaskTypes.Report },
  } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  expect(getComponentNavigation).toHaveBeenCalledTimes(1);
  expect(getTasksForComponent).toHaveBeenCalledTimes(1);
});

it('should redirect if in tutorials and ends the first analyses', async () => {
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: { key: 'bar', analysisDate: undefined },
  });
  (getTasksForComponent as jest.Mock<any>).mockResolvedValueOnce({
    queue: [],
    current: { id: 'foo', status: TaskStatuses.Success, type: TaskTypes.Report },
  });

  const replace = jest.fn();
  const wrapper = shallowRender({
    location: mockLocation({ pathname: '/tutorials' }),
    router: mockRouter({ replace }),
  });

  await waitAndUpdate(wrapper);
  expect(replace).toHaveBeenCalledTimes(1);
});

it('only fully reloads a non-empty component if there was previously some task in progress', async () => {
  jest.mocked(getComponentData).mockResolvedValueOnce({
    component: { key: 'bar', analysisDate: '2019-01-01' },
  } as unknown as Awaited<ReturnType<typeof getComponentData>>);

  jest
    .mocked(getTasksForComponent)
    .mockResolvedValueOnce({
      queue: [{ id: 'foo', status: TaskStatuses.InProgress, type: TaskTypes.AppRefresh }],
    } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>)
    .mockResolvedValueOnce({
      queue: [],
      current: { id: 'foo', status: TaskStatuses.Success, type: TaskTypes.AppRefresh },
    } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

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
  jest
    .mocked(getComponentNavigation)
    .mockRejectedValueOnce(new Response(null, { status: HttpStatus.NotFound }));

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should redirect if the user has no access', async () => {
  jest
    .mocked(getComponentNavigation)
    .mockRejectedValueOnce(new Response(null, { status: HttpStatus.Forbidden }));

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(handleRequiredAuthorization).toHaveBeenCalled();
});

it('should redirect if the component is a portfolio', async () => {
  const componentKey = 'comp-key';

  jest.mocked(getComponentData).mockResolvedValueOnce({
    component: { key: componentKey, breadcrumbs: [{ qualifier: ComponentQualifier.Portfolio }] },
  } as unknown as Awaited<ReturnType<typeof getComponentData>>);

  const replace = jest.fn();

  const wrapper = shallowRender({
    location: mockLocation({ pathname: '/dashboard' }),
    router: mockRouter({ replace }),
  });

  await waitAndUpdate(wrapper);
  expect(replace).toHaveBeenCalledWith({ pathname: '/portfolio', search: `?id=${componentKey}` });
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
    jest
      .mocked(getComponentNavigation)
      .mockResolvedValueOnce({} as unknown as Awaited<ReturnType<typeof getComponentNavigation>>);

    jest
      .mocked(getComponentData)
      .mockResolvedValueOnce({ component } as unknown as Awaited<
        ReturnType<typeof getComponentData>
      >);

    if (n > 0) {
      jest.mocked(validateProjectAlmBinding).mockResolvedValueOnce(projectBindingErrors);
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

    jest
      .mocked(getComponentNavigation)
      .mockResolvedValueOnce({} as unknown as Awaited<ReturnType<typeof getComponentNavigation>>);

    jest
      .mocked(getComponentData)
      .mockResolvedValueOnce({ component } as unknown as Awaited<
        ReturnType<typeof getComponentData>
      >);

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
      router={mockRouter()}
      {...props}
    >
      <Inner />
    </ComponentContainer>
  );
}
