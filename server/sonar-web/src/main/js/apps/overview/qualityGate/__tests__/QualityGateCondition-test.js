/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import QualityGateCondition from '../QualityGateCondition';
import { DrilldownLink } from '../../../../components/shared/drilldown-link';

it('should render DrilldownLink', () => {
  const component = {
    id: 'abcd',
    key: 'abcd-key'
  };
  const periods = [];
  const condition = {
    actual: '10',
    error: '0',
    level: 'ERROR',
    measure: {
      metric: {
        key: 'open_issues',
        type: 'INT',
        name: 'Open Issues'
      },
      value: '10'
    },
    metric: 'open_issues',
    op: 'GT'
  };

  const output = shallow(
      <QualityGateCondition
          component={component}
          periods={periods}
          condition={condition}/>
  );

  const link = output.find(DrilldownLink);
  expect(link.prop('component')).toBe('abcd-key');
  expect(link.prop('metric')).toBe('open_issues');
});
