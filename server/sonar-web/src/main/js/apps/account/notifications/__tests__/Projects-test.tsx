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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import Projects from '../Projects';

jest.mock('../../../../api/components', () => ({
  getSuggestions: jest.fn().mockResolvedValue({
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

const channels = ['channel1', 'channel2'];
const types = ['type1', 'type2'];

const projectFoo = { project: 'foo', projectName: 'Foo' };
const projectBar = { project: 'bar', projectName: 'Bar' };
const extraProps = {
  channel: 'channel1',
  type: 'type2'
};
const projects = [
  { ...projectFoo, ...extraProps },
  { ...projectBar, ...extraProps }
];

it('should render projects', () => {
  const wrapper = shallowRender({
    notifications: projects
  });

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.state()).toMatchSnapshot();
});

it('should handle project addition', () => {
  const wrapper = shallowRender();
  const { handleAddProject } = wrapper.instance();

  handleAddProject(projectFoo);

  expect(wrapper.state('addedProjects')).toEqual([
    {
      project: 'foo',
      projectName: 'Foo'
    }
  ]);
});

it('should handle search', () => {
  const wrapper = shallowRender();
  const { handleAddProject, handleSearch } = wrapper.instance();

  handleAddProject(projectFoo);
  handleAddProject(projectBar);

  handleSearch('Bar');
  expect(wrapper.state('search')).toBe('bar');
  expect(wrapper.find('ProjectNotifications')).toHaveLength(1);
});

it('should handle submit from modal', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleAddProject = jest.fn();
  const { handleAddProject, handleSubmit } = wrapper.instance();

  handleSubmit(projectFoo);
  await waitAndUpdate(wrapper);

  expect(handleAddProject).toHaveBeenCalledWith(projectFoo);
});

it('should toggle modal', () => {
  const wrapper = shallowRender();
  const { closeModal, openModal } = wrapper.instance();

  expect(wrapper.state('showModal')).toBe(false);

  openModal();
  expect(wrapper.state('showModal')).toBe(true);

  closeModal();
  expect(wrapper.state('showModal')).toBe(false);
});

function shallowRender(props?: Partial<Projects['props']>) {
  return shallow<Projects>(
    <Projects
      addNotification={jest.fn()}
      channels={channels}
      notifications={[]}
      removeNotification={jest.fn()}
      types={types}
      {...props}
    />
  );
}
