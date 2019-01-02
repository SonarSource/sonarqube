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
import { shallow, mount } from 'enzyme';
import SearchBox from '../SearchBox';
import { click, change } from '../../../helpers/testUtils';

jest.mock('lodash', () => {
  const lodash = require.requireActual('lodash');
  const debounce = (fn: Function) => {
    const debounced: any = (...args: any[]) => fn(...args);
    debounced.cancel = jest.fn();
    return debounced;
  };
  return Object.assign({}, lodash, { debounce });
});

it('renders', () => {
  const wrapper = shallow(
    <SearchBox minLength={2} onChange={jest.fn()} placeholder="placeholder" value="foo" />
  );
  expect(wrapper).toMatchSnapshot();
});

it('warns when input is too short', () => {
  const wrapper = shallow(
    <SearchBox minLength={2} onChange={jest.fn()} placeholder="placeholder" value="f" />
  );
  expect(wrapper.find('.search-box-note').exists()).toBeTruthy();
});

it('shows clear button only when there is a value', () => {
  const wrapper = shallow(<SearchBox onChange={jest.fn()} placeholder="placeholder" value="f" />);
  expect(wrapper.find('.search-box-clear').exists()).toBeTruthy();
  wrapper.setProps({ value: '' });
  expect(wrapper.find('.search-box-clear').exists()).toBeFalsy();
});

it('attaches ref', () => {
  const ref = jest.fn();
  mount(<SearchBox innerRef={ref} onChange={jest.fn()} placeholder="placeholder" value="f" />);
  expect(ref).toBeCalled();
  expect(ref.mock.calls[0][0]).toBeInstanceOf(HTMLInputElement);
});

it('resets', () => {
  const onChange = jest.fn();
  const wrapper = shallow(<SearchBox onChange={onChange} placeholder="placeholder" value="f" />);
  click(wrapper.find('.search-box-clear'));
  expect(onChange).toBeCalledWith('');
});

it('changes', () => {
  const onChange = jest.fn();
  const wrapper = shallow(<SearchBox onChange={onChange} placeholder="placeholder" value="f" />);
  change(wrapper.find('.search-box-input'), 'foo');
  expect(onChange).toBeCalledWith('foo');
});

it('does not change when value is too short', () => {
  const onChange = jest.fn();
  const wrapper = shallow(
    <SearchBox minLength={3} onChange={onChange} placeholder="placeholder" value="" />
  );
  change(wrapper.find('.search-box-input'), 'fo');
  expect(onChange).not.toBeCalled();
});
