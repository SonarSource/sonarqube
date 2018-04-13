/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { ComponentContainer } from '../ComponentContainer';
import { getBranches, getPullRequests } from '../../../api/branches';
import { getTasksForComponent, Task } from '../../../api/ce';
import { getComponentData } from '../../../api/components';
import { getComponentNavigation } from '../../../api/nav';
import {
  ShortLivingBranch,
  MainBranch,
  LongLivingBranch,
  PullRequest,
  BranchType
} from '../../types';

jest.mock('../../../api/branches', () => ({
  getBranches: jest.fn(() => Promise.resolve([])),
  getPullRequests: jest.fn(() => Promise.resolve([]))
}));

jest.mock('../../../api/ce', () => ({
  getTasksForComponent: jest.fn(() => Promise.resolve({ queue: [] }))
}));

jest.mock('../../../api/components', () => ({
  getComponentData: jest.fn(() => Promise.resolve({}))
}));

jest.mock('../../../api/nav', () => ({
  getComponentNavigation: jest.fn(() =>
    Promise.resolve({
      breadcrumbs: [{ key: 'portfolioKey', name: 'portfolio', qualifier: 'VW' }],
      key: 'portfolioKey'
    })
  )
}));

// mock this, because some of its children are using redux store
jest.mock('../nav/component/ComponentNav', () => ({
  default: () => null
}));

const Inner = () => <div />;

beforeEach(() => {
  (getBranches as jest.Mock).mockClear();
  (getPullRequests as jest.Mock).mockClear();
  (getComponentData as jest.Mock).mockClear();
  (getComponentNavigation as jest.Mock).mockClear();
  (getTasksForComponent as jest.Mock).mockClear();
});

it('changes component', () => {
  const wrapper = shallow(
    <ComponentContainer fetchOrganizations={jest.fn()} location={{ query: { id: 'foo' } }}>
      <Inner />
    </ComponentContainer>
  );
  (wrapper.instance() as ComponentContainer).mounted = true;
  wrapper.setState({
    branches: [{ isMain: true }],
    component: { qualifier: 'TRK', visibility: 'public' },
    loading: false
  });

  (wrapper.find(Inner).prop('onComponentChange') as Function)({ visibility: 'private' });
  expect(wrapper.state().component).toEqual({ qualifier: 'TRK', visibility: 'private' });
});

it("loads branches for module's project", async () => {
  (getComponentNavigation as jest.Mock<any>).mockImplementationOnce(() =>
    Promise.resolve({
      breadcrumbs: [
        { key: 'projectKey', name: 'project', qualifier: 'TRK' },
        { key: 'moduleKey', name: 'module', qualifier: 'BRC' }
      ]
    })
  );

  mount(
    <ComponentContainer fetchOrganizations={jest.fn()} location={{ query: { id: 'moduleKey' } }}>
      <Inner />
    </ComponentContainer>
  );

  await new Promise(setImmediate);
  expect(getBranches).toBeCalledWith('projectKey');
  expect(getPullRequests).toBeCalledWith('projectKey');
  expect(getComponentData).toBeCalledWith({ component: 'moduleKey', branch: undefined });
  expect(getComponentNavigation).toBeCalledWith({ componentKey: 'moduleKey', branch: undefined });
});

it("doesn't load branches portfolio", async () => {
  const wrapper = mount(
    <ComponentContainer fetchOrganizations={jest.fn()} location={{ query: { id: 'portfolioKey' } }}>
      <Inner />
    </ComponentContainer>
  );

  await new Promise(setImmediate);
  expect(getBranches).not.toBeCalled();
  expect(getPullRequests).not.toBeCalled();
  expect(getComponentData).toBeCalledWith({ component: 'portfolioKey', branch: undefined });
  expect(getComponentNavigation).toBeCalledWith({
    componentKey: 'portfolioKey',
    branch: undefined
  });
  wrapper.update();
  expect(wrapper.find(Inner).exists()).toBeTruthy();
});

it('updates branches on change', () => {
  const wrapper = shallow(
    <ComponentContainer fetchOrganizations={jest.fn()} location={{ query: { id: 'portfolioKey' } }}>
      <Inner />
    </ComponentContainer>
  );
  (wrapper.instance() as ComponentContainer).mounted = true;
  wrapper.setState({
    branches: [{ isMain: true }],
    component: { breadcrumbs: [{ key: 'projectKey', name: 'project', qualifier: 'TRK' }] },
    loading: false
  });
  (wrapper.find(Inner).prop('onBranchesChange') as Function)();
  expect(getBranches).toBeCalledWith('projectKey');
  expect(getPullRequests).toBeCalledWith('projectKey');
});

it('loads organization', async () => {
  (getComponentData as jest.Mock<any>).mockImplementationOnce(() =>
    Promise.resolve({ organization: 'org' })
  );

  const fetchOrganizations = jest.fn();
  mount(
    <ComponentContainer fetchOrganizations={fetchOrganizations} location={{ query: { id: 'foo' } }}>
      <Inner />
    </ComponentContainer>,
    { context: { organizationsEnabled: true } }
  );

  await new Promise(setImmediate);
  expect(fetchOrganizations).toBeCalledWith(['org']);
});

it('fetches status', async () => {
  (getComponentData as jest.Mock<any>).mockImplementationOnce(() =>
    Promise.resolve({ organization: 'org' })
  );

  mount(
    <ComponentContainer fetchOrganizations={jest.fn()} location={{ query: { id: 'foo' } }}>
      <Inner />
    </ComponentContainer>,
    { context: { organizationsEnabled: true } }
  );

  await new Promise(setImmediate);
  expect(getTasksForComponent).toBeCalledWith('portfolioKey');
});

it('filters correctly the pending tasks for a main branch', () => {
  const wrapper = shallow(
    <ComponentContainer fetchOrganizations={jest.fn()} location={{ query: { id: 'foo' } }}>
      <Inner />
    </ComponentContainer>
  );

  const component = wrapper.instance() as ComponentContainer;
  const mainBranch: MainBranch = { isMain: true, name: 'master' };
  const shortBranch: ShortLivingBranch = {
    isMain: false,
    mergeBranch: 'master',
    name: 'feature',
    type: BranchType.SHORT
  };
  const longBranch: LongLivingBranch = { isMain: false, name: 'branch-7.2', type: BranchType.LONG };
  const pullRequest: PullRequest = {
    base: 'feature',
    branch: 'feature',
    key: 'pr-89',
    title: 'PR Feature'
  };

  expect(component.isSameBranch({}, undefined)).toBeTruthy();
  expect(component.isSameBranch({}, mainBranch)).toBeTruthy();
  expect(component.isSameBranch({}, shortBranch)).toBeFalsy();
  expect(
    component.isSameBranch({ branch: 'feature', branchType: 'SHORT' }, shortBranch)
  ).toBeTruthy();
  expect(
    component.isSameBranch({ branch: 'feature', branchType: 'SHORT' }, longBranch)
  ).toBeFalsy();
  expect(
    component.isSameBranch({ branch: 'feature', branchType: 'SHORT' }, longBranch)
  ).toBeFalsy();
  expect(
    component.isSameBranch({ branch: 'branch-7.1', branchType: 'LONG' }, longBranch)
  ).toBeFalsy();
  expect(
    component.isSameBranch({ branch: 'branch-7.2', branchType: 'LONG' }, pullRequest)
  ).toBeFalsy();
  expect(component.isSameBranch({ pullRequest: 'pr-89' }, pullRequest)).toBeTruthy();

  const currentTask = { pullRequest: 'pr-89' } as Task;
  const pendingTasks = [
    currentTask,
    { branch: 'feature', branchType: 'SHORT' } as Task,
    {} as Task
  ];
  expect(component.getCurrentTask(undefined)).toBe(undefined);
  component.setState({ currentTask });
  expect(component.getCurrentTask(mainBranch)).toBe(undefined);
  expect(component.getCurrentTask(pullRequest)).toMatchObject(currentTask);
  component.setState({ pendingTasks });
  expect(component.getPendingTasks(mainBranch)).toMatchObject([{}]);
  expect(component.getPendingTasks(pullRequest)).toMatchObject([currentTask]);
});
