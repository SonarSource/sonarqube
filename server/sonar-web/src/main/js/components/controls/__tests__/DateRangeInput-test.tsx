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
import { parseDate } from 'sonar-ui-common/helpers/dates';
import DateRangeInput from '../DateRangeInput';

const dateA = parseDate('2018-01-17T00:00:00.000Z');
const dateB = parseDate('2018-02-05T00:00:00.000Z');

it('should render', () => {
  expect(
    shallow(<DateRangeInput onChange={jest.fn()} value={{ from: dateA, to: dateB }} />)
  ).toMatchSnapshot();
});

it('should change', () => {
  const onChange = jest.fn();
  const wrapper = shallow(<DateRangeInput onChange={onChange} />);

  wrapper.find('DateInput[data-test="from"]').prop<Function>('onChange')(dateA);
  expect(onChange).lastCalledWith({ from: dateA, to: undefined });
  wrapper.setProps({ value: { from: dateA } });

  wrapper.find('DateInput[data-test="to"]').prop<Function>('onChange')(dateB);
  wrapper.update();
  expect(onChange).lastCalledWith({ from: dateA, to: dateB });
});
