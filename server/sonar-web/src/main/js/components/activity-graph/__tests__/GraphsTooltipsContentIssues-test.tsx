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
import { shallow } from 'enzyme';
import * as React from 'react';
import { parseDate } from 'sonar-ui-common/helpers/dates';
import GraphsTooltipsContentIssues, {
  GraphsTooltipsContentIssuesProps
} from '../GraphsTooltipsContentIssues';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ tooltipIdx: -1 }).type()).toBeNull();
});

function shallowRender(props: Partial<GraphsTooltipsContentIssuesProps> = {}) {
  return shallow<GraphsTooltipsContentIssuesProps>(
    <GraphsTooltipsContentIssues
      index={2}
      measuresHistory={[
        {
          metric: 'bugs',
          history: [
            { date: parseDate('2011-10-01T22:01:00.000Z'), value: '500' },
            { date: parseDate('2011-10-25T10:27:41.000Z'), value: '1.2k' }
          ]
        },
        {
          metric: 'reliability_rating',
          history: [
            { date: parseDate('2011-10-01T22:01:00.000Z') },
            { date: parseDate('2011-10-25T10:27:41.000Z'), value: '5.0' }
          ]
        }
      ]}
      name="bugs"
      tooltipIdx={1}
      translatedName="Bugs"
      value="1.2k"
      {...props}
    />
  );
}
