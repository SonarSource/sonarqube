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
import App, { Props } from '../App';

jest.mock('lodash', () => {
  const lodash = require.requireActual('lodash');
  lodash.debounce = (fn: Function) => (...args: any[]) => fn(args);
  return lodash;
});

jest.mock('../../../api/components', () => ({ getComponents: jest.fn() }));

const getComponents = require('../../../api/components').getComponents as jest.Mock<any>;

const organization: T.Organization = { key: 'org', name: 'org', projectVisibility: 'public' };

const defaultSearchParameters = {
  organization: 'org',
  p: undefined,
  ps: 50,
  q: undefined
};

beforeEach(() => {
  getComponents
    .mockImplementation(() => Promise.resolve({ paging: { total: 0 }, components: [] }))
    .mockClear();
});

it('fetches all projects on mount', () => {
  shallowRender();
  expect(getComponents).lastCalledWith({ ...defaultSearchParameters, qualifiers: 'TRK' });
});

it('selects provisioned', () => {
  const wrapper = shallowRender();
  wrapper.find('Search').prop<Function>('onProvisionedChanged')(true);
  expect(getComponents).lastCalledWith({
    ...defaultSearchParameters,
    onProvisionedOnly: true,
    qualifiers: 'TRK'
  });
});

it('changes qualifier and resets provisioned', () => {
  const wrapper = shallowRender();
  wrapper.setState({ provisioned: true });
  wrapper.find('Search').prop<Function>('onQualifierChanged')('VW');
  expect(getComponents).lastCalledWith({ ...defaultSearchParameters, qualifiers: 'VW' });
});

it('searches', () => {
  const wrapper = shallowRender();
  wrapper.find('Search').prop<Function>('onSearch')('foo');
  expect(getComponents).lastCalledWith({ ...defaultSearchParameters, q: 'foo', qualifiers: 'TRK' });
});

it('loads more', () => {
  const wrapper = shallowRender();
  wrapper.find('ListFooter').prop<Function>('loadMore')();
  expect(getComponents).lastCalledWith({ ...defaultSearchParameters, p: 2, qualifiers: 'TRK' });
});

it('selects and deselects projects', async () => {
  getComponents.mockImplementation(() =>
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

  wrapper.find('Search').prop<Function>('onAllDeselected')();
  expect(wrapper.state('selection')).toEqual([]);

  wrapper.find('Search').prop<Function>('onAllSelected')();
  expect(wrapper.state('selection')).toEqual(['foo', 'bar']);
});

it('creates project', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('CreateProjectForm').exists()).toBeFalsy();

  wrapper.find('Header').prop<Function>('onProjectCreate')();
  wrapper.update();
  expect(wrapper.find('CreateProjectForm').exists()).toBeTruthy();

  wrapper.find('CreateProjectForm').prop<Function>('onProjectCreated')();
  wrapper.update();
  expect(getComponents.mock.calls).toHaveLength(2);

  wrapper.find('CreateProjectForm').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('CreateProjectForm').exists()).toBeFalsy();
});

it('changes default project visibility', () => {
  const onVisibilityChange = jest.fn();
  const wrapper = shallowRender({ onVisibilityChange });
  wrapper.find('Header').prop<Function>('onVisibilityChange')('private');
  expect(onVisibilityChange).toBeCalledWith('private');
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow(
    <App
      currentUser={{ login: 'foo' }}
      hasProvisionPermission={true}
      onOrganizationUpgrade={jest.fn()}
      onVisibilityChange={jest.fn()}
      organization={organization}
      topLevelQualifiers={['TRK', 'VW', 'APP']}
      {...props}
    />
  );
}
