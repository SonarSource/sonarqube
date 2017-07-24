/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { shallow } from 'enzyme';
import ApplicationQualityGateProject from '../ApplicationQualityGateProject';

const metrics = {
  bugs: { key: 'bugs', name: 'Bugs', type: 'INT' },
  new_coverage: { key: 'new_coverage', name: 'Coverage on New Code', type: 'PERCENT' },
  skipped_tests: { key: 'skipped_tests', name: 'Skipped Tests', type: 'INT' }
};

it('renders', () => {
  const project = {
    key: 'foo',
    name: 'Foo',
    status: 'ERROR',
    conditions: [
      {
        status: 'ERROR',
        metricKey: 'new_coverage',
        comparator: 'LT',
        onLeak: true,
        errorThreshold: '85',
        value: '82.50562381034781'
      },
      {
        status: 'WARN',
        metricKey: 'bugs',
        comparator: 'GT',
        onLeak: false,
        warningThreshold: '0',
        value: '17'
      },
      {
        status: 'ERROR',
        metricKey: 'bugs',
        comparator: 'GT',
        onLeak: true,
        warningThreshold: '0',
        value: '3'
      },
      {
        status: 'OK',
        metricKey: 'skipped_tests',
        comparator: 'GT',
        onLeak: false,
        warningThreshold: '0',
        value: '0'
      }
    ]
  };
  const wrapper = shallow(<ApplicationQualityGateProject metrics={metrics} project={project} />);
  expect(wrapper).toMatchSnapshot();
});
