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
import { shallow } from 'enzyme';
import * as addDays from 'date-fns/add_days';
import * as setMonth from 'date-fns/set_month';
import * as setYear from 'date-fns/set_year';
import * as subDays from 'date-fns/sub_days';
import * as subMonths from 'date-fns/sub_months';
import DateInput from '../DateInput';
import { parseDate } from '../../../helpers/dates';

jest.mock('../../lazyLoad', () => ({
  lazyLoad: () => {
    return function DayPicker() {
      return null;
    };
  }
}));

beforeAll(() => {
  Date.prototype.getFullYear = jest.fn().mockReturnValue(2018); // eslint-disable-line no-extend-native
});

const dateA = parseDate('2018-01-17T00:00:00.000Z');
const dateB = parseDate('2018-02-05T00:00:00.000Z');

it('should render', () => {
  // pass `maxDate` and `minDate` to avoid differences in snapshots
  const { wrapper } = shallowRender();

  expect(wrapper).toMatchSnapshot();

  wrapper.setProps({ value: dateA });
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({ open: true });
  expect(wrapper).toMatchSnapshot();
});

it('should change current month', () => {
  const { wrapper, instance } = shallowRender();
  expect(wrapper.state().currentMonth).toEqual(dateA);

  instance.handlePreviousMonthClick();
  expect(wrapper.state().currentMonth).toEqual(subMonths(dateA, 1));

  instance.handleNextMonthClick();
  expect(wrapper.state().currentMonth).toEqual(dateA);

  instance.handleCurrentMonthChange({ value: 5 });
  expect(wrapper.state().currentMonth).toEqual(setMonth(dateA, 5));

  instance.handleCurrentYearChange({ value: 2015 });
  expect(wrapper.state().currentMonth).toEqual(setYear(setMonth(dateA, 5), 2015));
});

it('should select a day', () => {
  const onChange = jest.fn();
  const { wrapper, instance } = shallowRender({ onChange });
  wrapper.setState({ open: true });

  instance.handleDayClick(dateA, { disabled: true, outside: undefined, today: undefined });
  expect(onChange).not.toBeCalled();
  expect(wrapper.state().open).toBe(true);

  instance.handleDayClick(dateA, { outside: undefined, today: undefined });
  expect(onChange).lastCalledWith(dateA);
  wrapper.update();
  expect(wrapper.state().open).toBe(false);
  expect(wrapper).toMatchSnapshot();

  instance.handleResetClick();
  expect(onChange).lastCalledWith(undefined);
});

it('should hightlightFrom range', () => {
  const { wrapper, instance } = shallowRender({ highlightFrom: dateA });
  wrapper.setState({ open: true });

  const dateC = addDays(dateA, 3);
  instance.handleDayMouseEnter(dateC, { outside: undefined, today: undefined });
  wrapper.update();
  const dayPicker = wrapper.find('DayPicker');
  expect(dayPicker.prop('modifiers')).toEqual({ highlighted: { from: dateA, to: dateC } });
  expect(dayPicker.prop('selectedDays')).toEqual([dateA]);
});

it('should hightlightTo range', () => {
  const { wrapper, instance } = shallowRender({ highlightTo: dateB });
  wrapper.setState({ open: true });

  const dateC = subDays(dateB, 5);
  instance.handleDayMouseEnter(dateC, { outside: undefined, today: undefined });
  wrapper.update();
  const dayPicker = wrapper.find('DayPicker');
  expect(dayPicker.prop('modifiers')).toEqual({ highlighted: { from: dateC, to: dateB } });
  expect(dayPicker.prop('selectedDays')).toEqual([dateB]);
});

function shallowRender(props?: Partial<DateInput['props']>) {
  const wrapper = shallow<DateInput>(
    <DateInput
      currentMonth={dateA}
      maxDate={dateB}
      minDate={dateA}
      onChange={jest.fn()}
      placeholder="placeholder"
      {...props}
    />
  );
  const instance = wrapper.instance();
  return { wrapper, instance };
}
