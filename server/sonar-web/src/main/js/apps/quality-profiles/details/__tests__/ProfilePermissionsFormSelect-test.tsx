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
import ProfilePermissionsFormSelect from '../ProfilePermissionsFormSelect';

jest.mock('lodash', () => {
  const lodash = require.requireActual('lodash');
  lodash.debounce = (fn: Function) => (...args: any[]) => fn(...args);
  return lodash;
});

it('renders', () => {
  expect(
    shallow(
      <ProfilePermissionsFormSelect
        onChange={jest.fn()}
        onSearch={jest.fn(() => Promise.resolve([]))}
        selected={{ name: 'lambda' }}
      />
    )
  ).toMatchSnapshot();
});

it('searches', () => {
  const onSearch = jest.fn(() => Promise.resolve([]));
  const wrapper = shallow(
    <ProfilePermissionsFormSelect
      onChange={jest.fn()}
      onSearch={onSearch}
      selected={{ name: 'lambda' }}
    />
  );
  expect(onSearch).toBeCalledWith('');
  onSearch.mockClear();

  wrapper.prop<Function>('onInputChange')('f');
  expect(onSearch).not.toBeCalled();

  wrapper.prop<Function>('onInputChange')('foo');
  expect(onSearch).toBeCalledWith('foo');
});
