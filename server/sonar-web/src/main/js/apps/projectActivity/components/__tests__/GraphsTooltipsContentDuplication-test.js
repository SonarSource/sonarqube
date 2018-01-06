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
import GraphsTooltipsContentDuplication from '../GraphsTooltipsContentDuplication';

const MEASURES_DUPLICATION = [
  {
    metric: 'duplicated_lines_density',
    history: [
      {
        date: '2011-10-01T22:01:00.000Z'
      },
      {
        date: '2011-10-25T10:27:41.000Z',
        value: '10245'
      }
    ]
  }
];

const DEFAULT_PROPS = {
  addSeparator: true,
  measuresHistory: MEASURES_DUPLICATION,
  tooltipIdx: 1
};

it('should render correctly', () => {
  expect(shallow(<GraphsTooltipsContentDuplication {...DEFAULT_PROPS} />)).toMatchSnapshot();
});

it('should render null when data is missing', () => {
  expect(
    shallow(<GraphsTooltipsContentDuplication {...DEFAULT_PROPS} tooltipIdx={0} />).type()
  ).toBeNull();
});
