/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
jest.mock('../ProjectsListContainer', () => ({
  default: function ProjectsListContainer() {
    return null;
  }
}));

jest.mock('../ProjectsListFooterContainer', () => ({
  default: function ProjectsListFooterContainer() {
    return null;
  }
}));
jest.mock('../PageHeaderContainer', () => ({
  default: function PageHeaderContainer() {
    return null;
  }
}));

jest.mock('../PageSidebar', () => ({
  default: function PageSidebar() {
    return null;
  }
}));

jest.mock('../../../../helpers/storage', () => ({
  getSort: () => null,
  getView: jest.fn(() => null),
  getVisualization: () => null,
  saveSort: jest.fn(),
  saveView: jest.fn(),
  saveVisualization: jest.fn()
}));

import * as React from 'react';
import { mount, shallow } from 'enzyme';
import AllProjects from '../AllProjects';
import { getView, saveSort, saveView, saveVisualization } from '../../../../helpers/storage';

beforeEach(() => {
  (getView as jest.Mock<any>).mockImplementation(() => null);
  (saveSort as jest.Mock<any>).mockClear();
  (saveView as jest.Mock<any>).mockClear();
  (saveVisualization as jest.Mock<any>).mockClear();
});

it('renders', () => {
  const wrapper = shallow(
    <AllProjects
      fetchProjects={jest.fn()}
      isFavorite={false}
      location={{ pathname: '/projects', query: {} }}
    />,
    { context: { router: {} } }
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ query: { view: 'visualizations' } });
  expect(wrapper).toMatchSnapshot();
});

it('fetches projects', () => {
  const fetchProjects = jest.fn();
  mountRender({ fetchProjects });
  expect(fetchProjects).lastCalledWith(
    {
      coverage: null,
      duplications: null,
      gate: null,
      languages: null,
      maintainability: null,
      new_coverage: null,
      new_duplications: null,
      new_lines: null,
      new_maintainability: null,
      new_reliability: null,
      new_security: null,
      reliability: null,
      search: null,
      security: null,
      size: null,
      sort: null,
      tags: null,
      view: undefined,
      visualization: null
    },
    false,
    undefined
  );
});

it('redirects to the saved search', () => {
  (getView as jest.Mock<any>).mockImplementation(() => 'leak');
  const replace = jest.fn();
  mountRender({}, jest.fn(), replace);
  expect(replace).lastCalledWith({ pathname: '/projects', query: { view: 'leak' } });
});

it('changes sort', () => {
  const push = jest.fn();
  const wrapper = mountRender({}, push);
  wrapper.find('PageHeaderContainer').prop<Function>('onSortChange')('size', false);
  expect(push).lastCalledWith({ pathname: '/projects', query: { sort: 'size' } });
  expect(saveSort).lastCalledWith('size');
});

it('changes perspective to leak', () => {
  const push = jest.fn();
  const wrapper = mountRender({}, push);
  wrapper.find('PageHeaderContainer').prop<Function>('onPerspectiveChange')({ view: 'leak' });
  expect(push).lastCalledWith({
    pathname: '/projects',
    query: { view: 'leak', visualization: undefined }
  });
  expect(saveSort).lastCalledWith(undefined);
  expect(saveView).lastCalledWith('leak');
  expect(saveVisualization).lastCalledWith(undefined);
});

it('updates sorting when changing perspective from leak', () => {
  const push = jest.fn();
  const wrapper = mountRender(
    { location: { pathname: '/projects', query: { sort: 'new_coverage', view: 'leak' } } },
    push
  );
  wrapper.find('PageHeaderContainer').prop<Function>('onPerspectiveChange')({
    view: undefined
  });
  expect(push).lastCalledWith({
    pathname: '/projects',
    query: { sort: 'coverage', view: undefined, visualization: undefined }
  });
  expect(saveSort).lastCalledWith('coverage');
  expect(saveView).lastCalledWith(undefined);
  expect(saveVisualization).lastCalledWith(undefined);
});

it('changes perspective to risk visualization', () => {
  const push = jest.fn();
  const wrapper = mountRender({}, push);
  wrapper.find('PageHeaderContainer').prop<Function>('onPerspectiveChange')({
    view: 'visualizations',
    visualization: 'risk'
  });
  expect(push).lastCalledWith({
    pathname: '/projects',
    query: { view: 'visualizations', visualization: 'risk' }
  });
  expect(saveSort).lastCalledWith(undefined);
  expect(saveView).lastCalledWith('visualizations');
  expect(saveVisualization).lastCalledWith('risk');
});

function mountRender(props: any = {}, push: Function = jest.fn(), replace: Function = jest.fn()) {
  return mount(
    <AllProjects
      fetchProjects={jest.fn()}
      isFavorite={false}
      location={{ pathname: '/projects', query: {} }}
      {...props}
    />,
    { context: { router: { push, replace } } }
  );
}
