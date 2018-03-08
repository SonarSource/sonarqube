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
// @flow
import * as React from 'react';
import { shallow, mount } from 'enzyme';
import App from '../App';
import { shallowWithIntl, waitAndUpdate } from '../../../../helpers/testUtils';

const replace = jest.fn();
const issues = [{ key: 'foo' }, { key: 'bar' }, { key: 'third' }, { key: 'fourth' }];
const facets = [{ property: 'severities', values: [{ val: 'MINOR', count: 4 }] }];
const paging = [{ pageIndex: 1, pageSize: 100, total: 4 }];

const eventNoShiftKey = { shiftKey: false };
const eventWithShiftKey = { shiftKey: true };

const PROPS = {
  branch: { isMain: true, name: 'master' },
  currentUser: {
    isLoggedIn: true,
    avatar: 'foo',
    email: 'forr@bar.com',
    login: 'JohnDoe',
    name: 'John Doe'
  },
  component: { key: 'foo', name: 'bar', organization: 'John', qualifier: 'Doe' },
  location: { pathname: '/issues', query: {} },
  fetchIssues: () => Promise.resolve({ facets, issues, paging }),
  onBranchesChange: () => {},
  onSonarCloud: false,
  organization: { key: 'foo' }
};

it('should render a list of issue', async () => {
  const wrapper = shallowWithIntl(<App {...PROPS} />, {
    context: { router: { replace } }
  });

  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);
});

it('should be able to check/uncheck a group of issues with the Shift key', async () => {
  const wrapper = shallowWithIntl(<App {...PROPS} />, {
    context: { router: { replace } }
  });

  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);

  wrapper.instance().handleIssueCheck('foo', eventNoShiftKey);
  expect(wrapper.state().checked.length).toBe(1);

  wrapper.instance().handleIssueCheck('fourth', eventWithShiftKey);
  expect(wrapper.state().checked.length).toBe(4);

  wrapper.instance().handleIssueCheck('third', eventNoShiftKey);
  expect(wrapper.state().checked.length).toBe(3);

  wrapper.instance().handleIssueCheck('foo', eventWithShiftKey);
  expect(wrapper.state().checked.length).toBe(1);
});

it('should avoid non-existing keys', async () => {
  const wrapper = shallowWithIntl(<App {...PROPS} />, {
    context: { router: { replace } }
  });

  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);

  wrapper.instance().handleIssueCheck('foo', eventNoShiftKey);
  expect(wrapper.state().checked.length).toBe(1);

  wrapper.instance().handleIssueCheck('non-existing-key', eventWithShiftKey);
  expect(wrapper.state().checked.length).toBe(1);
});

it('should be able to uncheck all issue with global checkbox', async () => {
  const wrapper = shallowWithIntl(<App {...PROPS} />, {
    context: { router: { replace } }
  });

  await waitAndUpdate(wrapper);
  expect(wrapper.state().issues.length).toBe(4);

  wrapper.instance().handleIssueCheck('foo', eventNoShiftKey);
  wrapper.instance().handleIssueCheck('bar', eventNoShiftKey);
  expect(wrapper.state().checked.length).toBe(2);

  wrapper.instance().onCheckAll(false);
  expect(wrapper.state().checked.length).toBe(0);
});

it('should be able to check all issue with global checkbox', async () => {
  const wrapper = shallowWithIntl(<App {...PROPS} />, {
    context: { router: { replace } }
  });

  await waitAndUpdate(wrapper);

  expect(wrapper.state().checked.length).toBe(0);
  wrapper.instance().onCheckAll(true);
  expect(wrapper.state().checked.length).toBe(wrapper.state().issues.length);
});
