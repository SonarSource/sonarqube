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
import Projects, { Props } from '../Projects';

jest.mock('../../../../api/components', () => ({
  getSuggestions: jest.fn(() =>
    Promise.resolve({
      results: [
        {
          q: 'TRK',
          items: [
            { key: 'foo', name: 'Foo', organization: 'org' },
            { key: 'bar', name: 'Bar', organization: 'org' }
          ]
        },
        // this file should be ignored
        { q: 'FIL', items: [{ key: 'foo:file.js', name: 'file.js', organization: 'org' }] }
      ]
    })
  )
}));

const channels = ['channel1', 'channel2'];
const types = ['type1', 'type2'];

const projectFoo = { key: 'foo', name: 'Foo', organization: 'org' };
const projectBar = { key: 'bar', name: 'Bar', organization: 'org' };
const projects = [projectFoo, projectBar];

const newProject = { key: 'qux', name: 'Qux', organization: 'org' };

it('should render projects', () => {
  const wrapper = shallowRender({
    notificationsByProject: {
      foo: [
        {
          channel: 'channel1',
          organization: 'org',
          project: 'foo',
          projectName: 'Foo',
          type: 'type1'
        },
        {
          channel: 'channel1',
          organization: 'org',
          project: 'foo',
          projectName: 'Foo',
          type: 'type2'
        }
      ]
    },
    projects
  });
  expect(wrapper).toMatchSnapshot();

  // let's add a new project
  wrapper.setState({ addedProjects: [newProject] });
  expect(wrapper).toMatchSnapshot();

  // let's say we saved it, so it's passed back in `props`
  wrapper.setProps({ projects: [...projects, newProject] });
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.state()).toMatchSnapshot();
});

it('should search projects', () => {
  const wrapper = shallowRender({ projects: [projectBar] });
  const loadOptions = wrapper.find('AsyncSelect').prop<Function>('loadOptions');
  expect(loadOptions('')).resolves.toEqual({ options: [] });
  // should not contain `projectBar`
  expect(loadOptions('more than two symbols')).resolves.toEqual({
    options: [{ label: 'Foo', organization: 'org', value: 'foo' }]
  });
});

it('should add project', () => {
  const wrapper = shallowRender();
  expect(wrapper.state('addedProjects')).toEqual([]);
  wrapper.find('AsyncSelect').prop<Function>('onChange')({
    label: 'Qwe',
    organization: 'org',
    value: 'qwe'
  });
  expect(wrapper.state('addedProjects')).toEqual([
    { key: 'qwe', name: 'Qwe', organization: 'org' }
  ]);
});

it('should render option', () => {
  const wrapper = shallowRender();
  const optionRenderer = wrapper.find('AsyncSelect').prop<Function>('optionRenderer');
  expect(
    shallow(
      optionRenderer({
        label: 'Qwe',
        organization: 'org',
        value: 'qwe'
      })
    )
  ).toMatchSnapshot();
});

function shallowRender(props?: Partial<Props>) {
  return shallow(
    <Projects
      addNotification={jest.fn()}
      channels={channels}
      notificationsByProject={{}}
      projects={[]}
      removeNotification={jest.fn()}
      types={types}
      {...props}
    />
  );
}
