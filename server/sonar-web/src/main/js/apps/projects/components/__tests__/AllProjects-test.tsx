/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { get, save } from 'sonar-ui-common/helpers/storage';
import { ComponentQualifier } from '../../../../types/component';
import {
  AllProjects,
  LS_PROJECTS_SORT,
  LS_PROJECTS_VIEW,
  LS_PROJECTS_VISUALIZATION
} from '../AllProjects';

jest.mock('../ProjectsList', () => ({
  // eslint-disable-next-line
  default: function ProjectsList() {
    return null;
  }
}));

jest.mock('../PageHeader', () => ({
  // eslint-disable-next-line
  default: function PageHeader() {
    return null;
  }
}));

jest.mock('../PageSidebar', () => ({
  // eslint-disable-next-line
  default: function PageSidebar() {
    return null;
  }
}));

jest.mock('../../utils', () => {
  const utils = require.requireActual('../../utils');
  utils.fetchProjects = jest.fn(() => Promise.resolve({ projects: [] }));
  return utils;
});

jest.mock('sonar-ui-common/helpers/storage', () => ({
  get: jest.fn(() => null),
  save: jest.fn()
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
  wrapper.setState({ query: { view: 'visualizations' } });
  expect(wrapper).toMatchSnapshot();
});

it('fetches projects', () => {
  shallowRender();
  expect(fetchProjects).lastCalledWith(
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
      visualization: undefined
    },
    false,
    undefined
  );
});

it('redirects to the saved search', () => {
  const localeStorageMock: T.Dict<string> = {
    [LS_PROJECTS_VIEW]: 'leak',
    [LS_PROJECTS_SORT]: 'coverage',
    [LS_PROJECTS_VISUALIZATION]: 'security'
  };

  (get as jest.Mock).mockImplementation((key: string) => localeStorageMock[key]);
  const replace = jest.fn();
  shallowRender({}, jest.fn(), replace);

  expect(replace).lastCalledWith({
    pathname: '/projects',
    query: {
      view: localeStorageMock[LS_PROJECTS_VIEW],
      sort: localeStorageMock[LS_PROJECTS_SORT],
      visualization: localeStorageMock[LS_PROJECTS_VISUALIZATION]
    }
  });
});

it('changes sort', () => {
  const push = jest.fn();
  const wrapper = shallowRender({}, push);
  wrapper.find('PageHeader').prop<Function>('onSortChange')('size', false);
  expect(push).lastCalledWith({ pathname: '/projects', query: { sort: 'size' } });
  expect(save).lastCalledWith(LS_PROJECTS_SORT, 'size');
});

it('changes perspective to leak', () => {
  const push = jest.fn();
  const wrapper = shallowRender({}, push);
  wrapper.find('PageHeader').prop<Function>('onPerspectiveChange')({ view: 'leak' });
  expect(push).lastCalledWith({
    pathname: '/projects',
    query: { view: 'leak', visualization: undefined }
  });
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_SORT, undefined);
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_VIEW, 'leak');
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_VISUALIZATION, undefined);
});

it('updates sorting when changing perspective from leak', () => {
  const push = jest.fn();
  const wrapper = shallowRender({}, push);
  wrapper.setState({ query: { sort: 'new_coverage', view: 'leak' } });
  wrapper.find('PageHeader').prop<Function>('onPerspectiveChange')({
    view: undefined
  });
  expect(push).lastCalledWith({
    pathname: '/projects',
    query: { sort: 'coverage', view: undefined, visualization: undefined }
  });
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_SORT, 'coverage');
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_VIEW, undefined);
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_VISUALIZATION, undefined);
});

it('changes perspective to risk visualization', () => {
  const push = jest.fn();
  const wrapper = shallowRender({}, push);
  wrapper.find('PageHeader').prop<Function>('onPerspectiveChange')({
    view: 'visualizations',
    visualization: 'risk'
  });
  expect(push).lastCalledWith({
    pathname: '/projects',
    query: { view: 'visualizations', visualization: 'risk' }
  });
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_SORT, undefined);
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_VIEW, 'visualizations');
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_VISUALIZATION, 'risk');
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
      currentUser={{ isLoggedIn: true }}
      isFavorite={false}
      location={{ pathname: '/projects', query: {} }}
      organization={undefined}
      qualifiers={[ComponentQualifier.Project, ComponentQualifier.Application]}
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
        visibility: 'public'
      }
    ],
    total: 0
  });
  return wrapper;
}
