/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getBranches, getPullRequests } from '../../../api/branches';
import { getTasksForComponent } from '../../../api/ce';
import { getComponentData } from '../../../api/components';
import { getComponentNavigation } from '../../../api/nav';
import { STATUSES } from '../../../apps/background-tasks/constants';
import { isSonarCloud } from '../../../helpers/system';
import {
  mockComponent,
  mockLocation,
  mockLongLivingBranch,
  mockMainBranch,
  mockPullRequest,
  mockRouter,
  mockShortLivingBranch
} from '../../../helpers/testMocks';
import { ComponentContainer } from '../ComponentContainer';

jest.mock('../../../api/branches', () => {
  const { mockMainBranch, mockPullRequest } = require.requireActual('../../../helpers/testMocks');
  return {
    getBranches: jest
      .fn()
      .mockResolvedValue([mockMainBranch({ status: { qualityGateStatus: 'OK' } })]),
    getPullRequests: jest
      .fn()
      .mockResolvedValue([
        mockPullRequest({ key: 'pr-89', status: { qualityGateStatus: 'ERROR' } }),
        mockPullRequest({ key: 'pr-90', title: 'PR Feature 2' })
      ])
  };
});

jest.mock('../../../api/ce', () => ({
  getAnalysisStatus: jest.fn().mockResolvedValue({ component: { warnings: [] } }),
  getTasksForComponent: jest.fn().mockResolvedValue({ queue: [] })
}));

jest.mock('../../../api/components', () => ({
  getComponentData: jest.fn().mockResolvedValue({ component: { analysisDate: '2018-07-30' } })
}));

jest.mock('../../../api/nav', () => ({
  getComponentNavigation: jest.fn().mockResolvedValue({
    breadcrumbs: [{ key: 'portfolioKey', name: 'portfolio', qualifier: 'VW' }],
    key: 'portfolioKey'
  })
}));

jest.mock('../../../helpers/system', () => ({
  isSonarCloud: jest.fn().mockReturnValue(false)
}));

// mock this, because some of its children are using redux store
jest.mock('../nav/component/ComponentNav', () => ({
  default: () => null
}));

const Inner = () => <div />;

const mainBranch: T.MainBranch = { isMain: true, name: 'master' };

beforeEach(() => {
  jest.clearAllMocks();
});

it('changes component', () => {
  const wrapper = shallowRender();
  wrapper.setState({
    branchLikes: [{ isMain: true, name: 'master' }],
    component: { qualifier: 'TRK', visibility: 'public' } as T.Component,
    loading: false
  });

  (wrapper.find(Inner).prop('onComponentChange') as Function)({ visibility: 'private' });
  expect(wrapper.state().component).toEqual({ qualifier: 'TRK', visibility: 'private' });
});

it("doesn't load branches portfolio", async () => {
  const wrapper = shallowRender({ location: mockLocation({ query: { id: 'portfolioKey' } }) });
  await new Promise(setImmediate);
  expect(getBranches).not.toBeCalled();
  expect(getPullRequests).not.toBeCalled();
  expect(getComponentData).toBeCalledWith({ component: 'portfolioKey', branch: undefined });
  expect(getComponentNavigation).toBeCalledWith({ component: 'portfolioKey', branch: undefined });
  wrapper.update();
  expect(wrapper.find(Inner).exists()).toBeTruthy();
});

it('updates branches on change', async () => {
  const registerBranchStatus = jest.fn();
  const wrapper = shallowRender({
    location: mockLocation({ query: { id: 'portfolioKey' } }),
    registerBranchStatus
  });
  wrapper.setState({
    branchLikes: [mainBranch],
    component: mockComponent({
      breadcrumbs: [{ key: 'projectKey', name: 'project', qualifier: 'TRK' }]
    }),
    loading: false
  });
  wrapper.find(Inner).prop<Function>('onBranchesChange')();
  expect(getBranches).toBeCalledWith('projectKey');
  expect(getPullRequests).toBeCalledWith('projectKey');
  await waitAndUpdate(wrapper);
  expect(registerBranchStatus).toBeCalledTimes(2);
});

it('loads organization', async () => {
  (isSonarCloud as jest.Mock).mockReturnValue(true);
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: { organization: 'org' }
  });

  const fetchOrganization = jest.fn();
  shallowRender({ fetchOrganization });
  await new Promise(setImmediate);
  expect(fetchOrganization).toBeCalledWith('org');
});

it('fetches status', async () => {
  (getComponentData as jest.Mock<any>).mockResolvedValueOnce({
    component: { organization: 'org' }
  });

  shallowRender();
  await new Promise(setImmediate);
  expect(getTasksForComponent).toBeCalledWith('portfolioKey');
});

it('filters correctly the pending tasks for a main branch', () => {
  const wrapper = shallowRender();
  const component = wrapper.instance();
  const mainBranch = mockMainBranch();
  const shortBranch = mockShortLivingBranch();
  const longBranch = mockLongLivingBranch();
  const pullRequest = mockPullRequest();

  expect(component.isSameBranch({}, undefined)).toBeTruthy();
  expect(component.isSameBranch({}, mainBranch)).toBeTruthy();
  expect(component.isSameBranch({}, shortBranch)).toBeFalsy();
  expect(
    component.isSameBranch({ branch: shortBranch.name, branchType: 'SHORT' }, shortBranch)
  ).toBeTruthy();
  expect(
    component.isSameBranch({ branch: 'feature', branchType: 'SHORT' }, longBranch)
  ).toBeFalsy();
  expect(
    component.isSameBranch({ branch: 'feature', branchType: 'SHORT' }, longBranch)
  ).toBeFalsy();
  expect(
    component.isSameBranch({ branch: 'branch-6.6', branchType: 'LONG' }, longBranch)
  ).toBeFalsy();
  expect(
    component.isSameBranch({ branch: longBranch.name, branchType: 'LONG' }, longBranch)
  ).toBeTruthy();
  expect(
    component.isSameBranch({ branch: 'branch-6.7', branchType: 'LONG' }, pullRequest)
  ).toBeFalsy();
  expect(component.isSameBranch({ pullRequest: pullRequest.key }, pullRequest)).toBeTruthy();

  const currentTask = { pullRequest: pullRequest.key, status: STATUSES.IN_PROGRESS } as T.Task;
  const failedTask = { ...currentTask, status: STATUSES.FAILED };
  const pendingTasks = [
    currentTask,
    { branch: shortBranch.name, branchType: 'SHORT' } as T.Task,
    {} as T.Task
  ];
  expect(component.getCurrentTask(currentTask, undefined)).toBe(undefined);
  expect(component.getCurrentTask(failedTask, mainBranch)).toBe(failedTask);
  expect(component.getCurrentTask(currentTask, mainBranch)).toBe(undefined);
  expect(component.getCurrentTask(currentTask, pullRequest)).toMatchObject(currentTask);
  expect(component.getPendingTasks(pendingTasks, mainBranch)).toMatchObject([{}]);
  expect(component.getPendingTasks(pendingTasks, pullRequest)).toMatchObject([currentTask]);
});

it('reload component after task progress finished', async () => {
  jest.useFakeTimers();
  const inProgressTask = { id: 'foo', status: STATUSES.IN_PROGRESS } as T.Task;
  (getTasksForComponent as jest.Mock<any>).mockResolvedValueOnce({ queue: [inProgressTask] });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getComponentNavigation).toHaveBeenCalledTimes(1);
  expect(getTasksForComponent).toHaveBeenCalledTimes(1);

  jest.runAllTimers();
  expect(getTasksForComponent).toHaveBeenCalledTimes(2);
  await waitAndUpdate(wrapper);
  expect(getComponentNavigation).toHaveBeenCalledTimes(2);
  expect(getTasksForComponent).toHaveBeenCalledTimes(3);

  jest.runAllTimers();
  await waitAndUpdate(wrapper);
  expect(getComponentNavigation).toHaveBeenCalledTimes(2);
  expect(getTasksForComponent).toHaveBeenCalledTimes(3);
});

it('should show component not found if it does not exist', async () => {
  (getComponentNavigation as jest.Mock).mockRejectedValue({ status: 404 });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should redirect if the user has no access', async () => {
  (getComponentNavigation as jest.Mock).mockRejectedValue({ status: 403 });
  const requireAuthorization = jest.fn();
  const wrapper = shallowRender({ requireAuthorization });
  await waitAndUpdate(wrapper);
  expect(requireAuthorization).toBeCalled();
});

function shallowRender(props: Partial<ComponentContainer['props']> = {}) {
  return shallow<ComponentContainer>(
    <ComponentContainer
      fetchOrganization={jest.fn()}
      location={mockLocation({ query: { id: 'foo' } })}
      registerBranchStatus={jest.fn()}
      requireAuthorization={jest.fn()}
      router={mockRouter()}
      {...props}>
      <Inner />
    </ComponentContainer>
  );
}
