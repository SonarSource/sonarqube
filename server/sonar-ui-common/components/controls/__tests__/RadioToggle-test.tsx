/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { change } from '../../../helpers/testUtils';
import RadioToggle from '../RadioToggle';

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('calls onCheck', () => {
  const onCheck = jest.fn();
  const wrapper = shallowRender({ onCheck });
  change(wrapper.find('input[id="sample__two"]'), '');
  expect(onCheck).toBeCalledWith('two');
});

it('handles numeric values', () => {
  const onCheck = jest.fn();
  const wrapper = shallowRender({
    onCheck,
    options: [
      { value: 1, label: 'first', tooltip: 'foo' },
      { value: 2, label: 'second', tooltip: 'bar' },
    ],
    value: 1,
  });
  change(wrapper.find('input[id="sample__2"]'), '');
  expect(onCheck).toBeCalledWith(2);
});

it('handles boolean values', () => {
  const onCheck = jest.fn();
  const wrapper = shallowRender({
    onCheck,
    options: [
      { value: true, label: 'yes', tooltip: 'foo' },
      { value: false, label: 'no', tooltip: 'bar' },
    ],
    value: true,
  });
  change(wrapper.find('input[id="sample__false"]'), '');
  expect(onCheck).toBeCalledWith(false);
});

it('initialize value', () => {
  const onCheck = jest.fn();
  const wrapper = shallowRender({
    onCheck,
    options: [
      { value: 1, label: 'first', tooltip: 'foo' },
      { value: 2, label: 'second', tooltip: 'bar', disabled: true },
    ],
    value: 2,
  });
  expect(wrapper.find('input[checked=true]').prop('id')).toBe('sample__2');
});

it('accepts advanced options fields', () => {
  expect(
    shallowRender({
      options: [
        { value: 'one', label: 'first', tooltip: 'foo' },
        { value: 'two', label: 'second', tooltip: 'bar', disabled: true },
      ],
    })
  ).toMatchSnapshot();
});

function shallowRender(props?: Partial<RadioToggle['props']>) {
  const options = [
    { value: 'one', label: 'first' },
    { value: 'two', label: 'second' },
  ];
  return shallow(<RadioToggle name="sample" onCheck={() => true} options={options} {...props} />);
}
