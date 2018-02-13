/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import { mount } from 'enzyme';
import NewProjectForm from '../NewProjectForm';
import { change, submit, waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/components', () => ({
  createProject: () => Promise.resolve(),
  deleteProject: () => Promise.resolve()
}));

jest.mock('../../../../components/icons-components/ClearIcon');

it('creates new project', async () => {
  const onDone = jest.fn();
  const wrapper = mount(<NewProjectForm onDelete={jest.fn()} onDone={onDone} />);
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
  const wrapper = mount(<NewProjectForm onDelete={onDelete} onDone={jest.fn()} />);
  wrapper.setState({ done: true, loading: false, projectKey: 'foo' });
  expect(wrapper).toMatchSnapshot();
  wrapper.find('DeleteButton').prop('onClick')();
  wrapper.update();
  expect(wrapper).toMatchSnapshot(); // spinner
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(onDelete).toBeCalled();
});
