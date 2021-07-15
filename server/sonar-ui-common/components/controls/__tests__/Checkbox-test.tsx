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
import { click } from '../../../helpers/testUtils';
import Checkbox from '../Checkbox';

it('should render', () => {
  const checkbox = shallow(<Checkbox checked={true} onCheck={() => {}} title="Title value" />);
  expect(checkbox).toMatchSnapshot();
});

it('should render unchecked', () => {
  const checkbox = shallow(<Checkbox checked={false} onCheck={() => true} />);
  expect(checkbox.is('.icon-checkbox-checked')).toBe(false);
  expect(checkbox.prop('aria-checked')).toBe(false);
});

it('should render checked', () => {
  const checkbox = shallow(<Checkbox checked={true} onCheck={() => true} />);
  expect(checkbox.is('.icon-checkbox-checked')).toBe(true);
  expect(checkbox.prop('aria-checked')).toBe(true);
});

it('should render disabled', () => {
  const checkbox = shallow(<Checkbox checked={true} disabled={true} onCheck={() => true} />);
  expect(checkbox.is('.icon-checkbox-disabled')).toBe(true);
});

it('should render unchecked third state', () => {
  const checkbox = shallow(<Checkbox checked={false} onCheck={() => true} thirdState={true} />);
  expect(checkbox.is('.icon-checkbox-single')).toBe(true);
  expect(checkbox.is('.icon-checkbox-checked')).toBe(false);
});

it('should render checked third state', () => {
  const checkbox = shallow(<Checkbox checked={true} onCheck={() => true} thirdState={true} />);
  expect(checkbox.is('.icon-checkbox-single')).toBe(true);
  expect(checkbox.is('.icon-checkbox-checked')).toBe(true);
});

it('should render with a spinner', () => {
  const checkbox = shallow(<Checkbox checked={false} loading={true} onCheck={() => true} />);
  expect(checkbox.find('DeferredSpinner').exists()).toBe(true);
});

it('should render children', () => {
  const checkbox = shallow(
    <Checkbox checked={false} onCheck={() => true}>
      <span>foo</span>
    </Checkbox>
  );
  expect(checkbox.hasClass('link-checkbox')).toBe(true);
  expect(checkbox.find('span').exists()).toBe(true);
});

it('should render children with a spinner', () => {
  const checkbox = shallow(
    <Checkbox checked={false} loading={true} onCheck={() => true}>
      <span>foo</span>
    </Checkbox>
  );
  expect(checkbox.hasClass('link-checkbox')).toBe(true);
  expect(checkbox.find('span').exists()).toBe(true);
  expect(checkbox.find('DeferredSpinner').exists()).toBe(true);
});

it('should call onCheck', () => {
  const onCheck = jest.fn();
  const checkbox = shallow(<Checkbox checked={false} onCheck={onCheck} />);
  click(checkbox);
  expect(onCheck).toBeCalledWith(true, undefined);
});

it('should not call onCheck when disabled', () => {
  const onCheck = jest.fn();
  const checkbox = shallow(<Checkbox checked={false} disabled={true} onCheck={onCheck} />);
  click(checkbox);
  expect(onCheck).toHaveBeenCalledTimes(0);
});

it('should call onCheck with id as second parameter', () => {
  const onCheck = jest.fn();
  const checkbox = shallow(<Checkbox checked={false} id="foo" onCheck={onCheck} />);
  click(checkbox);
  expect(onCheck).toBeCalledWith(true, 'foo');
});

it('should apply custom class', () => {
  const checkbox = shallow(
    <Checkbox checked={true} className="customclass" onCheck={() => true} />
  );
  expect(checkbox.is('.customclass')).toBe(true);
});

it('should render the checkbox on the right', () => {
  expect(shallow(<Checkbox checked={true} onCheck={() => true} right={true} />)).toMatchSnapshot();
});
