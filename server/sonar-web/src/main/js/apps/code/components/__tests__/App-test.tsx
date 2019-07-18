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
import { mockIssue, mockPullRequest, mockRouter } from '../../../../helpers/testMocks';
import { retrieveComponent } from '../../utils';
import { App } from '../App';

jest.mock('../../utils', () => ({
  retrieveComponent: jest.fn().mockResolvedValue({
    breadcrumbs: [],
    component: { qualifier: 'APP' },
    components: [],
    page: 0,
    total: 1
  }),
  retrieveComponentChildren: () => Promise.resolve()
}));

const METRICS = {
  coverage: { id: '2', key: 'coverage', type: 'PERCENT', name: 'Coverage', domain: 'Coverage' },
  new_bugs: { id: '4', key: 'new_bugs', type: 'INT', name: 'New Bugs', domain: 'Reliability' }
};

beforeEach(() => {
  (retrieveComponent as jest.Mock<any>).mockClear();
});

it('should have correct title for APP based component', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.find('HelmetWrapper')).toMatchSnapshot();
});

it('should have correct title for portfolio base component', async () => {
  (retrieveComponent as jest.Mock<any>).mockResolvedValueOnce({
    breadcrumbs: [],
    component: { qualifier: 'VW' },
    components: [],
    page: 0,
    total: 1
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.find('HelmetWrapper')).toMatchSnapshot();
});

it('should have correct title for project component', async () => {
  (retrieveComponent as jest.Mock<any>).mockResolvedValueOnce({
    breadcrumbs: [],
    component: { qualifier: 'TRK' },
    components: [],
    page: 0,
    total: 1
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.find('HelmetWrapper')).toMatchSnapshot();
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

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      component={{
        breadcrumbs: [],
        name: 'foo',
        key: 'foo',
        organization: 'foo',
        qualifier: 'FOO'
      }}
      fetchBranchStatus={jest.fn()}
      fetchMetrics={jest.fn()}
      location={{ query: { branch: 'b', id: 'foo', line: '7' } }}
      metrics={METRICS}
      router={mockRouter()}
      {...props}
    />
  );
}
