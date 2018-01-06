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
import React from 'react';
import { shallow } from 'enzyme';
import LeakPeriodLegend from '../LeakPeriodLegend';

describe('check note', () => {
  it('10 days', () => {
    const period = {
      date: '2013-09-22T00:00:00+0200',
      mode: 'days',
      parameter: '10'
    };
    expect(shallow(<LeakPeriodLegend period={period} />)).toMatchSnapshot();
  });

  it('date', () => {
    const period = {
      date: '2013-09-22T00:00:00+0200',
      mode: 'date',
      parameter: '2013-01-01'
    };
    expect(shallow(<LeakPeriodLegend period={period} />).find('DateFromNow')).toMatchSnapshot();
  });

  it('version', () => {
    const period = {
      date: '2013-09-22T00:00:00+0200',
      mode: 'version',
      parameter: '0.1'
    };
    expect(shallow(<LeakPeriodLegend period={period} />).find('DateFromNow')).toMatchSnapshot();
  });

  it('previous_version', () => {
    const period = {
      date: '2013-09-22T00:00:00+0200',
      mode: 'previous_version'
    };
    expect(shallow(<LeakPeriodLegend period={period} />).find('DateFromNow')).toHaveLength(1);
  });

  it('previous_analysis', () => {
    const period = {
      date: '2013-09-22T00:00:00+0200',
      mode: 'previous_analysis'
    };
    expect(shallow(<LeakPeriodLegend period={period} />).find('DateFromNow')).toHaveLength(1);
  });
});
