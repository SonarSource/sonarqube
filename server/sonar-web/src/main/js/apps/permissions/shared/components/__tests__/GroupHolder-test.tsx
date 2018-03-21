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
import * as React from 'react';
import { shallow } from 'enzyme';
import GroupHolder from '../GroupHolder';
import { waitAndUpdate } from '../../../../../helpers/testUtils';

const group = {
  id: 'foobar',
  name: 'Foobar',
  permissions: ['bar']
};

const groupHolder = (
  <GroupHolder
    group={group}
    key="foo"
    onToggle={jest.fn(() => Promise.resolve())}
    permissions={['bar']}
    permissionsOrder={['bar', 'baz']}
    selectedPermission={'bar'}
  />
);

it('should display checkboxes for permissions', () => {
  expect(shallow(groupHolder)).toMatchSnapshot();
});

it('should disabled checkboxes when waiting for promise to return', async () => {
  const wrapper = shallow(groupHolder);
  expect(wrapper.state().loading).toEqual([]);

  (wrapper.instance() as GroupHolder).handleCheck(true, 'baz');
  wrapper.update();
  expect(wrapper.state().loading).toEqual(['baz']);
  expect(wrapper).toMatchSnapshot();

  (wrapper.instance() as GroupHolder).handleCheck(true, 'bar');
  wrapper.update();
  expect(wrapper.state().loading).toEqual(['baz', 'bar']);
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loading).toEqual([]);
});
