/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
jest.mock('../../../api/branches', () => ({ getBranches: jest.fn() }));
jest.mock('../../../api/components', () => ({ getComponentData: jest.fn() }));
jest.mock('../../../api/nav', () => ({ getComponentNavigation: jest.fn() }));

import * as React from 'react';
import { shallow, mount } from 'enzyme';
import ProjectContainer from '../ProjectContainer';
import { getBranches } from '../../../api/branches';
import { getComponentData } from '../../../api/components';
import { getComponentNavigation } from '../../../api/nav';
import { doAsync } from '../../../helpers/testUtils';

beforeEach(() => {
  (getBranches as jest.Mock<any>).mockClear();
  (getComponentData as jest.Mock<any>).mockClear();
  (getComponentNavigation as jest.Mock<any>).mockClear();
});

it('changes component', () => {
  const Inner = () => <div />;

  const wrapper = shallow(
    <ProjectContainer location={{ query: { id: 'foo' } }}>
      <Inner />
    </ProjectContainer>
  );
  (wrapper.instance() as ProjectContainer).mounted = true;
  wrapper.setState({
    branches: [{ isMain: true }],
    component: { qualifier: 'TRK', visibility: 'public' },
    loading: false
  });

  (wrapper.find(Inner).prop('onComponentChange') as Function)({ visibility: 'private' });
  expect(wrapper.state().component).toEqual({ qualifier: 'TRK', visibility: 'private' });
});

it("loads branches for module's project", () => {
  (getBranches as jest.Mock<any>).mockImplementation(() => Promise.resolve([]));
  (getComponentData as jest.Mock<any>).mockImplementation(() => Promise.resolve({}));
  (getComponentNavigation as jest.Mock<any>).mockImplementation(() =>
    Promise.resolve({
      breadcrumbs: [
        { key: 'projectKey', name: 'project', qualifier: 'TRK' },
        { key: 'moduleKey', name: 'module', qualifier: 'BRC' }
      ]
    })
  );

  mount(
    <ProjectContainer location={{ query: { id: 'moduleKey' } }}>
      <div />
    </ProjectContainer>
  );

  return doAsync().then(() => {
    expect(getBranches).toBeCalledWith('projectKey');
    expect(getComponentData).toBeCalledWith('moduleKey', undefined);
    expect(getComponentNavigation).toBeCalledWith('moduleKey');
  });
});

it("doesn't load branches portfolio", () => {
  (getBranches as jest.Mock<any>).mockImplementation(() => Promise.resolve([]));
  (getComponentData as jest.Mock<any>).mockImplementation(() => Promise.resolve({}));
  (getComponentNavigation as jest.Mock<any>).mockImplementation(() =>
    Promise.resolve({
      breadcrumbs: [{ key: 'portfolioKey', name: 'portfolio', qualifier: 'VW' }]
    })
  );

  mount(
    <ProjectContainer location={{ query: { id: 'portfolioKey' } }}>
      <div />
    </ProjectContainer>
  );

  return doAsync().then(() => {
    expect(getBranches).not.toBeCalled();
    expect(getComponentData).toBeCalledWith('portfolioKey', undefined);
    expect(getComponentNavigation).toBeCalledWith('portfolioKey');
  });
});

it('updates branches on change', () => {
  (getBranches as jest.Mock<any>).mockImplementation(() => Promise.resolve([]));
  const Inner = () => <div />;
  const wrapper = shallow(
    <ProjectContainer location={{ query: { id: 'portfolioKey' } }}>
      <Inner />
    </ProjectContainer>
  );
  (wrapper.instance() as ProjectContainer).mounted = true;
  wrapper.setState({
    branches: [{ isMain: true }],
    component: { breadcrumbs: [{ key: 'projectKey', name: 'project', qualifier: 'TRK' }] },
    loading: false
  });
  (wrapper.find(Inner).prop('onBranchesChange') as Function)();
  expect(getBranches).toBeCalledWith('projectKey');
});
