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
import { getComponents } from '../../../api/components';
import { changeProjectDefaultVisibility } from '../../../api/permissions';
import { getValue } from '../../../api/settings';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { ProjectManagementApp, Props } from '../ProjectManagementApp';

jest.mock('lodash', () => {
  const lodash = jest.requireActual('lodash');
  lodash.debounce =
    (fn: Function) =>
    (...args: any[]) =>
      fn(args);
  return lodash;
});

jest.mock('../../../api/components', () => ({
  getComponents: jest.fn().mockResolvedValue({ paging: { total: 0 }, components: [] }),
}));

jest.mock('../../../api/permissions', () => ({
  changeProjectDefaultVisibility: jest.fn().mockResolvedValue({}),
}));

jest.mock('../../../api/settings', () => ({
  getValue: jest.fn().mockResolvedValue({ value: 'public' }),
}));

const defaultSearchParameters = {
  p: undefined,
  ps: 50,
  q: undefined,
};

beforeEach(() => {
  jest.clearAllMocks();
});

it('fetches all projects on mount', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getComponents).toHaveBeenLastCalledWith({ ...defaultSearchParameters, qualifiers: 'TRK' });
  expect(getValue).toHaveBeenCalled();
  expect(wrapper.state().defaultProjectVisibility).toBe('public');
});

it('selects provisioned', () => {
  const wrapper = shallowRender();
  wrapper.find('withAppStateContext(Search)').prop<Function>('onProvisionedChanged')(true);
  expect(getComponents).toHaveBeenLastCalledWith({
    ...defaultSearchParameters,
    onProvisionedOnly: true,
    qualifiers: 'TRK',
  });
});

it('changes qualifier and resets provisioned', () => {
  const wrapper = shallowRender();
  wrapper.setState({ provisioned: true });
  wrapper.find('withAppStateContext(Search)').prop<Function>('onQualifierChanged')('VW');
  expect(getComponents).toHaveBeenLastCalledWith({ ...defaultSearchParameters, qualifiers: 'VW' });
});

it('searches', () => {
  const wrapper = shallowRender();
  wrapper.find('withAppStateContext(Search)').prop<Function>('onSearch')('foo');
  expect(getComponents).toHaveBeenLastCalledWith({
    ...defaultSearchParameters,
    q: 'foo',
    qualifiers: 'TRK',
  });
});

it('should handle date filtering', () => {
  const wrapper = shallowRender();
  wrapper.find('withAppStateContext(Search)').prop<Function>('onDateChanged')(
    '2019-11-14T06:55:02.663Z'
  );
  expect(getComponents).toHaveBeenCalledWith({
    ...defaultSearchParameters,
    qualifiers: 'TRK',
    analyzedBefore: '2019-11-14',
  });
});

it('should handle default project visibility change', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().defaultProjectVisibility).toBe('public');
  wrapper.instance().handleDefaultProjectVisibilityChange('private');

  expect(changeProjectDefaultVisibility).toHaveBeenCalledWith('private');
  await waitAndUpdate(wrapper);
  expect(wrapper.state().defaultProjectVisibility).toBe('private');
});

it('loads more', () => {
  const wrapper = shallowRender();
  wrapper.find('ListFooter').prop<Function>('loadMore')();
  expect(getComponents).toHaveBeenLastCalledWith({
    ...defaultSearchParameters,
    p: 2,
    qualifiers: 'TRK',
  });
});

it('selects and deselects projects', async () => {
  (getComponents as jest.Mock).mockImplementation(() =>
    Promise.resolve({ paging: { total: 2 }, components: [{ key: 'foo' }, { key: 'bar' }] })
  );
  const wrapper = shallowRender();
  await new Promise(setImmediate);

  wrapper.find('Projects').prop<Function>('onProjectSelected')('foo');
  expect(wrapper.state('selection')).toEqual(['foo']);

  wrapper.find('Projects').prop<Function>('onProjectSelected')('bar');
  expect(wrapper.state('selection')).toEqual(['foo', 'bar']);

  // should not select already selected project
  wrapper.find('Projects').prop<Function>('onProjectSelected')('bar');
  expect(wrapper.state('selection')).toEqual(['foo', 'bar']);

  wrapper.find('Projects').prop<Function>('onProjectDeselected')('foo');
  expect(wrapper.state('selection')).toEqual(['bar']);

  wrapper.find('withAppStateContext(Search)').prop<Function>('onAllDeselected')();
  expect(wrapper.state('selection')).toEqual([]);

  wrapper.find('withAppStateContext(Search)').prop<Function>('onAllSelected')();
  expect(wrapper.state('selection')).toEqual(['foo', 'bar']);
});

it('creates project', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('CreateProjectForm').exists()).toBe(false);

  wrapper.find('Header').prop<Function>('onProjectCreate')();
  wrapper.update();
  expect(wrapper.find('CreateProjectForm').exists()).toBe(true);

  wrapper.find('CreateProjectForm').prop<Function>('onProjectCreated')();
  wrapper.update();
  expect((getComponents as jest.Mock).mock.calls).toHaveLength(2);

  wrapper.find('CreateProjectForm').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('CreateProjectForm').exists()).toBe(false);
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow<ProjectManagementApp>(
    <ProjectManagementApp
      currentUser={mockLoggedInUser({ login: 'foo', permissions: { global: ['provisioning'] } })}
      {...props}
    />
  );
}
