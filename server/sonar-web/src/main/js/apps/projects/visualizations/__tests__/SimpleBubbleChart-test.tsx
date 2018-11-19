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
import * as React from 'react';
import { shallow } from 'enzyme';
import SimpleBubbleChart from '../SimpleBubbleChart';

it('renders', () => {
  const project1 = {
    key: 'foo',
    measures: { complexity: '17.2', coverage: '53.5', ncloc: '1734' },
    name: 'Foo',
    tags: [],
    visibility: 'public'
  };
  expect(
    shallow(
      <SimpleBubbleChart
        colorMetric="security_rating"
        displayOrganizations={false}
        projects={[project1]}
        sizeMetric={{ key: 'ncloc', type: 'INT' }}
        xMetric={{ key: 'complexity', type: 'INT' }}
        yMetric={{ key: 'coverage', type: 'PERCENT' }}
      />
    )
  ).toMatchSnapshot();
});
