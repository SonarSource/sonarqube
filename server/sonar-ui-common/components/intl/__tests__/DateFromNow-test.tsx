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
import { FormattedRelative, IntlProvider } from 'react-intl';
import DateFromNow, { DateFromNowProps } from '../DateFromNow';
import DateTimeFormatter from '../DateTimeFormatter';

const date = '2020-02-20T20:20:20Z';

it('should render correctly', () => {
  const wrapper = shallowRender();

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find(DateTimeFormatter).props().children(date)).toMatchSnapshot('children');
});

it('should render correctly when there is no date', () => {
  const children = jest.fn();

  shallowRender({ date: undefined }, children);

  expect(children).toHaveBeenCalledWith('never');
});

it('should render correctly when the date is less than one hour in the past', () => {
  const veryCloseDate = new Date(date);
  veryCloseDate.setMinutes(veryCloseDate.getMinutes() - 10);
  jest.spyOn(Date, 'now').mockImplementation(() => (new Date(date) as unknown) as number);
  const children = jest.fn();

  shallowRender({ date: veryCloseDate, hourPrecision: true }, children)
    .dive()
    .dive()
    .find(FormattedRelative)
    .props()
    .children(date);

  expect(children).toHaveBeenCalledWith('less_than_1_hour_ago');
});

function shallowRender(overrides: Partial<DateFromNowProps> = {}, children: jest.Mock = jest.fn()) {
  return shallow<DateFromNowProps>(
    <IntlProvider defaultLocale="en-US" locale="en">
      <DateFromNow date={date} {...overrides}>
        {(formattedDate) => children(formattedDate)}
      </DateFromNow>
    </IntlProvider>
  ).dive();
}
