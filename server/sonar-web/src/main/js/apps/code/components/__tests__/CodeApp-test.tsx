/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent, mockComponentMeasure } from '../../../../helpers/mocks/component';
import { mockIssue, mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../types/component';
import { loadMoreChildren, retrieveComponent } from '../../utils';
import { CodeApp } from '../CodeApp';

jest.mock('../../utils', () => {
  const { getCodeMetrics } = jest.requireActual('../../utils');
  return {
    getCodeMetrics,
    loadMoreChildren: jest.fn().mockResolvedValue({}),
    retrieveComponent: jest.fn().mockResolvedValue({
      breadcrumbs: [],
      component: { qualifier: 'APP' },
      components: [],
      page: 0,
      total: 1
    }),
    retrieveComponentChildren: () => Promise.resolve()
  };
});

const METRICS = {
  coverage: { id: '2', key: 'coverage', type: 'PERCENT', name: 'Coverage', domain: 'Coverage' },
  new_bugs: { id: '4', key: 'new_bugs', type: 'INT', name: 'New Bugs', domain: 'Reliability' }
};

beforeEach(() => {
  (retrieveComponent as jest.Mock<any>).mockClear();
});

it.each([
  [ComponentQualifier.Application],
  [ComponentQualifier.Project],
  [ComponentQualifier.Portfolio],
  [ComponentQualifier.SubPortfolio]
])('should render correclty when no sub component for %s', async qualifier => {
  const component = {
    breadcrumbs: [],
    name: 'foo',
    key: 'foo',
    qualifier,
    canBrowseAllChildProjects: true
  };
  (retrieveComponent as jest.Mock<any>).mockResolvedValueOnce({
    breadcrumbs: [],
    component,
    components: [],
    page: 0,
    total: 1
  });
  const wrapper = shallowRender({ component });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  wrapper.instance().handleSearchResults([]);
  expect(wrapper).toMatchSnapshot('no search');
  (retrieveComponent as jest.Mock<any>).mockResolvedValueOnce({
    breadcrumbs: [],
    component,
    components: [mockComponent({ qualifier: ComponentQualifier.File })],
    page: 0,
    total: 1
  });
  wrapper.instance().loadComponent(component.key);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('with sub component');
});

it('should refresh branch status if issues are updated', async () => {
  const fetchBranchStatus = jest.fn();
  const branchLike = mockPullRequest();
  const wrapper = shallowRender({ branchLike, fetchBranchStatus });
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  instance.handleIssueChange(mockIssue());
  expect(fetchBranchStatus).toBeCalledWith(branchLike, 'foo');
});

it('should load more behave correctly', async () => {
  const component1 = mockComponent();
  const component2 = mockComponent();
  (retrieveComponent as jest.Mock<any>).mockResolvedValueOnce({
    breadcrumbs: [],
    component: mockComponent(),
    components: [component1],
    page: 0,
    total: 1
  });
  let wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  (loadMoreChildren as jest.Mock<any>).mockResolvedValueOnce({
    components: [component2],
    page: 0,
    total: 1
  });

  wrapper.instance().handleLoadMore();
  expect(wrapper.state().components).toContainEqual(component1);
  expect(wrapper.state().components).toContainEqual(component2);

  (retrieveComponent as jest.Mock<any>).mockRejectedValueOnce({});
  wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.instance().handleLoadMore();
  expect(wrapper.state().components).toBeUndefined();
});

it('should handle go to parent correctly', async () => {
  const router = mockRouter();
  (retrieveComponent as jest.Mock<any>).mockResolvedValueOnce({
    breadcrumbs: [],
    component: mockComponent(),
    components: [],
    page: 0,
    total: 1
  });
  let wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.instance().handleGoToParent();
  expect(wrapper.state().highlighted).toBeUndefined();

  const breadcrumb = { key: 'key2', name: 'name2', qualifier: ComponentQualifier.Directory };
  (retrieveComponent as jest.Mock<any>).mockResolvedValueOnce({
    breadcrumbs: [
      { key: 'key1', name: 'name1', qualifier: ComponentQualifier.Directory },
      breadcrumb
    ],
    component: mockComponent(),
    components: [],
    page: 0,
    total: 1
  });
  wrapper = shallowRender({ router });
  await waitAndUpdate(wrapper);
  wrapper.instance().handleGoToParent();
  expect(wrapper.state().highlighted).toBe(breadcrumb);
  expect(router.push).toHaveBeenCalledWith({
    pathname: '/code',
    query: { id: 'foo', line: undefined, selected: 'key1' }
  });
});

it('should correcly display new/overall measure for portfolio', async () => {
  const component1 = mockComponent({ qualifier: ComponentQualifier.Project });
  const metrics = {
    reliability_rating: {
      id: '2',
      key: 'reliability_rating',
      type: 'RATING',
      name: 'reliability_rating',
      domain: 'reliability_rating'
    },
    new_reliability_rating: {
      id: '4',
      key: 'new_reliability_rating',
      type: 'RATING',
      name: 'new_reliability_rating',
      domain: 'new_reliability_rating'
    }
  };
  (retrieveComponent as jest.Mock<any>).mockResolvedValueOnce({
    breadcrumbs: [],
    component: mockComponent(),
    components: [component1],
    page: 0,
    total: 1
  });

  const wrapper = shallowRender({
    component: mockComponent({
      qualifier: ComponentQualifier.Portfolio,
      canBrowseAllChildProjects: true
    }),
    metrics
  });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('withKeyboardNavigation(Components)').props()).toMatchSnapshot('new metrics');
  wrapper.setState({ newCodeSelected: false });
  expect(wrapper.find('withKeyboardNavigation(Components)').props()).toMatchSnapshot(
    'overall metrics'
  );
});

it('should handle select correctly', () => {
  const router = mockRouter();
  const wrapper = shallowRender({ router });
  wrapper.setState({ highlighted: mockComponentMeasure() });

  wrapper.instance().handleSelect(mockComponentMeasure(true, { refKey: 'test' }));
  expect(router.push).toHaveBeenCalledWith({
    pathname: '/dashboard',
    query: { branch: undefined, id: 'test' }
  });
  expect(wrapper.state().highlighted).toBeUndefined();

  wrapper.instance().handleSelect(mockComponentMeasure());
  expect(router.push).toHaveBeenCalledWith({
    pathname: '/dashboard',
    query: { branch: undefined, id: 'test' }
  });
});

it('should render a warning message when user does not have access to all projects whithin a Portfolio', async () => {
  const wrapper = shallowRender({
    component: mockComponent({
      qualifier: ComponentQualifier.Portfolio,
      canBrowseAllChildProjects: false
    })
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('Project page with warning');
});

it.each([
  [ComponentQualifier.Portfolio, true, false],
  [ComponentQualifier.Project, false, false],
  [ComponentQualifier.Portfolio, false, true]
])(
  'should not render a warning message',
  async (
    componentQualifier: ComponentQualifier,
    canBrowseAllChildProjects: boolean,
    alertIsVisible: boolean
  ) => {
    const wrapper = shallowRender({
      component: mockComponent({
        qualifier: componentQualifier,
        canBrowseAllChildProjects
      })
    });
    await waitAndUpdate(wrapper);
    expect(wrapper.find('Styled(Alert)').exists()).toBe(alertIsVisible);
  }
);

function shallowRender(props: Partial<CodeApp['props']> = {}) {
  return shallow<CodeApp>(
    <CodeApp
      component={{
        breadcrumbs: [],
        name: 'foo',
        key: 'foo',
        qualifier: 'FOO'
      }}
      fetchBranchStatus={jest.fn()}
      location={{ query: { branch: 'b', id: 'foo', line: '7' } }}
      metrics={METRICS}
      router={mockRouter()}
      {...props}
    />
  );
}
