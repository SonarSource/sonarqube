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
import { change, elementKeydown, submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getSuggestions } from '../../../../api/components';
import ProjectModal from '../ProjectModal';

jest.mock('../../../../api/components', () => ({
  getSuggestions: jest.fn().mockResolvedValue({
    organizations: [{ key: 'org', name: 'Org' }],
    results: [
      {
        q: 'TRK',
        items: [
          { key: 'foo', name: 'Foo' },
          { key: 'bar', name: 'Bar' }
        ]
      },
      // this file should be ignored
      { q: 'FIL', items: [{ key: 'foo:file.js', name: 'file.js' }] }
    ]
  })
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender().dive()).toMatchSnapshot();
});

it('should trigger a search correctly', async () => {
  const wrapper = shallowRender();
  change(wrapper.dive().find('SearchBox'), 'foo');
  expect(getSuggestions).toBeCalledWith('foo');
  await waitAndUpdate(wrapper);
  expect(wrapper.dive().find('.notifications-add-project-search-results')).toMatchSnapshot();
});

it('should return an empty list when I search non-existent elements', async () => {
  (getSuggestions as jest.Mock<any>).mockResolvedValue({
    results: [
      { q: 'FIL', items: [], more: 0 },
      { q: 'TRK', items: [], more: 0 },
      { q: 'UTS', items: [], more: 0 }
    ],
    organizations: [],
    projects: []
  });

  const wrapper = shallowRender();
  change(wrapper.dive().find('SearchBox'), 'Supercalifragilisticexpialidocious');
  await waitAndUpdate(wrapper);
  expect(
    wrapper
      .dive()
      .find('.notifications-add-project-no-search-results')
      .exists()
  ).toBe(true);
});

it('should handle submit', async () => {
  const selectedProject = {
    projectName: 'Foo',
    project: 'foo'
  };
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit });
  wrapper.setState({
    selectedProject
  });
  submit(wrapper.dive().find('form'));
  await waitAndUpdate(wrapper);
  expect(onSubmit).toHaveBeenCalledWith(selectedProject);
});

it('should handle up and down keys', async () => {
  const foo = { project: 'foo', projectName: 'Foo' };
  const bar = { project: 'bar', projectName: 'Bar' };
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit });
  wrapper.setState({
    open: true,
    suggestions: [foo, bar]
  });
  await waitAndUpdate(wrapper);

  // Down.
  elementKeydown(wrapper.dive().find('SearchBox'), 40);
  expect(wrapper.state('highlighted')).toEqual(foo);
  elementKeydown(wrapper.dive().find('SearchBox'), 40);
  expect(wrapper.state('highlighted')).toEqual(bar);
  elementKeydown(wrapper.dive().find('SearchBox'), 40);
  expect(wrapper.state('highlighted')).toEqual(foo);

  // Up.
  elementKeydown(wrapper.dive().find('SearchBox'), 38);
  expect(wrapper.state('highlighted')).toEqual(bar);
  elementKeydown(wrapper.dive().find('SearchBox'), 38);
  expect(wrapper.state('highlighted')).toEqual(foo);
  elementKeydown(wrapper.dive().find('SearchBox'), 38);
  expect(wrapper.state('highlighted')).toEqual(bar);

  // Enter.
  elementKeydown(wrapper.dive().find('SearchBox'), 13);
  expect(wrapper.state('selectedProject')).toEqual(bar);
  expect(onSubmit).not.toHaveBeenCalled();
  elementKeydown(wrapper.dive().find('SearchBox'), 13);
  expect(onSubmit).toHaveBeenCalledWith(bar);
});

function shallowRender(props = {}) {
  return shallow<ProjectModal>(
    <ProjectModal addedProjects={[]} closeModal={jest.fn()} onSubmit={jest.fn()} {...props} />
  );
}
