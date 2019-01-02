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
import QualityGate from '../QualityGate';

it('renders message about ignored conditions', () => {
  expect(
    shallow(
      <QualityGate
        component={{ key: 'foo', qualifier: 'TRK' }}
        measures={[
          {
            metric: {
              id: '1',
              key: 'alert_status',
              name: 'Quality Gate Status',
              type: 'LEVEL'
            },
            value: 'OK'
          },
          {
            metric: {
              id: '2',
              key: 'quality_gate_details',
              name: 'QualityGateDetails',
              type: 'DATA'
            },
            value: '{"level":"OK","conditions":[],"ignoredConditions":true}'
          }
        ]}
      />
    )
  ).toMatchSnapshot();
});
