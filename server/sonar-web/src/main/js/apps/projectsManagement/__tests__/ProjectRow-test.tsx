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
import ProjectRow from '../ProjectRow';

const project = {
  key: 'project',
  name: 'Project',
  qualifier: 'TRK',
  visibility: 'private'
};

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({ project: { ...project, lastAnalysisDate: '2017-04-08T00:00:00.000Z' } })
  ).toMatchSnapshot();
});

it('checks project', () => {
  const onProjectCheck = jest.fn();
  const wrapper = shallowRender({ onProjectCheck });
  wrapper.find('Checkbox').prop<Function>('onCheck')(false);
  expect(onProjectCheck).toBeCalledWith(project, false);
});

function shallowRender(props?: any) {
  return shallow(
    <ProjectRow
      currentUser={{ login: 'foo' }}
      onApplyTemplate={jest.fn()}
      onProjectCheck={jest.fn()}
      project={project}
      selected={true}
      {...props}
    />
  );
}
