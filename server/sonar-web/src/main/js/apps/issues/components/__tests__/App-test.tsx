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
import * as React from 'react';
import { shallow } from 'enzyme';
import { App } from '../App';
import { mockCurrentUser, mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';

const ISSUES = [
  { key: 'foo' } as T.Issue,
  { key: 'bar' } as T.Issue,
  { key: 'third' } as T.Issue,
  { key: 'fourth' } as T.Issue
];
const FACETS = [{ property: 'severities', values: [{ val: 'MINOR', count: 4 }] }];
const PAGING = { pageIndex: 1, pageSize: 100, total: 4 };

const eventNoShiftKey = { shiftKey: false } as MouseEvent;
const eventWithShiftKey = { shiftKey: true } as MouseEvent;

const referencedComponent = { key: 'foo-key', name: 'bar', organization: 'John', uuid: 'foo-uuid' };

it('should render a list of issue', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);
  expect(wrapper.state().referencedComponentsById).toEqual({ 'foo-uuid': referencedComponent });
  expect(wrapper.state().referencedComponentsByKey).toEqual({ 'foo-key': referencedComponent });
});

it('should be able to check/uncheck a group of issues with the Shift key', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);

  const instance = wrapper.instance();
  instance.handleIssueCheck('foo', eventNoShiftKey);
  expect(wrapper.state().checked.length).toBe(1);

  instance.handleIssueCheck('fourth', eventWithShiftKey);
  expect(wrapper.state().checked.length).toBe(4);

  instance.handleIssueCheck('third', eventNoShiftKey);
  expect(wrapper.state().checked.length).toBe(3);

  instance.handleIssueCheck('foo', eventWithShiftKey);
  expect(wrapper.state().checked.length).toBe(1);
});

it('should avoid non-existing keys', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);

  const instance = wrapper.instance();
  instance.handleIssueCheck('foo', eventNoShiftKey);
  expect(wrapper.state().checked.length).toBe(1);

  instance.handleIssueCheck('non-existing-key', eventWithShiftKey);
  expect(wrapper.state().checked.length).toBe(1);
});

it('should be able to uncheck all issue with global checkbox', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);

  const instance = wrapper.instance();
  instance.handleIssueCheck('foo', eventNoShiftKey);
  instance.handleIssueCheck('bar', eventNoShiftKey);
  expect(wrapper.state().checked.length).toBe(2);

  instance.handleCheckAll(false);
  expect(wrapper.state().checked.length).toBe(0);
});

it('should be able to check all issue with global checkbox', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  const instance = wrapper.instance();
  expect(wrapper.state().checked.length).toBe(0);
  instance.handleCheckAll(true);
  expect(wrapper.state().checked.length).toBe(wrapper.state().issues.length);
});

it('should check all issues, even the ones that are not visible', async () => {
  const wrapper = shallowRender({
    fetchIssues: jest.fn().mockResolvedValue({
      components: [referencedComponent],
      effortTotal: 1,
      facets: FACETS,
      issues: ISSUES,
      languages: [],
      paging: { pageIndex: 1, pageSize: 100, total: 250 },
      rules: [],
      users: []
    })
  });
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  // Checking all issues should show the correct count in the Bulk Change button.
  instance.handleCheckAll(true);
  waitAndUpdate(wrapper);
  expect(wrapper.find('#issues-bulk-change')).toMatchSnapshot();
});

it('should check max 500 issues', async () => {
  const wrapper = shallowRender({
    fetchIssues: jest.fn().mockResolvedValue({
      components: [referencedComponent],
      effortTotal: 1,
      facets: FACETS,
      issues: ISSUES,
      languages: [],
      paging: { pageIndex: 1, pageSize: 100, total: 1000 },
      rules: [],
      users: []
    })
  });
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  // Checking all issues should show 500 in the Bulk Change button, and display
  // a warning.
  instance.handleCheckAll(true);
  waitAndUpdate(wrapper);
  expect(wrapper.find('#issues-bulk-change')).toMatchSnapshot();
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      component={{
        breadcrumbs: [],
        key: 'foo',
        name: 'bar',
        organization: 'John',
        qualifier: 'Doe'
      }}
      currentUser={mockCurrentUser()}
      fetchIssues={jest.fn().mockResolvedValue({
        components: [referencedComponent],
        effortTotal: 1,
        facets: FACETS,
        issues: ISSUES,
        languages: [],
        paging: PAGING,
        rules: [],
        users: []
      })}
      location={{ pathname: '/issues', query: {} }}
      onBranchesChange={() => {}}
      organization={{ key: 'foo' }}
      router={mockRouter()}
      userOrganizations={[]}
      {...props}
    />
  );
}
