/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import SearchFilter from '../SearchFilter';
import { change } from '../../../../helpers/testUtils';

it('should render correctly without any search query', () => {
  const wrapper = shallow(<SearchFilter handleSearch={jest.fn()} query={{}} />);
  expect(wrapper).toMatchSnapshot();
});

it('should render with a search query', () => {
  const wrapper = shallow(<SearchFilter handleSearch={jest.fn()} query={{ search: 'foo' }} />);
  expect(wrapper).toMatchSnapshot();
});

it('should display a help message when there is less than 2 characters', () => {
  const wrapper = shallow(<SearchFilter handleSearch={jest.fn()} query={{ search: 'a' }} />);
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ userQuery: 'foo' });
  expect(wrapper).toMatchSnapshot();
});

it('searches', () => {
  const handleSearch = jest.fn();
  const wrapper = shallow(<SearchFilter handleSearch={handleSearch} query={{}} />);

  change(wrapper.find('input'), 'a');
  expect(handleSearch).not.toBeCalled();

  change(wrapper.find('input'), 'abc');
  expect(handleSearch).toBeCalledWith('abc');
});

it('updates state to new props', () => {
  const wrapper = shallow(<SearchFilter handleSearch={jest.fn()} query={{ search: 'abc' }} />);
  expect(wrapper.state()).toEqual({ userQuery: 'abc' });
  wrapper.setProps({ query: { search: 'def' } });
  expect(wrapper.state()).toEqual({ userQuery: 'def' });
});
