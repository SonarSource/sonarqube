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
import WorstProjects from '../WorstProjects';

it('renders', () => {
  const subComponents = [
    {
      key: 'foo',
      measures: {
        releasability_rating: '3',
        reliability_rating: '2',
        security_rating: '1',
        sqale_rating: '4',
        ncloc: '200'
      },
      name: 'Foo',
      qualifier: 'SVW'
    },
    {
      key: 'bar',
      measures: {
        alert_status: 'ERROR',
        reliability_rating: '2',
        security_rating: '1',
        sqale_rating: '4',
        ncloc: '100'
      },
      name: 'Bar',
      qualifier: 'TRK',
      refKey: 'barbar'
    },
    {
      key: 'baz',
      measures: {
        alert_status: 'WARN',
        reliability_rating: '2',
        security_rating: '1',
        sqale_rating: '4',
        ncloc: '150'
      },
      name: 'Baz',
      qualifier: 'TRK',
      refKey: 'bazbaz'
    }
  ];
  expect(
    shallow(<WorstProjects component="comp" subComponents={subComponents} total={3} />)
  ).toMatchSnapshot();
});
