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
import { change, submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import NewProjectForm from '../NewProjectForm';

jest.mock('../../../../api/components', () => ({
  createProject: () => Promise.resolve(),
  deleteProject: () => Promise.resolve()
}));

it('creates new project', async () => {
  const onDone = jest.fn();
  const wrapper = shallow(<NewProjectForm onDelete={jest.fn()} onDone={onDone} />);
  expect(wrapper).toMatchSnapshot();
  change(wrapper.find('input'), 'foo');
  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot(); // spinner
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(onDone).toBeCalledWith('foo');
});

it('deletes project', async () => {
  const onDelete = jest.fn();
  const wrapper = shallow(<NewProjectForm onDelete={onDelete} onDone={jest.fn()} />);
  wrapper.setState({ done: true, loading: false, projectKey: 'foo' });
  expect(wrapper).toMatchSnapshot();
  (wrapper.find('DeleteButton').prop('onClick') as Function)();
  wrapper.update();
  expect(wrapper).toMatchSnapshot(); // spinner
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(onDelete).toBeCalled();
});
