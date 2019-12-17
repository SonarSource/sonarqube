/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
/* eslint-disable sonarjs/no-duplicate-string */
import { shallow } from 'enzyme';
import * as React from 'react';
import { parseDate } from 'sonar-ui-common/helpers/dates';
import GraphsTooltipsContentCoverage, {
  GraphsTooltipsContentCoverageProps
} from '../GraphsTooltipsContentCoverage';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ addSeparator: true })).toMatchSnapshot('with separator');
  expect(shallowRender({ tooltipIdx: -1 }).type()).toBeNull();
});

function shallowRender(props: Partial<GraphsTooltipsContentCoverageProps> = {}) {
  return shallow<GraphsTooltipsContentCoverageProps>(
    <GraphsTooltipsContentCoverage
      addSeparator={false}
      measuresHistory={[
        {
          metric: 'coverage',
          history: [
            { date: parseDate('2011-10-01T22:01:00.000Z') },
            { date: parseDate('2011-10-25T10:27:41.000Z'), value: '80.3' }
          ]
        },
        {
          metric: 'lines_to_cover',
          history: [
            { date: parseDate('2011-10-01T22:01:00.000Z'), value: '60545' },
            { date: parseDate('2011-10-25T10:27:41.000Z'), value: '65215' }
          ]
        },
        {
          metric: 'uncovered_lines',
          history: [
            { date: parseDate('2011-10-01T22:01:00.000Z'), value: '40564' },
            { date: parseDate('2011-10-25T10:27:41.000Z'), value: '10245' }
          ]
        }
      ]}
      tooltipIdx={1}
      {...props}
    />
  );
}
