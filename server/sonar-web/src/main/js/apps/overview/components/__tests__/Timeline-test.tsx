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
import Timeline from '../Timeline';

const range = parseDate('2017-05-01T00:00:00.000Z');
const history = [
  { date: parseDate('2017-04-08T00:00:00.000Z'), value: '29.6' },
  { date: parseDate('2017-04-09T00:00:00.000Z'), value: '170.8' },
  { date: parseDate('2017-05-08T00:00:00.000Z'), value: '360' },
  { date: parseDate('2017-05-09T00:00:00.000Z'), value: '39' }
];

it('should render correctly with an "after" range', () => {
  expect(shallow(<Timeline after={range} history={history} />)).toMatchSnapshot();
});

it('should render correctly with a "before" range', () => {
  expect(shallow(<Timeline before={range} history={history} />)).toMatchSnapshot();
});

it('should have a correct domain with strings or numbers', () => {
  const date = parseDate('2017-05-08T00:00:00.000Z');
  const wrapper = shallow(<Timeline after={range} history={history} />);
  expect(wrapper.find('LineChart').prop('domain')).toEqual([0, 360]);

  wrapper.setProps({ history: [{ date, value: '360.33' }, { date, value: '39.54' }] });
  expect(wrapper.find('LineChart').prop('domain')).toEqual([0, 360.33]);

  wrapper.setProps({ history: [{ date, value: 360 }, { date, value: 39 }] });
  expect(wrapper.find('LineChart').prop('domain')).toEqual([0, 360]);
});

it('should not fail when a value is missing', () => {
  expect(
    shallow(
      <Timeline
        before={range}
        history={[{ date: parseDate('2017-04-07T00:00:00.000Z') }, ...history]}
      />
    )
  ).toMatchSnapshot();
});
