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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import ProjectKeyInput from '../ProjectKeyInput';

jest.useFakeTimers();

jest.mock('../../../../api/components', () => ({
  doesComponentExists: jest
    .fn()
    .mockImplementation(({ component }) => Promise.resolve(component === 'exists'))
}));

it('should render correctly', async () => {
  const wrapper = shallow(<ProjectKeyInput onChange={jest.fn()} value="key" />);
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ touched: true });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('ValidationInput').prop('isValid')).toBe(true);
});

it('should not display any status when the key is not defined', async () => {
  const wrapper = shallow(<ProjectKeyInput onChange={jest.fn()} value="" />);
  await waitAndUpdate(wrapper);
  expect(wrapper.find('ValidationInput').prop('isInvalid')).toBe(false);
  expect(wrapper.find('ValidationInput').prop('isValid')).toBe(false);
});

it('should have an error when the key is invalid', async () => {
  const wrapper = shallow(<ProjectKeyInput onChange={jest.fn()} value="KEy-with#speci@l_char" />);
  await waitAndUpdate(wrapper);
  expect(wrapper.find('ValidationInput').prop('isInvalid')).toBe(true);
});

it('should have an error when the key already exists', async () => {
  const wrapper = shallow(<ProjectKeyInput onChange={jest.fn()} value="exists" />);
  await waitAndUpdate(wrapper);

  jest.runAllTimers();
  await new Promise(setImmediate);
  expect(wrapper.find('ValidationInput').prop('isInvalid')).toBe(true);
});

it('should handle Change', async () => {
  const onChange = jest.fn();
  const wrapper = shallow(<ProjectKeyInput onChange={onChange} value="" />);
  await waitAndUpdate(wrapper);

  wrapper.find('input').simulate('change', { currentTarget: { value: 'key' } });

  expect(wrapper.state('touched')).toBe(true);
  expect(onChange).toBeCalledWith('key');
});

it('should ignore promise return if value has been changed in the meantime', async () => {
  const onChange = (value: string) => wrapper.setProps({ value });
  const wrapper = shallow(<ProjectKeyInput onChange={onChange} value="" />);
  await waitAndUpdate(wrapper);

  wrapper.find('input').simulate('change', { currentTarget: { value: 'exists' } });
  wrapper.find('input').simulate('change', { currentTarget: { value: 'exists%' } });

  jest.runAllTimers();
  await new Promise(setImmediate);

  expect(wrapper.state('touched')).toBe(true);
  expect(wrapper.state('error')).toBe('onboarding.create_project.project_key.error');
});
