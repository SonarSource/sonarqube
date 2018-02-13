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
import NewOrganizationForm from '../NewOrganizationForm';
import { change, submit, waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/organizations', () => ({
  createOrganization: () => Promise.resolve(),
  deleteOrganization: () => Promise.resolve(),
  getOrganization: () => Promise.resolve(null)
}));

jest.mock('../../../../components/icons-components/ClearIcon');

it('creates new organization', async () => {
  const onDone = jest.fn();
  const wrapper = mount(<NewOrganizationForm onDelete={jest.fn()} onDone={onDone} />);
  expect(wrapper).toMatchSnapshot();
  change(wrapper.find('input'), 'foo');
  submit(wrapper.find('form'));
  expect(wrapper).toMatchSnapshot(); // spinner
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(onDone).toBeCalledWith('foo');
});

it('deletes organization', async () => {
  const onDelete = jest.fn();
  const wrapper = mount(<NewOrganizationForm onDelete={onDelete} onDone={jest.fn()} />);
  wrapper.setState({ done: true, loading: false, organization: 'foo' });
  expect(wrapper).toMatchSnapshot();
  wrapper.find('DeleteButton').prop('onClick')();
  wrapper.update();
  expect(wrapper).toMatchSnapshot(); // spinner
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(onDelete).toBeCalled();
});
