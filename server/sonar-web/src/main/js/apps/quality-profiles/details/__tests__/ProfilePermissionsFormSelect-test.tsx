/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { searchGroups, searchUsers } from '../../../../api/quality-profiles';
import {
  mockReactSelectControlProps,
  mockReactSelectOptionProps,
} from '../../../../helpers/mocks/react-select';
import { mockUser } from '../../../../helpers/testMocks';
import ProfilePermissionsFormSelect from '../ProfilePermissionsFormSelect';

jest.mock('lodash', () => {
  const lodash = jest.requireActual('lodash');
  lodash.debounce =
    (fn: Function) =>
    (...args: any[]) =>
      fn(...args);
  return lodash;
});

jest.mock('../../../../api/quality-profiles', () => ({
  searchGroups: jest.fn().mockResolvedValue([]),
  searchUsers: jest.fn().mockResolvedValue([]),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle search', async () => {
  (searchUsers as jest.Mock).mockResolvedValueOnce({ users: [mockUser()] });
  (searchGroups as jest.Mock).mockResolvedValueOnce({ groups: [{ name: 'group1' }] });

  const wrapper = shallowRender();
  const query = 'Waldo';
  const results = await new Promise((resolve) => {
    wrapper.instance().handleSearch(query, resolve);
  });
  expect(searchUsers).toHaveBeenCalledWith(expect.objectContaining({ q: query }));
  expect(searchGroups).toHaveBeenCalledWith(expect.objectContaining({ q: query }));

  expect(results).toHaveLength(2);
});

it('should render option correctly', () => {
  const wrapper = shallowRender();
  const OptionRenderer = wrapper.instance().optionRenderer;
  expect(
    shallow(<OptionRenderer {...mockReactSelectOptionProps({ value: 'test', name: 'name' })} />)
  ).toMatchSnapshot('option renderer');
});

it('should render value correctly', () => {
  const wrapper = shallowRender();
  const ValueRenderer = wrapper.instance().singleValueRenderer;
  expect(
    shallow(<ValueRenderer {...mockReactSelectOptionProps({ value: 'test', name: 'name' })} />)
  ).toMatchSnapshot('value renderer');
});

it('should render control correctly', () => {
  const wrapper = shallowRender();
  const ControlRenderer = wrapper.instance().controlRenderer;
  expect(shallow(<ControlRenderer {...mockReactSelectControlProps()} />)).toMatchSnapshot(
    'control renderer'
  );
});

function shallowRender(overrides: Partial<ProfilePermissionsFormSelect['props']> = {}) {
  return shallow<ProfilePermissionsFormSelect>(
    <ProfilePermissionsFormSelect
      onChange={jest.fn()}
      profile={{ language: 'Java', name: 'Sonar Way' }}
      {...overrides}
    />
  );
}
