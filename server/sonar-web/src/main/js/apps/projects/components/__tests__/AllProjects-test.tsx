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
import { get, save } from '../../../../helpers/storage';
import { mockAppState, mockLocation } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { AllProjects, LS_PROJECTS_SORT, LS_PROJECTS_VIEW } from '../AllProjects';

jest.mock(
  '../ProjectsList',
  () =>
    // eslint-disable-next-line
    function ProjectsList() {
      return null;
    }
);

jest.mock(
  '../PageHeader',
  () =>
    // eslint-disable-next-line
    function PageHeader() {
      return null;
    }
);

jest.mock(
  '../PageSidebar',
  () =>
    // eslint-disable-next-line
    function PageSidebar() {
      return null;
    }
);

jest.mock('../../utils', () => ({
  ...jest.requireActual('../../utils'),
  fetchProjects: jest.fn(() => Promise.resolve({ projects: [] })),
}));

jest.mock('../../../../helpers/storage', () => ({
  get: jest.fn(() => null),
  save: jest.fn(),
}));

const fetchProjects = require('../../utils').fetchProjects as jest.Mock;

beforeEach(() => {
  (get as jest.Mock).mockImplementation(() => null);
  (save as jest.Mock).mockClear();
  fetchProjects.mockClear();
});

it('renders', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('fetches projects', () => {
  shallowRender();
  expect(fetchProjects).toHaveBeenLastCalledWith(
    {
      coverage: undefined,
      duplications: undefined,
      gate: undefined,
      languages: undefined,
      maintainability: undefined,
      new_coverage: undefined,
      new_duplications: undefined,
      new_lines: undefined,
      new_maintainability: undefined,
      new_reliability: undefined,
      new_security: undefined,
      reliability: undefined,
      search: undefined,
      security: undefined,
      size: undefined,
      sort: undefined,
      tags: undefined,
      view: undefined,
    },
    false
  );
});

it('changes sort', () => {
  const push = jest.fn();
  const wrapper = shallowRender({}, push);
  wrapper.find('PageHeader').prop<Function>('onSortChange')('size', false);
  expect(push).toHaveBeenLastCalledWith({ pathname: '/projects', query: { sort: 'size' } });
  expect(save).toHaveBeenLastCalledWith(LS_PROJECTS_SORT, 'size');
});

it('changes perspective to leak', () => {
  const push = jest.fn();
  const wrapper = shallowRender({}, push);
  wrapper.find('PageHeader').prop<Function>('onPerspectiveChange')({ view: 'leak' });
  expect(push).toHaveBeenLastCalledWith({
    pathname: '/projects',
    query: { view: 'leak' },
  });
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_SORT, undefined);
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_VIEW, 'leak');
});

it('updates sorting when changing perspective from leak', () => {
  const push = jest.fn();
  const wrapper = shallowRender({}, push);
  wrapper.setState({ query: { sort: 'new_coverage', view: 'leak' } });
  wrapper.find('PageHeader').prop<Function>('onPerspectiveChange')({
    view: undefined,
  });
  expect(push).toHaveBeenLastCalledWith({
    pathname: '/projects',
    query: { sort: 'coverage', view: undefined },
  });
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_SORT, 'coverage');
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_VIEW, undefined);
});

it('handles favorite projects', () => {
  const wrapper = shallowRender();
  expect(wrapper.state('projects')).toMatchSnapshot();

  wrapper.instance().handleFavorite('foo', true);
  expect(wrapper.state('projects')).toMatchSnapshot();
});

function shallowRender(
  props: Partial<AllProjects['props']> = {},
  push = jest.fn(),
  replace = jest.fn()
) {
  const wrapper = shallow<AllProjects>(
    <AllProjects
      currentUser={{ isLoggedIn: true, dismissedNotices: {} }}
      isFavorite={false}
      location={mockLocation({ pathname: '/projects', query: {} })}
      appState={mockAppState({
        qualifiers: [ComponentQualifier.Project, ComponentQualifier.Application],
      })}
      router={{ push, replace }}
      {...props}
    />
  );
  wrapper.setState({
    loading: false,
    projects: [
      {
        key: 'foo',
        measures: {},
        name: 'Foo',
        qualifier: ComponentQualifier.Project,
        tags: [],
        visibility: 'public',
      },
    ],
    total: 0,
  });
  return wrapper;
}
